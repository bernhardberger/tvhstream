package cz.preclikos.tvhstream.viewmodels

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.preclikos.tvhstream.R
import cz.preclikos.tvhstream.core.ConnectionPolicy
import cz.preclikos.tvhstream.core.ConnectionFailureKind
import cz.preclikos.tvhstream.core.connectionFailureKind
import cz.preclikos.tvhstream.htsp.HtspEvent
import cz.preclikos.tvhstream.htsp.HtspMessage
import cz.preclikos.tvhstream.htsp.HtspService
import cz.preclikos.tvhstream.htsp.SubscriptionStatus
import cz.preclikos.tvhstream.repositories.TvhRepository
import cz.preclikos.tvhstream.services.StatusService
import cz.preclikos.tvhstream.services.StatusSlot
import cz.preclikos.tvhstream.services.UiText
import cz.preclikos.tvhstream.settings.SecurePasswordStore
import cz.preclikos.tvhstream.settings.ServerSettingsStore
import cz.preclikos.tvhstream.settings.StoredPassword
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber

class AppConnectionViewModel(
    private val htsp: HtspService,
    private val repo: TvhRepository,
    private val statusService: StatusService,
    private val settings: ServerSettingsStore,
    private val passwords: SecurePasswordStore,
) : ViewModel() {

    val status = statusService.headline
    val connectionState = htsp.state

    private data class ServerCfg(
        val host: String,
        val htspPort: Int,
        val username: String,
        val password: String
    )

    @Volatile
    private var lastCfg: ServerCfg? = null
    private var reconnectJob: Job? = null
    private var autoJob: Job? = null

    private val subs = mutableMapOf<Int, SubscriptionStatus>()

    init {
        repo.startIfNeeded()

        autoJob = viewModelScope.launch(Dispatchers.IO) {
            combine(
                settings.serverSettings,
                passwords.passwordState,
            ) { server, password -> server to password }
                .collectLatest { (server, password) ->
                    if (!ConnectionPolicy.isAutoConnectReady(server.host, server.htspPort)) {
                        return@collectLatest
                    }

                    val value = when (password) {
                        StoredPassword.Empty -> ""
                        is StoredPassword.Available -> password.value
                        StoredPassword.Unavailable -> {
                            lastCfg = null
                            reconnectJob?.cancel()
                            reconnectJob = null
                            statusService.set(
                                StatusSlot.CONNECTION,
                                UiText.Res(R.string.credential_unavailable),
                            )
                            return@collectLatest
                        }
                    }

                    val cfg = ServerCfg(
                        host = server.host,
                        htspPort = server.htspPort,
                        username = server.username,
                        password = value,
                    )
                    if (lastCfg == cfg) return@collectLatest
                    lastCfg = cfg
                    startOrRestartReconnectLoop()
                }
        }

        viewModelScope.launch(Dispatchers.IO) {
            htsp.controlEvents.collectLatest { e ->
                when (e) {
                    is HtspEvent.ConnectionError -> {
                        statusService.set(
                            StatusSlot.CONNECTION,
                            UiText.Res(R.string.status_disconnected_reconnecting)
                        )
                        repo.onDisconnected()
                        startOrRestartReconnectLoop()
                    }

                    is HtspEvent.ServerMessage -> {
                        val msg = e.msg

                        msg.toSubStatusOrNull()?.let { st ->
                            subs[st.id] = st
                            publishSubsStatus()
                            return@collectLatest
                        }

                        msg.subStopIdOrNull()?.let { id ->
                            subs.remove(id)
                            publishSubsStatus()
                            return@collectLatest
                        }
                    }
                }
            }
        }
    }

    private fun publishSubsStatus() {
        val resId = subs.values.computeUiStatusResId()
        if (resId == null) {
            statusService.set(StatusSlot.CONNECTION, null)
        } else {
            statusService.set(StatusSlot.CONNECTION, UiText.Res(resId))
        }
    }

    private fun HtspMessage.toSubStatusOrNull(): SubscriptionStatus? {
        val m = method ?: return null
        if (m != "subscriptionStatus" && m != "subscriptionStart") return null

        val id = int("subscriptionId") ?: int("id") ?: return null

        return SubscriptionStatus(
            id = id,
            state = str("state") ?: str("status"),
            subscriptionError = str("subscriptionError") ?: str("error")
        )
    }

    private fun HtspMessage.subStopIdOrNull(): Int? {
        val m = method ?: return null
        if (m != "subscriptionStop") return null
        return int("subscriptionId") ?: int("id")
    }

    @StringRes
    private fun Collection<SubscriptionStatus>.computeUiStatusResId(): Int? {
        if (isEmpty()) return null

        fun norm(v: String?): String =
            v?.lowercase()?.replace(" ", "") ?: ""

        for (s in this) {
            val code = norm(s.subscriptionError ?: s.state)

            return when {
                "invalidtarget" in code -> R.string.tvh_target_invalid
                "nofreeadapter" in code -> R.string.tvh_no_free_adapter
                "muxnotenabled" in code -> R.string.tvh_mux_not_enabled
                "tuningfailed" in code -> R.string.tvh_tuning_failed
                "badsignal" in code -> R.string.tvh_bad_signal
                "scrambled" in code -> R.string.tvh_scrambled
                "subscriptionoverridden" in code -> R.string.tvh_subscription_overridden
                "noinput" in code -> R.string.tvh_no_input
                else -> continue
            }
        }

        return null
    }

    private fun startOrRestartReconnectLoop() {
        reconnectJob?.cancel()

        reconnectJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val cfg = lastCfg ?: return@launch

                val ok = connectInternal(
                    cfg.host,
                    cfg.htspPort,
                    cfg.username,
                    cfg.password
                )
                if (ok) return@launch

                delay(5_000)
            }
        }
    }

    fun reconnectNow() = startOrRestartReconnectLoop()

    fun connectOnceFromUi(
        host: String,
        htspPort: Int,
        username: String,
        password: String
    ) {
        lastCfg = ServerCfg(host, htspPort, username, password)
        startOrRestartReconnectLoop()
    }

    private suspend fun connectInternal(
        host: String,
        port: Int,
        username: String,
        password: String
    ): Boolean {
        return try {
            statusService.set(StatusSlot.SYNC, null)
            statusService.set(StatusSlot.EPG, null)
            statusService.set(
                StatusSlot.CONNECTION,
                UiText.Res(R.string.status_connecting, listOf(host, port)),
            )

            repo.onNewConnectionStarting()

            htsp.connect(
                host = host,
                port = port,
                username = username,
                password = password,
                forceReconnect = true,
                connectTimeoutMs = 10_000,
                responseTimeoutMs = 5_000
            )
            statusService.set(StatusSlot.CONNECTION, UiText.Res(R.string.status_connected))

            statusService.set(StatusSlot.SYNC, UiText.Res(R.string.status_syncing))
            htsp.enableAsyncMetadataAndWaitInitialSync()

            repo.awaitChannelsReady()
            repo.startEpgWorker()

            true
        } catch (e: Exception) {
            Timber.e(e, "Connect failed")
            val statusRes = when (connectionFailureKind(e)) {
                ConnectionFailureKind.AUTHENTICATION -> R.string.status_connection_failed_authentication
                ConnectionFailureKind.DNS -> R.string.status_connection_failed_dns
                ConnectionFailureKind.UNREACHABLE -> R.string.status_connection_failed_unreachable
                ConnectionFailureKind.TIMEOUT -> R.string.status_connection_failed_timeout
                ConnectionFailureKind.OTHER -> R.string.status_connection_failed_other
            }
            statusService.set(
                StatusSlot.CONNECTION,
                UiText.Res(statusRes)
            )
            false
        }
    }
}
