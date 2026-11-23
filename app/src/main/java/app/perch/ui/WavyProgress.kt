package app.perch.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Material 3 Expressive style progress: the played part is a travelling wave that flattens when
 * playback pauses, the rest is a straight track. Tap or drag to seek ([onSeek] gets 0..1).
 */
@Composable
fun WavyProgress(
    progress: Float,
    playing: Boolean,
    color: Color,
    trackColor: Color,
    seekable: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val amplitude by animateFloatAsState(if (playing) 1f else 0f, tween(500), label = "amp")
    val phase by rememberInfiniteTransition(label = "wave").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    var dragging by remember { mutableStateOf<Float?>(null) }
    val shown = (dragging ?: progress).coerceIn(0f, 1f)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(36.dp)
            .pointerInput(seekable) {
                if (!seekable) return@pointerInput
                detectTapGestures { onSeek((it.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(seekable) {
                if (!seekable) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { dragging = (it.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = { dragging?.let(onSeek); dragging = null },
                    onDragCancel = { dragging = null },
                ) { change, _ -> dragging = (change.position.x / size.width).coerceIn(0f, 1f) }
            },
    ) {
        val midY = size.height / 2
        val strokeW = 5.dp.toPx()
        val amp = 3.5.dp.toPx() * amplitude
        val wavelength = 26.dp.toPx()
        val endX = size.width * shown
        val gap = 6.dp.toPx()

        // The wave, from the left edge to the playhead.
        val wave = Path()
        var x = 0f
        wave.moveTo(0f, midY)
        while (x <= endX - gap / 2) {
            wave.lineTo(x, midY + amp * sin(x / wavelength * 2 * PI.toFloat() - phase))
            x += 3f
        }
        drawPath(wave, color, style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // The remaining track.
        if (endX + gap < size.width) {
            drawLine(trackColor, Offset(endX + gap, midY), Offset(size.width, midY), 4.dp.toPx(), StrokeCap.Round)
        }

        // The playhead: a tall pill.
        val ph = 22.dp.toPx()
        val pw = 5.dp.toPx()
        drawRoundRect(color, Offset(endX - pw / 2, midY - ph / 2), Size(pw, ph), CornerRadius(pw / 2))
    }
}
