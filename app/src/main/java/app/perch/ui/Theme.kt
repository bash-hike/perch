package app.perch.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val base = Typography()

/** Wallpaper-derived Material You colours (always dark: this is a screen for a dim room). */
@Composable
fun PerchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = dynamicDarkColorScheme(LocalContext.current),
        typography = base.copy(
            headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
            headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
            titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Normal),
            labelLarge = base.labelLarge.copy(letterSpacing = 0.6.sp),
        ),
        content = content,
    )
}

/** Clock digits: thin and tall, like the system's always-on display. */
val ClockFont = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.ExtraLight,
    letterSpacing = (-2).sp,
)
