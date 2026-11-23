package app.perch

import android.graphics.Bitmap
import android.media.session.PlaybackState

data class Track(
    val key: String,
    val title: String,
    val artist: String,
    val album: String,
    val art: Bitmap?,
    val durationMs: Long,
)

data class Playback(
    val state: Int,
    val positionMs: Long,
    /** SystemClock.elapsedRealtime() when [positionMs] was reported. */
    val updatedAt: Long,
    val speed: Float,
)

/** What the active media session says, as one immutable snapshot. */
data class NowPlaying(
    val sessionPkg: String? = null,
    val track: Track? = null,
    val playback: Playback? = null,
    val actions: Long = 0,
    /** elapsedRealtime of the last moment a song was playing (or when watching began). */
    val lastPlayingAt: Long = 0,
) {
    val hasSession get() = sessionPkg != null
    val isPlaying
        get() = playback?.state.let {
            it == PlaybackState.STATE_PLAYING || it == PlaybackState.STATE_BUFFERING ||
                it == PlaybackState.STATE_CONNECTING
        }
    val canNext get() = actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L
    val canPrev get() = actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L
    val canSeek get() = actions and PlaybackState.ACTION_SEEK_TO != 0L

    /** The position extrapolated to [nowMs] (an elapsedRealtime value) while playing. */
    fun positionAt(nowMs: Long): Long {
        val p = playback ?: return 0
        val base = if (p.state == PlaybackState.STATE_PLAYING) p.positionMs + ((nowMs - p.updatedAt) * p.speed).toLong() else p.positionMs
        val d = track?.durationMs ?: 0
        return if (d > 0) base.coerceIn(0, d) else base.coerceAtLeast(0)
    }
}
