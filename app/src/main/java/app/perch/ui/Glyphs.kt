package app.perch.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Transport and status glyphs drawn on a canvas: crisp at any size, no icon-font dependency. */

private fun DrawScope.roundedPoly(points: List<Offset>, color: Color, round: Float) {
    val p = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
        close()
    }
    drawPath(p, color, style = Fill)
    drawPath(p, color, style = Stroke(width = round, join = StrokeJoin.Round, cap = StrokeCap.Round))
}

@Composable
fun PlayGlyph(color: Color, size: Dp = 40.dp) = Canvas(Modifier.size(size)) {
    val s = this.size.minDimension
    val r = s * 0.07f
    roundedPoly(
        listOf(Offset(s * 0.30f, s * 0.20f + r / 2), Offset(s * 0.30f, s * 0.80f - r / 2), Offset(s * 0.80f - r / 2, s * 0.5f)),
        color, r,
    )
}

@Composable
fun PauseGlyph(color: Color, size: Dp = 40.dp) = Canvas(Modifier.size(size)) {
    val s = this.size.minDimension
    val w = s * 0.17f
    val top = s * 0.22f
    val h = s * 0.56f
    drawRoundRect(color, Offset(s * 0.29f, top), Size(w, h), CornerRadius(w * 0.4f))
    drawRoundRect(color, Offset(s * 0.71f - w, top), Size(w, h), CornerRadius(w * 0.4f))
}

/** Skip: a triangle against a bar; [next] points right, otherwise left. */
@Composable
fun SkipGlyph(next: Boolean, color: Color, size: Dp = 32.dp) = Canvas(Modifier.size(size)) {
    val s = this.size.minDimension
    val r = s * 0.07f
    val flip = if (next) 1f else -1f
    fun x(f: Float) = s * 0.5f + flip * (f - 0.5f) * s
    roundedPoly(
        listOf(Offset(x(0.24f), s * 0.24f + r / 2), Offset(x(0.24f), s * 0.76f - r / 2), Offset(x(0.66f) - flip * r / 2, s * 0.5f)),
        color, r,
    )
    val bw = s * 0.11f
    drawRoundRect(color, Offset(if (next) x(0.74f) else x(0.74f) - bw, s * 0.24f), Size(bw, s * 0.52f), CornerRadius(bw * 0.45f))
}

/** Three sliders: the settings glyph. */
@Composable
fun TuneGlyph(color: Color, size: Dp = 24.dp) = Canvas(Modifier.size(size)) {
    val s = this.size.minDimension
    val stroke = s * 0.085f
    val rows = listOf(0.28f to 0.35f, 0.5f to 0.68f, 0.72f to 0.45f)
    for ((y, knob) in rows) {
        drawLine(color, Offset(s * 0.16f, s * y), Offset(s * 0.84f, s * y), stroke, StrokeCap.Round)
        drawCircle(color, s * 0.1f, Offset(s * (0.16f + 0.68f * knob), s * y))
    }
}

/** A lightning bolt, for "charging". */
@Composable
fun BoltGlyph(color: Color, size: Dp = 18.dp) = Canvas(Modifier.size(size)) {
    val s = this.size.minDimension
    val p = Path().apply {
        moveTo(s * 0.58f, s * 0.08f)
        lineTo(s * 0.24f, s * 0.56f)
        lineTo(s * 0.47f, s * 0.56f)
        lineTo(s * 0.40f, s * 0.92f)
        lineTo(s * 0.76f, s * 0.42f)
        lineTo(s * 0.53f, s * 0.42f)
        close()
    }
    drawPath(p, color)
}

/** A music note, the artwork placeholder. */
@Composable
fun NoteGlyph(color: Color, size: Dp = 96.dp) = Canvas(Modifier.size(size)) {
    val s = this.size.minDimension
    val stem = s * 0.07f
    drawRoundRect(color, Offset(s * 0.56f, s * 0.2f), Size(stem, s * 0.5f), CornerRadius(stem / 2))
    drawCircle(color, s * 0.13f, Offset(s * 0.46f, s * 0.7f))
    val flag = Path().apply {
        moveTo(s * 0.56f, s * 0.2f)
        cubicTo(s * 0.7f, s * 0.26f, s * 0.78f, s * 0.34f, s * 0.74f, s * 0.5f)
        cubicTo(s * 0.72f, s * 0.4f, s * 0.66f, s * 0.36f, s * 0.56f, s * 0.34f)
        close()
    }
    drawPath(flag, color)
}

/** A cross, for "leave the screen saver". */
@Composable
fun CloseGlyph(color: Color, size: Dp = 24.dp) = Canvas(Modifier.size(size)) {
    val s = this.size.minDimension
    val stroke = s * 0.09f
    drawLine(color, Offset(s * 0.28f, s * 0.28f), Offset(s * 0.72f, s * 0.72f), stroke, StrokeCap.Round)
    drawLine(color, Offset(s * 0.72f, s * 0.28f), Offset(s * 0.28f, s * 0.72f), stroke, StrokeCap.Round)
}
