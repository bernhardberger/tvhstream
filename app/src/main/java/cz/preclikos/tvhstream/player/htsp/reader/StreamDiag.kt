package cz.preclikos.tvhstream.player.htsp.reader

import timber.log.Timber

/**
 * Diagnostic-only PTS/DTS logger for the HTSP playback pipeline (issue #2:
 * IPTV/m3u8 freezes after the first frame). It does NOT alter timestamps.
 *
 * What we learned from a working SAT channel: the video PTS legitimately moves
 * backward by ~60–100 ms because of B-frame reordering (transmission/decode order
 * != presentation order), so a naive "PTS went backward" check is just noise. The
 * real signals for the IPTV freeze are:
 *  - a large audio↔video START SKEW (A/V desync baked into the source),
 *  - DTS (decode order, which MUST be monotonic) going backward or gapping by
 *    seconds (a true discontinuity / stream reset),
 *  - very large PTS jumps beyond the reorder window.
 *
 * Reset per subscription. Filter logcat by tag "PtsDiag".
 */
internal object StreamDiag {

    private const val TAG = "PtsDiag"
    private const val LOG_FIRST = 30             // verbose per-packet logs for the first N packets
    private const val DTS_GAP_WARN_US = 1_500_000L  // warn on decode-order gaps larger than 1.5s
    private const val PTS_BACK_WARN_US = 1_000_000L // ignore <1s backward PTS (normal B-frame reorder)

    private class State {
        var count = 0
        var firstPts = Long.MIN_VALUE
        var firstDts = Long.MIN_VALUE
        var lastPts = Long.MIN_VALUE
        var lastDts = Long.MIN_VALUE
        var firstLogged = false
    }

    private val states = HashMap<String, State>()
    private var firstVideoPts: Long? = null
    private var firstAudioPts: Long? = null
    private var skewLogged = false

    @Synchronized
    fun reset() {
        if (states.isNotEmpty()) Timber.tag(TAG).i("---- reset (new subscription) ----")
        states.clear()
        firstVideoPts = null
        firstAudioPts = null
        skewLogged = false
    }

    private fun isVideo(kind: String) = kind == "video" || kind == "h264" || kind == "h265"

    @Synchronized
    fun onSample(
        kind: String,
        index: Int,
        pts: Long,
        dts: Long?,
        frameType: Int,
        payloadSize: Int,
    ) {
        val key = "$kind#$index"
        val s = states.getOrPut(key) { State() }

        if (!s.firstLogged) {
            s.firstPts = pts
            s.firstDts = dts ?: Long.MIN_VALUE
            s.firstLogged = true
            Timber.tag(TAG).i(
                "FIRST %s pts=%d dts=%s frametype=%d size=%d",
                key, pts, dts?.toString() ?: "n/a", frameType, payloadSize
            )
            if (isVideo(kind) && firstVideoPts == null) firstVideoPts = pts
            if (kind == "audio" && firstAudioPts == null) firstAudioPts = pts
            maybeLogSkew()
        }

        if (s.count < LOG_FIRST) {
            val dPts = if (s.lastPts == Long.MIN_VALUE) 0L else pts - s.lastPts
            val dDts =
                if (dts != null && s.lastDts != Long.MIN_VALUE) (dts - s.lastDts).toString() else "n/a"
            Timber.tag(TAG).d(
                "%s #%d pts=%d dts=%s dPts=%d dDts=%s frametype=%d size=%d",
                key, s.count, pts, dts?.toString() ?: "n/a", dPts, dDts, frameType, payloadSize
            )
        } else {
            // Steady-state: trust DTS (decode order) for monotonicity; it must not move
            // backward. Fall back to PTS only when the stream provides no DTS.
            if (dts != null && s.lastDts != Long.MIN_VALUE) {
                val dDts = dts - s.lastDts
                when {
                    dDts < 0 -> Timber.tag(TAG).w(
                        "%s DTS WENT BACKWARD by %d us (dts=%d prev=%d) <-- discontinuity",
                        key, -dDts, dts, s.lastDts
                    )

                    dDts > DTS_GAP_WARN_US -> Timber.tag(TAG).w(
                        "%s DTS GAP %d us (dts=%d prev=%d)", key, dDts, dts, s.lastDts
                    )
                }
            } else if (dts == null && s.lastPts != Long.MIN_VALUE) {
                val dPts = pts - s.lastPts
                when {
                    dPts < -PTS_BACK_WARN_US -> Timber.tag(TAG).w(
                        "%s PTS WENT BACKWARD by %d us (no dts) (pts=%d prev=%d) <-- discontinuity",
                        key, -dPts, pts, s.lastPts
                    )

                    dPts > DTS_GAP_WARN_US -> Timber.tag(TAG).w(
                        "%s PTS GAP %d us (no dts) (pts=%d prev=%d)", key, dPts, pts, s.lastPts
                    )
                }
            }
        }

        if (dts != null) s.lastDts = dts
        s.lastPts = pts
        s.count++
    }

    private fun maybeLogSkew() {
        val v = firstVideoPts
        val a = firstAudioPts
        if (!skewLogged && v != null && a != null) {
            skewLogged = true
            val skew = v - a
            Timber.tag(TAG).i(
                "A/V START SKEW: video-audio = %d us (%.3f s) [firstVideoPts=%d firstAudioPts=%d]",
                skew, skew / 1_000_000.0, v, a
            )
        }
    }
}
