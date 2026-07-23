package cz.preclikos.tvhstream.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.util.EventLogger
import cz.preclikos.tvhstream.core.PlaybackRecoveryPolicy
import cz.preclikos.tvhstream.htsp.HtspService
import cz.preclikos.tvhstream.player.htsp.HtspSubscriptionDataSource
import cz.preclikos.tvhstream.player.htsp.LegacyRenderer
import cz.preclikos.tvhstream.player.htsp.TvheadendExtractorsFactory
import cz.preclikos.tvhstream.settings.PlayerSettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

sealed interface PlaybackSessionState {
    data object Idle : PlaybackSessionState
    data object Starting : PlaybackSessionState
    data object Playing : PlaybackSessionState
    data class Recovering(val retryDelayMillis: Long) : PlaybackSessionState
}

class PlayerSession(
    private val htsp: HtspService,
    private val playerSettingsStore: PlayerSettingsStore,
) {
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var player: ExoPlayer? = null
    private var dataSourceFactory: HtspSubscriptionDataSource.Factory? = null

    private var subscriptionId: Int? = null
    private var activeContext: Context? = null
    private var activeServiceId: Int? = null
    private var consecutiveFailures = 0
    private var retryJob: Job? = null

    private val _state = MutableStateFlow<PlaybackSessionState>(PlaybackSessionState.Idle)
    val state: StateFlow<PlaybackSessionState> = _state

    private var playWhenReadyState = true
    private var currentItem = 0
    private var playbackPosition = 0L

    private var watchdogJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            Timber.e(error, "Playback failed")
            scheduleRecovery()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> _state.value = PlaybackSessionState.Playing
                Player.STATE_ENDED -> scheduleRecovery()
                Player.STATE_BUFFERING -> {
                    if (_state.value !is PlaybackSessionState.Recovering) {
                        _state.value = PlaybackSessionState.Starting
                    }
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                retryJob?.cancel()
                retryJob = null
                consecutiveFailures = 0
                _state.value = PlaybackSessionState.Playing
            }
        }
    }

    private companion object {
        // If playback is still BUFFERING with the position not advancing this long
        // after start, a renderer (typically an audio track the hardware decoder can't
        // actually play) is blocking; drop audio so the video plays instead of freezing.
        const val STUCK_TIMEOUT_MS = 6_000L
        const val STUCK_POS_MS = 1_000L
    }

    @OptIn(UnstableApi::class)
    fun getOrCreatePlayer(context: Context): ExoPlayer {
        // Hardware decoders everywhere (MODE_ON) so AAC keeps its 5.1 channels and
        // AC3/EAC3 can still pass through to an AVR. The one exception is MPEG-1/2 audio
        // (MP1/MP2): the Amlogic platform decoder advertises support but fails valid
        // DVB/IPTV frames with "Invalid data frame", and never falls back. LegacyRenderer
        // hides that decoder via a MediaCodecSelector so only MP1/MP2 drop to the bundled
        // FFmpeg software decoder; everything else stays on hardware.
        val renderersFactory = LegacyRenderer(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

        return player ?: ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context))
            .build()
            .also { p ->
                p.addAnalyticsListener(EventLogger())
                p.addListener(playerListener)
                player = p

                p.playWhenReady = playWhenReadyState
                p.seekTo(currentItem, playbackPosition)
            }
    }

    @OptIn(UnstableApi::class)
    fun playService(context: Context, serviceId: Int) {
        activeContext = context.applicationContext
        activeServiceId = serviceId
        consecutiveFailures = 0
        retryJob?.cancel()
        retryJob = null
        subscriptionId = null
        watchdogJob?.cancel()

        mainScope.launch {
            startPlayback(context.applicationContext, serviceId)
        }
    }

    @OptIn(UnstableApi::class)
    private suspend fun startPlayback(context: Context, serviceId: Int) {
        if (activeServiceId != serviceId) return

        try {
            _state.value = PlaybackSessionState.Starting
            val p = getOrCreatePlayer(context)
            val settings = playerSettingsStore.playerSettings.first()

            val previousFactory = dataSourceFactory
            dataSourceFactory = null
            if (previousFactory != null) {
                p.stop()
                p.clearMediaItems()
                withContext(Dispatchers.IO) {
                    previousFactory.releaseCurrentDataSource()
                }
                if (activeServiceId != serviceId) return
            }

            // Apply audio/subtitle language preferences. Subtitles default to OFF
            // unless a subtitle language is configured. Audio is re-enabled here so a
            // previous channel's stuck-audio recovery doesn't keep audio off.
            p.trackSelectionParameters = p.trackSelectionParameters.buildUpon().apply {
                setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)

                settings.audioLanguage?.takeIf { it.isNotBlank() }
                    ?.let { setPreferredAudioLanguage(it) }

                val sub = settings.subtitleLanguage
                if (sub.isNullOrBlank()) {
                    setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                } else {
                    setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    setPreferredTextLanguage(sub)
                }
            }.build()

            val factory = HtspSubscriptionDataSource.Factory(context, htsp, settings.profile)
            dataSourceFactory = factory
            val mediaSource = ProgressiveMediaSource.Factory(
                factory,
                TvheadendExtractorsFactory()
            ).createMediaSource(MediaItem.fromUri("htsp://service/$serviceId"))

            p.setMediaSource(mediaSource)
            p.prepare()
            p.playWhenReady = true

            startStuckAudioWatchdog(p, serviceId)
        } catch (t: Throwable) {
            Timber.e(t, "Unable to start service %d", serviceId)
            scheduleRecovery()
        }
    }

    private fun scheduleRecovery() {
        val context = activeContext ?: return
        val serviceId = activeServiceId ?: return
        if (retryJob?.isActive == true) return

        val delayMillis = PlaybackRecoveryPolicy.retryDelayMillis(++consecutiveFailures)
        _state.value = PlaybackSessionState.Recovering(delayMillis)
        watchdogJob?.cancel()
        retryJob = mainScope.launch {
            delay(delayMillis)
            if (activeServiceId != serviceId) return@launch
            retryJob = null

            Timber.i(
                "Retrying service %d after playback failure %d",
                serviceId,
                consecutiveFailures,
            )
            startPlayback(context, serviceId)
        }
    }

    /**
     * Safety net for a stream whose audio track the hardware decoder can't actually
     * play (e.g. some IPTV ADTS AAC the platform decoder rejects with 0x1001): the
     * player then never reaches READY and the first video frame stays frozen. If we're
     * still stuck buffering with the position barely moved after [STUCK_TIMEOUT_MS],
     * disable the audio track so the video renderer can drive playback on its own.
     *
     * This only fires on a genuine stall — when the hardware decoder plays the audio
     * fine (the normal case) the watchdog returns without touching anything.
     */
    private fun startStuckAudioWatchdog(p: ExoPlayer, serviceId: Int) {
        watchdogJob?.cancel()
        watchdogJob = mainScope.launch {
            delay(STUCK_TIMEOUT_MS)
            val stuck = p.playWhenReady &&
                    p.playbackState == Player.STATE_BUFFERING &&
                    p.currentPosition < STUCK_POS_MS
            if (stuck) {
                Timber.w(
                    "Playback stuck buffering after %d ms (pos=%d); disabling audio to recover video",
                    STUCK_TIMEOUT_MS, p.currentPosition
                )
                p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                    .build()

                delay(STUCK_TIMEOUT_MS)
                val stillStuck = activeServiceId == serviceId &&
                    p.playWhenReady &&
                    p.playbackState == Player.STATE_BUFFERING &&
                    p.currentPosition < STUCK_POS_MS
                if (stillStuck) scheduleRecovery()
            }
        }
    }

    fun stop() {
        activeContext = null
        activeServiceId = null
        consecutiveFailures = 0
        retryJob?.cancel()
        retryJob = null
        watchdogJob?.cancel()
        _state.value = PlaybackSessionState.Idle
        val factory = dataSourceFactory
        dataSourceFactory = null
        mainScope.launch {
            player?.let { p ->
                updateState(p)
                p.stop()
                p.clearMediaItems()
            }
            if (factory != null) {
                withContext(Dispatchers.IO) {
                    factory.releaseCurrentDataSource()
                }
            }
        }
    }

    fun release() {
        activeContext = null
        activeServiceId = null
        retryJob?.cancel()
        retryJob = null
        watchdogJob?.cancel()
        _state.value = PlaybackSessionState.Idle
        val factory = dataSourceFactory
        dataSourceFactory = null
        mainScope.launch {
            player?.let { p ->
                updateState(p)
                p.removeListener(playerListener)
                p.release()
            }
            player = null
            if (factory != null) {
                withContext(Dispatchers.IO) {
                    factory.releaseCurrentDataSource()
                }
            }
        }
    }

    private fun updateState(p: ExoPlayer) {
        playWhenReadyState = p.playWhenReady
        currentItem = p.currentMediaItemIndex
        playbackPosition = p.currentPosition
    }
}
