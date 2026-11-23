package app.perch.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette

/** The colours the player takes from the cover: a dark gradient behind a light accent. */
data class ArtColors(val top: Color, val bottom: Color, val accent: Color, val onAccent: Color) {
    companion object {
        val Default = ArtColors(Color(0xFF262A31), Color(0xFF0B0C0E), Color(0xFFB9C8FF), Color(0xFF0B0C0E))
    }
}

fun artColors(bitmap: Bitmap?): ArtColors {
    if (bitmap == null) return ArtColors.Default
    val palette = Palette.from(bitmap).maximumColorCount(18).resizeBitmapArea(160 * 160).generate()
    val dominant = palette.dominantSwatch?.rgb ?: return ArtColors.Default
    val seed = palette.vibrantSwatch?.rgb ?: palette.lightVibrantSwatch?.rgb ?: dominant
    return ArtColors(
        top = tone(dominant, maxSat = 0.55f, light = 0.25f),
        bottom = tone(dominant, maxSat = 0.45f, light = 0.065f),
        // Light enough to read on the dark gradient whatever the cover's colour.
        accent = tone(seed, maxSat = 1f, light = 0.80f, minSat = 0.0f),
        onAccent = tone(dominant, maxSat = 0.5f, light = 0.09f),
    )
}

/** Keeps the colour's hue, bounds its saturation and fixes its lightness. */
private fun tone(argb: Int, maxSat: Float, light: Float, minSat: Float = 0f): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(argb, hsl)
    hsl[1] = hsl[1].coerceIn(minSat, maxSat)
    hsl[2] = light
    return Color(ColorUtils.HSLToColor(hsl))
}
