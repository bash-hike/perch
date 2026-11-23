package app.perch

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.SystemClock

/**
 * Screenshot support (debug builds only): publishes a media session with a generated cover and made-up
 * track details, so store and README images contain no copyrighted art and no real library.
 *
 *   adb shell am start -n app.perch/.DemoActivity --ei track 1 --ez playing true --el pos 78000
 *   adb shell am start -n app.perch/.DemoActivity --ez stop true
 */
class DemoActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra("stop", false)) DemoSession.stop()
        else DemoSession.start(
            this,
            intent.getIntExtra("track", 1),
            intent.getBooleanExtra("playing", true),
            intent.getLongExtra("pos", 78_000),
        )
        finish()
    }
}

object DemoSession {
    private var session: MediaSession? = null
    private var track = 1

    private class T(val title: String, val artist: String, val durationMs: Long, val a: Int, val b: Int, val accent: Int)

    private val tracks = listOf(
        T("Evening Light", "Sample Artist", 222_000, 0xFF1B2A49.toInt(), 0xFFF2994A.toInt(), 0xFFFFD2A0.toInt()),
        T("Paper Boats", "The Placeholders", 187_000, 0xFF0F3D3E.toInt(), 0xFF7BDFF2.toInt(), 0xFFB2F7EF.toInt()),
        T("Slow Orbit", "Example Quartet", 251_000, 0xFF2B1B4A.toInt(), 0xFFE0549C.toInt(), 0xFFFFC2E2.toInt()),
    )

    fun stop() {
        session?.release()
        session = null
    }

    fun start(ctx: Context, index: Int, playing: Boolean, positionMs: Long) {
        track = index.coerceIn(1, tracks.size)
        stop()
        val t = tracks[track - 1]
        val s = MediaSession(ctx.applicationContext, "perch-demo")
        s.setCallback(object : MediaSession.Callback() {
            override fun onPlay() = state(s, true, position(s))
            override fun onPause() = state(s, false, position(s))
            override fun onSeekTo(pos: Long) = state(s, s.controller.playbackState?.state == PlaybackState.STATE_PLAYING, pos)
        })
        s.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, t.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, t.artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, "Demo")
                .putLong(MediaMetadata.METADATA_KEY_DURATION, t.durationMs)
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, cover(t))
                .build()
        )
        s.isActive = true
        state(s, playing, positionMs)
        session = s
    }

    private fun position(s: MediaSession): Long {
        val p = s.controller.playbackState ?: return 0
        return if (p.state == PlaybackState.STATE_PLAYING) p.position + (SystemClock.elapsedRealtime() - p.lastPositionUpdateTime) else p.position
    }

    private fun state(s: MediaSession, playing: Boolean, pos: Long) {
        s.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS or PlaybackState.ACTION_SEEK_TO
                )
                .setState(if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED, pos, 1f, SystemClock.elapsedRealtime())
                .build()
        )
    }

    /** An abstract dusk: a gradient, a sun and layered hills. Made here, so nothing in it is anyone's artwork. */
    private fun cover(t: T): Bitmap {
        val n = 640
        val bmp = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, 0f, n.toFloat(), t.a, t.b, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, n.toFloat(), n.toFloat(), p)
        p.shader = RadialGradient(n * 0.62f, n * 0.42f, n * 0.26f, intArrayOf(t.accent, Color.argb(0, Color.red(t.accent), Color.green(t.accent), Color.blue(t.accent))), null, Shader.TileMode.CLAMP)
        c.drawCircle(n * 0.62f, n * 0.42f, n * 0.26f, p)
        p.shader = null
        p.color = t.accent
        c.drawCircle(n * 0.62f, n * 0.42f, n * 0.11f, p)
        for ((i, shade) in listOf(0.35f, 0.55f, 0.75f).withIndex()) {
            p.color = Color.argb(255, (Color.red(t.a) * (1 - shade)).toInt(), (Color.green(t.a) * (1 - shade)).toInt(), (Color.blue(t.a) * (1 - shade)).toInt())
            val path = android.graphics.Path()
            val base = n * (0.62f + 0.12f * i)
            path.moveTo(0f, n.toFloat())
            path.lineTo(0f, base)
            path.cubicTo(n * 0.25f, base - n * 0.12f, n * 0.5f, base + n * 0.05f, n.toFloat(), base - n * 0.08f)
            path.lineTo(n.toFloat(), n.toFloat())
            path.close()
            c.drawPath(path, p)
        }
        return bmp
    }
}
