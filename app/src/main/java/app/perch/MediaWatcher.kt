package app.perch

import android.app.Application
import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Follows the media session of whatever app is playing (the tablet's own Spotify app, normally,
 * once a phone has chosen "This tablet" in Spotify Connect) and exposes it as [NowPlaying].
 *
 * Reading other apps' sessions needs notification access; [start] is a no-op until it is granted
 * and is called again whenever [PerchListener] connects.
 */
class MediaWatcher(private val app: Application, private val scope: CoroutineScope) {
    private val msm = app.getSystemService(MediaSessionManager::class.java)
    private val component = ComponentName(app, PerchListener::class.java)

    private val _state = MutableStateFlow(NowPlaying(lastPlayingAt = SystemClock.elapsedRealtime()))
    val state: StateFlow<NowPlaying> = _state

    private var controller: MediaController? = null
    private var started = false
    private var artJob: Job? = null
    private var artFor: String? = null

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { pick(it.orEmpty()) }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publishMeta(metadata)
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            publishPlayback(state)
            // A different app may have taken over playback; the sessions list does not change then.
            if (state?.state != PlaybackState.STATE_PLAYING) refresh()
        }
        override fun onSessionDestroyed() = refresh()
    }

    fun start() {
        if (started) { refresh(); return }
        try {
            msm.addOnActiveSessionsChangedListener(sessionsListener, component)
            started = true
            refresh()
        } catch (e: SecurityException) {
            Log.i(TAG, "notification access not granted yet")
        }
    }

    fun refresh() {
        try {
            pick(msm.getActiveSessions(component))
        } catch (e: SecurityException) {
            started = false
        }
    }

    // --- transport -------------------------------------------------------------------------

    fun play() = controller?.transportControls?.play()
    fun pause() = controller?.transportControls?.pause()
    fun next() = controller?.transportControls?.skipToNext()
    fun previous() = controller?.transportControls?.skipToPrevious()
    fun seekTo(ms: Long) = controller?.transportControls?.seekTo(ms)

    // --- session selection -----------------------------------------------------------------

    private fun pick(all: List<MediaController>) {
        val candidates = all.filter { DemoHooks.allowOwnSession || it.packageName != app.packageName }
        val best = candidates.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: candidates.firstOrNull { it.packageName == SPOTIFY }
            ?: candidates.firstOrNull()

        if (best?.sessionToken == controller?.sessionToken && best != null) return

        controller?.unregisterCallback(callback)
        controller = best
        artJob?.cancel(); artFor = null
        if (best == null) {
            _state.value = NowPlaying(lastPlayingAt = _state.value.lastPlayingAt)
            return
        }
        best.registerCallback(callback)
        _state.value = NowPlaying(sessionPkg = best.packageName, lastPlayingAt = _state.value.lastPlayingAt)
        publishMeta(best.metadata)
        publishPlayback(best.playbackState)
    }

    // --- publishing ------------------------------------------------------------------------

    private fun publishPlayback(s: PlaybackState?) {
        val now = SystemClock.elapsedRealtime()
        val cur = _state.value
        val playback = s?.let { Playback(it.state, it.position, it.lastPositionUpdateTime.takeIf { t -> t > 0 } ?: now, it.playbackSpeed) }
        val playing = s?.state == PlaybackState.STATE_PLAYING
        _state.value = cur.copy(
            playback = playback,
            actions = s?.actions ?: 0,
            lastPlayingAt = if (playing) now else cur.lastPlayingAt,
        )
    }

    private fun publishMeta(m: MediaMetadata?) {
        if (m == null) {
            _state.value = _state.value.copy(track = null)
            return
        }
        val title = m.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) ?: m.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        val artist = m.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: m.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: m.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE) ?: ""
        val album = m.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
        val key = m.getString(MediaMetadata.METADATA_KEY_MEDIA_ID) ?: "$title|$artist|$album"

        // Spotify re-sends the metadata many times per song, some with a 240 px thumbnail, some with
        // none. Never let a smaller picture replace the one we already hold for the same song.
        val previous = _state.value.track?.takeIf { it.key == key }?.art
        val bitmap = biggest(
            previous,
            m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART),
            m.getBitmap(MediaMetadata.METADATA_KEY_ART),
            m.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON),
        )

        _state.value = _state.value.copy(
            track = Track(key, title, artist, album, bitmap, m.getLong(MediaMetadata.METADATA_KEY_DURATION)),
        )

        // Spotify also hands out a content:// URI to the full 640 px cover: fetch it unless a bitmap
        // this size or larger is already held.
        val uri = m.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            ?: m.getString(MediaMetadata.METADATA_KEY_ART_URI)
            ?: m.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)
        val want = if (uri != null && (bitmap == null || bitmap.width < MIN_ART_PX)) "$key|$uri" else null
        if (want == null) {
            artJob?.cancel(); artFor = null
        } else if (want != artFor) {
            artJob?.cancel(); artFor = want
            artJob = scope.launch {
                val loaded = withContext(Dispatchers.IO) { load(uri!!) }
                if (loaded == null) { artFor = null; return@launch }
                val t = _state.value.track ?: return@launch
                if (t.key == key) _state.value = _state.value.copy(track = t.copy(art = loaded))
            }
        }
    }

    private fun biggest(vararg b: Bitmap?): Bitmap? = b.filterNotNull().maxByOrNull { it.width * it.height }

    /** Spotify serves the full 640 px cover through its own content provider; other schemes are ignored. */
    private fun load(uri: String): Bitmap? = runCatching {
        val u = android.net.Uri.parse(uri)
        if (u.scheme != "content") return null
        app.contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it) }
    }.onFailure { Log.i(TAG, "artwork not loadable from $uri: ${it.javaClass.simpleName}") }.getOrNull()

    companion object {
        private const val TAG = "PerchMedia"
        const val SPOTIFY = "com.spotify.music"
        private const val MIN_ART_PX = 500
    }
}
