package no.tilecam.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

val Ink950 = Color(0xFF0B0E14)
val Ink900 = Color(0xFF10151F)
val Ink850 = Color(0xFF131A26)
val Ink800 = Color(0xFF161C28)
val Ink700 = Color(0xFF212A3A)
val Accent = Color(0xFF3DDC97)
val AccentDim = Color(0xFF1E6B4C)
val TextHigh = Color(0xFFF4F6FB)
val TextMid = Color(0xFF9AA7BD)
val Danger = Color(0xFFFF6B6B)

private val TilecamColors = darkColorScheme(
    primary = Accent,
    onPrimary = Ink950,
    secondary = Ink700,
    onSecondary = TextHigh,
    background = Ink950,
    onBackground = TextHigh,
    surface = Ink900,
    onSurface = TextHigh,
    surfaceVariant = Ink800,
    onSurfaceVariant = TextMid,
    border = Ink700,
    error = Danger,
)

@Composable
fun TilecamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TilecamColors,
        typography = TilecamTypography,
        content = content,
    )
}
