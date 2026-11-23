package app.perch.ui

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.perch.DemoHooks
import app.perch.MediaWatcher
import app.perch.NowPlaying
import app.perch.Policy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class Transport(
    val play: () -> Unit,
    val pause: () -> Unit,
    val next: () -> Unit,
    val previous: () -> Unit,
    val seekTo: (Long) -> Unit,
)

private val sources = mapOf(
    MediaWatcher.SPOTIFY to "Spotify",
    "com.google.android.apps.youtube.music" to "YouTube Music",
    "com.google.android.youtube" to "YouTube",
)

@Composable
fun PlayerScreen(now: NowPlaying, transport: Transport, deviceName: String, modifier: Modifier = Modifier) {
    val track = now.track
    val raw by produceState(ArtColors.Default, track?.art) {
        value = withContext(Dispatchers.Default) { artColors(track?.art) }
    }
    val top by animateColorAsState(raw.top, tween(900), label = "top")
    val bottom by animateColorAsState(raw.bottom, tween(900), label = "bottom")
    val accent by animateColorAsState(raw.accent, tween(900), label = "accent")
    val onAccent by animateColorAsState(raw.onAccent, tween(900), label = "onAccent")

    // Smooth progress: re-read the extrapolated position a few times a second.
    val clock by produceState(SystemClock.elapsedRealtime()) {
        while (true) { value = SystemClock.elapsedRealtime(); delay(250) }
    }
    val duration = track?.durationMs ?: 0
    val position = now.positionAt(clock)
    val progress = if (duration > 0) position.toFloat() / duration else 0f

    Box(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(top, bottom)))) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 32.dp, vertical = 24.dp)) {
            val landscape = maxWidth > maxHeight
            val art: @Composable (Modifier) -> Unit = { m -> Artwork(now, accent, transport, m) }
            val details: @Composable (Modifier, Boolean) -> Unit = { m, center ->
                Column(m, horizontalAlignment = Alignment.Start) {
                    TitleBlock(now, false)
                    Spacer(Modifier.height(20.dp))
                    WavyProgress(
                        progress = progress,
                        playing = now.isPlaying,
                        color = accent,
                        trackColor = Color.White.copy(alpha = 0.22f),
                        seekable = now.canSeek && duration > 0,
                        onSeek = { transport.seekTo((it * duration).toLong()) },
                    )
                    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), Arrangement.SpaceBetween) {
                        Text(Policy.formatTime(position), style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.7f))
                        Text(if (duration > 0) Policy.formatTime(duration) else "", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.7f))
                    }
                    Spacer(Modifier.height(20.dp))
                    Controls(now, accent, onAccent, transport, center)
                }
            }

            if (landscape) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                    art(Modifier.fillMaxHeight(0.86f).aspectRatio(1f))
                    details(Modifier.weight(1f), false)
                }
            } else {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    SourceChip(now, deviceName)
                    Spacer(Modifier.weight(1f))
                    art(Modifier.fillMaxWidth().widthIn(max = 520.dp).aspectRatio(1f))
                    Spacer(Modifier.height(32.dp))
                    details(Modifier.fillMaxWidth().widthIn(max = 560.dp), true)
                    Spacer(Modifier.weight(1.1f))
                }
            }
        }
    }
}

@Composable
private fun SourceChip(now: NowPlaying, deviceName: String) {
    val name = DemoHooks.sourceName ?: sources[now.sessionPkg] ?: now.sessionPkg?.substringAfterLast('.')?.replaceFirstChar { it.uppercase() } ?: ""
    Row(
        Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.10f)).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (now.isPlaying) Color(0xFF6FE39A) else Color.White.copy(alpha = 0.4f)))
        Spacer(Modifier.size(10.dp))
        Text("$name  ·  $deviceName", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
    }
}

@Composable
private fun Artwork(now: NowPlaying, accent: Color, transport: Transport, modifier: Modifier) {
    val shape = RoundedCornerShape(36.dp)
    val bitmap = now.track?.art
    Box(
        modifier
            .shadow(28.dp, shape, ambientColor = accent, spotColor = accent)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.08f))
            .pointerInput(now.canNext, now.canPrev) {
                var dx = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dx = 0f },
                    onDragEnd = {
                        if (dx < -SWIPE_PX && now.canNext) transport.next()
                        if (dx > SWIPE_PX && now.canPrev) transport.previous()
                    },
                ) { _, d -> dx += d }
            },
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(bitmap, animationSpec = tween(500), label = "art") { b ->
            if (b != null) {
                Image(b.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { NoteGlyph(Color.White.copy(alpha = 0.35f)) }
            }
        }
    }
}

@Composable
private fun TitleBlock(now: NowPlaying, center: Boolean) {
    val title = now.track?.title?.ifBlank { null } ?: "Nothing playing"
    val artist = now.track?.artist.orEmpty()
    AnimatedContent(
        targetState = title to artist,
        transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(200)) },
        label = "title",
    ) { (t, a) ->
        Column(Modifier.fillMaxWidth(), horizontalAlignment = if (center) Alignment.CenterHorizontally else Alignment.Start) {
            Text(
                t, style = MaterialTheme.typography.headlineMedium, color = Color.White,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                textAlign = if (center) TextAlign.Center else TextAlign.Start,
            )
            if (a.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    a, style = MaterialTheme.typography.titleLarge, color = Color.White.copy(alpha = 0.72f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun Controls(now: NowPlaying, accent: Color, onAccent: Color, t: Transport, center: Boolean) {
    // The play button is a circle when paused and a squircle while playing, and morphs between them.
    val corner by animateIntAsState(if (now.isPlaying) 34 else 50, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow), label = "corner")
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(
            CONTROL_GAP, if (center) Alignment.CenterHorizontally else Alignment.Start,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundButton(enabled = now.canPrev, onClick = t.previous) { SkipGlyph(next = false, color = Color.White) }
        Box(
            Modifier.size(96.dp).clip(RoundedCornerShape(corner)).background(accent)
                .clickable { if (now.isPlaying) t.pause() else t.play() },
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(now.isPlaying, label = "playpause") { playing ->
                if (playing) PauseGlyph(onAccent, 52.dp) else PlayGlyph(onAccent, 52.dp)
            }
        }
        RoundButton(enabled = now.canNext, onClick = t.next) { SkipGlyph(next = true, color = Color.White) }
    }
}

@Composable
private fun RoundButton(enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.size(68.dp).clip(CircleShape).background(Color.White.copy(alpha = if (enabled) 0.12f else 0.05f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) { content() } }
}

private const val SWIPE_PX = 140f

/** Space between the previous, play and next buttons: wide enough that a thumb cannot hit the wrong one. */
private val CONTROL_GAP = 32.dp
