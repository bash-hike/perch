package app.perch.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.perch.NowPlaying
import app.perch.PowerInfo
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * The idle face: a big thin clock and the date on black, in the spirit of the system's always-on
 * display. Stacked hours-over-minutes in portrait, one line in landscape.
 */
@Composable
fun ClockScreen(
    time: LocalDateTime,
    is24h: Boolean,
    power: PowerInfo,
    now: NowPlaying,
    awake: Boolean,
    hasAccess: Boolean,
    onAllowAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hours = if (is24h) "%02d".format(time.hour) else (if (time.hour % 12 == 0) 12 else time.hour % 12).toString()
    val minutes = "%02d".format(time.minute)

    val hourColor = Color.White.copy(alpha = 0.92f)
    val minuteColor by animateColorAsState(
        if (awake) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.50f), tween(600), label = "minutes",
    )

    // A few dp of drift every minute keeps one set of pixels from lighting the same shapes all night.
    val dx = ((time.minute % 5) - 2) * 3
    val dy = (((time.minute / 5) % 3) - 1) * 3

    BoxWithConstraints(modifier.fillMaxSize().background(Color.Black).safeDrawingPadding()) {
        val landscape = maxWidth > maxHeight
        val density = LocalDensity.current
        val size = if (landscape) minOf(maxWidth * 0.85f / 2.7f, maxHeight * 0.5f) else minOf(maxWidth * 0.62f, maxHeight * 0.30f)
        val fontSize = with(density) { size.toSp() }
        val clock = ClockFont.copy(
            fontSize = fontSize,
                        textAlign = TextAlign.Center,
        )

        val tight = Modifier.layout { measurable, constraints ->
            val p = measurable.measure(constraints)
            val h = (p.height * 0.72f).toInt()
            layout(p.width, h) { p.place(0, -((p.height - h) / 2)) }
        }

        Column(
            Modifier.fillMaxSize().offset(dx.dp, dy.dp).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (landscape) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(hours, tight, style = clock, color = hourColor)
                    Text(":", tight, style = clock, color = Color.White.copy(alpha = 0.35f))
                    Text(minutes, tight, style = clock, color = minuteColor)
                }
            } else {
                Text(hours, tight, style = clock, color = hourColor)
                Text(minutes, tight, style = clock, color = minuteColor)
            }

            Spacer(Modifier.height(if (landscape) 12.dp else 28.dp))
            Text(
                time.format(DateTimeFormatter.ofPattern("EEE, d MMM")),
                style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 1.sp),
                color = Color.White.copy(alpha = 0.78f),
            )

            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (power.plugged) {
                    BoltGlyph(Color.White.copy(alpha = 0.6f), 16.dp)
                    Spacer(Modifier.size(4.dp))
                }
                Text("${power.level}%", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.6f))
            }

            val title = now.track?.title
            if (now.hasSession && !title.isNullOrBlank()) {
                Spacer(Modifier.height(18.dp))
                Text(
                    "Paused  ·  $title" + (now.track?.artist?.takeIf { it.isNotBlank() }?.let { " — $it" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.45f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.8f), textAlign = TextAlign.Center,
                )
            }

            if (!hasAccess) {
                Spacer(Modifier.height(24.dp))
                OutlinedButton(onClick = onAllowAccess) { Text("Allow media access") }
            }
        }
    }
}
