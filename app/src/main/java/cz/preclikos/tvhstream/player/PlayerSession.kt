package cz.preclikos.tvhstream.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.util.EventLogger
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class PlayerSession(
    private val htsp: HtspService,
    private val playerSettingsStore: PlayerSettingsStore,
) {
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var player: ExoPlayer? = null
    private lateinit var dataSourceFactory: HtspSubscriptionDataSource.Factory

    private var subscriptionId: Int? = null

    private var playWhenReadyState = true
    private var currentItem = 0
    private var playbackPosition = 0L

    private var watchdogJob: Job? = null

    private companion object {
        // If the player is still BUFFERING with the position not advancing this long
        // after start, assume a renderer (typically a non-decodable audio track) is
        // blocking and drop audio so the video can play instead of freezing.
        const val STUCK_TIMEOUT_MS = 5_000L
        const val STUCK_POS_MS = 1_000L
    }

    @OptIn(UnstableApi::class)
    fun getOrCreatePlayer(context: Context): ExoPlayer {
        val renderersFactory = LegacyRenderer(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

        return player ?: ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context))
            .build()
            .also { p ->
                p.addAnalyticsListener(EventLogger())
                player = p

                p.playWhenReady = playWhenReadyState
                p.seekTo(currentItem, playbackPosition)
            }
    }

    @OptIn(UnstableApi::class)
    fun playService(context: Context, serviceId: Int) {
        subscriptionId = null
        watchdogJob?.cancel()

        mainScope.launch {
            val p = getOrCreatePlayer(context)
            val settings = playerSettingsStore.playerSettings.first()

            // Apply audio/subtitle language preferences. Subtitles default to OFF
            // unless the user has configured a preferred subtitle language, so they
            // no longer turn themselves on without the user asking for them. Audio is
            // re-enabled here so a previous channel's stuck-audio recovery (below)
            // doesn't keep audio disabled on the next channel.
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

            dataSourceFactory = HtspSubscriptionDataSource.Factory(context, htsp, settings.profile)
            val mediaSource = ProgressiveMediaSource.Factory(
                dataSourceFactory,
                TvheadendExtractorsFactory()
            ).createMediaSource(MediaItem.fromUri("htsp://service/$serviceId"))

            p.setMediaSource(mediaSource)
            p.prepare()
            p.playWhenReady = true

            startStuckAudioWatchdog(p)
        }
    }

    /**
     * Recovery for streams whose audio track never lets the player reach READY (e.g.
     * an IPTV AAC stream the decoder can't decode): if playback is still stuck
     * buffering with the position barely moved after [STUCK_TIMEOUT_MS], disable the
     * audio track so the video renderer can drive playback on its own.
     */
    private fun startStuckAudioWatchdog(p: ExoPlayer) {
        watchdogJob?.cancel()
        watchdogJob = mainScope.launch {
            delay(STUCK_TIMEOUT_MS)
            val stuck = p.playWhenReady &&
                    p.playbackState == Player.STATE_BUFFERING &&
                    p.currentPosition < STUCK_POS_MS
            if (stuck) {
                Timber.w(
                    "Playback stuck buffering after %d ms (pos=%d); disabling audio track to recover video",
                    STUCK_TIMEOUT_MS, p.currentPosition
                )
                p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                    .build()
            }
        }
    }

    fun stop() {
        watchdogJob?.cancel()
        mainScope.launch {
            player?.let { p ->
                updateState(p)
                p.stop()
                p.clearMediaItems()
            }
        }
        unsubscribe()
    }

    fun release() {
        watchdogJob?.cancel()
        dataSourceFactory.releaseCurrentDataSource()
        mainScope.launch {
            player?.let { p ->
                updateState(p)
                p.release()
            }
            player = null
        }

        unsubscribe()
    }

    private fun unsubscribe() {
        mainScope.launch {
            withContext(Dispatchers.IO)
            {
                dataSourceFactory.releaseCurrentDataSource()
            }
        }
    }

    private fun updateState(p: ExoPlayer) {
        playWhenReadyState = p.playWhenReady
        currentItem = p.currentMediaItemIndex
        playbackPosition = p.currentPosition
    }
}
