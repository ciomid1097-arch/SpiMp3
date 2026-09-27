package com.spimp3.app.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// ---- Dark palette: true OLED black, neutral greys (no navy cast) ----
val Bg = Color(0xFF000000)
val Surface = Color(0xFF0A0A0A)
val SurfaceHigh = Color(0xFF151515)
val Outline = Color(0xFF262626)
val TextPrimary = Color(0xFFF5F5F7)
val TextSecondary = Color(0xFF9B9BA3)

// ---- Light palette ----
val LBackground = Color(0xFFFFFFFF)
val LSurface = Color(0xFFFFFFFF)
val LSurfaceHigh = Color(0xFFF2F2F6)
val LSurfaceHighest = Color(0xFFE8E8EF)
val LOutline = Color(0xFFDFDFE6)
val LTextPrimary = Color(0xFF0F0F14)
val LTextSecondary = Color(0xFF63636F)

val AccentGreen = Color(0xFF22C55E)
val AccentBlue = Color(0xFF38BDF8)
val AccentPurple = Color(0xFFA78BFA)
val AccentAmber = Color(0xFFFBBF24)
val AccentRose = Color(0xFFFB7185)
val OnAccent = Color(0xFF052E16)

data class ExtendedColors(val accent: Color, val onAccent: Color)

val LocalExtendedColors = staticCompositionLocalOf { ExtendedColors(AccentGreen, OnAccent) }

object Accents {
    val all = linkedMapOf(
        "green" to AccentGreen,
        "blue" to AccentBlue,
        "purple" to AccentPurple,
        "amber" to AccentAmber,
        "rose" to AccentRose,
    )
}

/** How the user wants the app themed. Persisted in DataStore as a string. */
enum class ThemeMode(val key: String, val label: String) {
    DARK("dark", "Dark"),
    LIGHT("light", "Light"),
    SYSTEM("system", "System");

    companion object {
        fun from(key: String?) = entries.firstOrNull { it.key == key } ?: DARK
    }
}

/**
 * Darkens a colour toward black by [amount] (0..1), used for the light theme
 * where a bright accent cannot carry white text at a readable contrast.
 */
private fun Color.darken(amount: Float): Color =
    Color(
        red = (red * (1 - amount)).coerceIn(0f, 1f),
        green = (green * (1 - amount)).coerceIn(0f, 1f),
        blue = (blue * (1 - amount)).coerceIn(0f, 1f),
        alpha = alpha,
    )

/** Mixes the accent a little toward the surface so it reads well on white. */
private fun Color.forLightBackground(): Color = lerp(this, Color.Black, 0.32f)

private fun darkScheme(accent: Color): ColorScheme = darkColorScheme(
    primary = accent,
    onPrimary = OnAccent,
    primaryContainer = SurfaceHigh,
    onPrimaryContainer = TextPrimary,
    secondary = SurfaceHigh,
    onSecondary = TextPrimary,
    background = Bg,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = Surface,
    surfaceContainerHigh = SurfaceHigh,
    surfaceContainerHighest = SurfaceHigh,
    outline = Outline,
    outlineVariant = Outline,
    error = AccentRose,
    onError = Color.Black,
)

private fun lightScheme(accent: Color): ColorScheme {
    val primary = accent.forLightBackground()
    return lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = LSurfaceHighest,
        onPrimaryContainer = LTextPrimary,
        secondary = primary,
        onSecondary = Color.White,
        background = LBackground,
        onBackground = LTextPrimary,
        surface = LSurface,
        onSurface = LTextPrimary,
        surfaceVariant = LSurfaceHigh,
        onSurfaceVariant = LTextSecondary,
        surfaceContainer = LSurface,
        surfaceContainerHigh = LSurfaceHigh,
        surfaceContainerHighest = LSurfaceHighest,
        outline = LOutline,
        outlineVariant = LOutline,
        error = Color(0xFFD92D20),
        onError = Color.White,
    )
}

@Composable
fun SpiMp3Theme(
    accentKey: String = "green",
    themeMode: ThemeMode = ThemeMode.DARK,
    systemInDark: Boolean = true,
    content: @Composable () -> Unit,
) {
    val accent = Accents.all[accentKey] ?: AccentGreen
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemInDark
    }
    val onAccent = if (useDark) OnAccent else Color.White
    val colors = if (useDark) darkScheme(accent) else lightScheme(accent)
    androidx.compose.runtime.CompositionLocalProvider(
        LocalExtendedColors provides ExtendedColors(accent, onAccent),
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = AppTypography,
        ) {
            // A root Surface is mandatory here: without it LocalContentColor
            // defaults to Color.Black, so any Text that omits an explicit color
            // renders invisible on our black background.
            Surface(
                color = colors.background,
                contentColor = colors.onSurface,
                modifier = Modifier.fillMaxSize(),
            ) {
                content()
            }
        }
    }
}
