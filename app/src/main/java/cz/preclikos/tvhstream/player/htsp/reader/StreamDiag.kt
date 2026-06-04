package cz.preclikos.tvhstream.player.htsp.reader

import timber.log.Timber

/**
 * Diagnostic-only PTS/DTS logger for the HTSP playback pipeline (issue #2:
 * IPTV/m3u8 freezes after the first frame). It does NOT alter timestamps — it only
 * logs them so we can see, per stream, whether the freeze is caused by:
 *  - a large audio↔video start-PTS offset (A/V desync in the source),
 *  - PTS going backward / large gaps (HLS segment / restream discontinuities),
 *  - unexpected frame-type values (keyframe detection failing).
 *
 * Reset per subscription so stream indices don't bleed across channels.
 * Filter logcat by tag "PtsDiag".
 */
internal object StreamDiag {

    private const val TAG = "PtsDiag"
    private const val LOG_FIRST = 40            // verbose per-packet logs for the first N packets
    private const val GAP_WARN_US = 2_000_000L  // warn on forward gaps larger than 2s

    private class State {
        var count = 0
        var lastPts = Long.MIN_VALUE
        var firstLogged = false
    }

    private val states = HashMap<String, State>()

    @Synchronized
    fun reset() {
        if (states.isNotEmpty()) Timber.tag(TAG).i("---- reset (new subscription) ----")
        states.clear()
    }

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
            Timber.tag(TAG).i(
                "FIRST %s pts=%d dts=%s frametype=%d size=%d",
                key, pts, dts?.toString() ?: "n/a", frameType, payloadSize
            )
            s.firstLogged = true
        }

        if (s.count < LOG_FIRST) {
            val dPts = if (s.lastPts == Long.MIN_VALUE) 0L else pts - s.lastPts
            Timber.tag(TAG).d(
                "%s #%d pts=%d dts=%s dPts=%d frametype=%d size=%d",
                key, s.count, pts, dts?.toString() ?: "n/a", dPts, frameType, payloadSize
            )
        } else if (s.lastPts != Long.MIN_VALUE) {
            val dPts = pts - s.lastPts
            when {
                dPts < 0 -> Timber.tag(TAG)
                    .w("%s PTS WENT BACKWARD by %d us (pts=%d prev=%d)", key, -dPts, pts, s.lastPts)

                dPts > GAP_WARN_US -> Timber.tag(TAG)
                    .w("%s PTS GAP %d us (pts=%d prev=%d)", key, dPts, pts, s.lastPts)
            }
        }

        s.lastPts = pts
        s.count++
    }
}
