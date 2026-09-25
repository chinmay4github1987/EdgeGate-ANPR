package com.chinmay.edgegate.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Brand colours are fixed on purpose (no Material You dynamic colour): gate decisions
// must look identical on every phone so guards learn them once.
private val LightScheme = lightColorScheme(
    primary = Palette.Teal, onPrimary = Color.White,
    primaryContainer = Palette.TealTint, onPrimaryContainer = Palette.TealDark,
    secondary = Palette.Ink, onSecondary = Color.White,
    secondaryContainer = Palette.Ink, onSecondaryContainer = Color.White,
    background = Palette.Ground, onBackground = Palette.Ink,
    surface = Palette.Surface, onSurface = Palette.Ink,
    surfaceVariant = Palette.Ground, onSurfaceVariant = Palette.Slate,
    surfaceContainer = Palette.Surface, surfaceContainerHigh = Palette.Surface,
    outline = Palette.Line, outlineVariant = Palette.LineSoft,
    error = Palette.Deny, onError = Color.White,
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF5EC4B8), onPrimary = Color(0xFF00332E),
    primaryContainer = Color(0xFF134E48), onPrimaryContainer = Color(0xFFCCEBE7),
    secondary = Color(0xFFE2E8F0), onSecondary = Palette.Ink,
    secondaryContainer = Color(0xFFE2E8F0), onSecondaryContainer = Palette.Ink,
    background = Palette.Night, onBackground = Color(0xFFF1F5F9),
    surface = Palette.NightSurface, onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Palette.Night, onSurfaceVariant = Color(0xFFA3AEC0),
    surfaceContainer = Palette.NightSurface, surfaceContainerHigh = Palette.NightSurface,
    outline = Palette.NightLine, outlineVariant = Color(0xFF1E293B),
    error = Color(0xFFEF4444), onError = Color.White,
)

/** Radii: 12 controls, 16 cards, 24 sheets. */
val EdgeShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Spacing on a 4 dp grid. */
object Space {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val gutter = 20.dp
    val xl = 24.dp
    val touch = 48.dp
}

@Composable
fun EdgeGateTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalEdgeColors provides if (darkTheme) DarkEdgeColors else LightEdgeColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = EdgeTypography,
            shapes = EdgeShapes,
            content = content,
        )
    }
}

/** `EdgeTheme.colors.allow` etc. anywhere inside [EdgeGateTheme]. */
object EdgeTheme {
    val colors: EdgeColors
        @Composable @ReadOnlyComposable get() = LocalEdgeColors.current
}
