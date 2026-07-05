package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class CustomThemePalette(
    val primary: Color,
    val secondary: Color,
    val backgroundStart: Color,
    val backgroundEnd: Color,
    val surface: Color,
    val border: Color,
    val textPrimary: Color = OnGlassTextPrimary,
    val textSecondary: Color = OnGlassTextSecondary
)

val LocalThemePalette = staticCompositionLocalOf {
    CustomThemePalette(
        primary = SophisticatedPrimary,
        secondary = SophisticatedSecondary,
        backgroundStart = SophisticatedBackgroundStart,
        backgroundEnd = SophisticatedBackgroundEnd,
        surface = SophisticatedSurface,
        border = SophisticatedBorder,
        textPrimary = SophisticatedTextPrimary,
        textSecondary = SophisticatedTextSecondary
    )
}

@Composable
fun BrainTrainerTheme(
    themeName: String = "Sophisticated Dark",
    content: @Composable () -> Unit
) {
    val palette = when (themeName) {
        "Sophisticated Dark" -> CustomThemePalette(
            primary = SophisticatedPrimary,
            secondary = SophisticatedSecondary,
            backgroundStart = SophisticatedBackgroundStart,
            backgroundEnd = SophisticatedBackgroundEnd,
            surface = SophisticatedSurface,
            border = SophisticatedBorder,
            textPrimary = SophisticatedTextPrimary,
            textSecondary = SophisticatedTextSecondary
        )
        "Dark Glass" -> CustomThemePalette(
            primary = GlassPrimary,
            secondary = GlassSecondary,
            backgroundStart = GlassBackgroundStart,
            backgroundEnd = GlassBackgroundEnd,
            surface = GlassSurface,
            border = GlassBorder,
            textPrimary = OnGlassTextPrimary,
            textSecondary = OnGlassTextSecondary
        )
        "Electric Neon" -> CustomThemePalette(
            primary = NeonPrimary,
            secondary = NeonSecondary,
            backgroundStart = NeonBackgroundStart,
            backgroundEnd = NeonBackgroundEnd,
            surface = NeonSurface,
            border = GlassBorder,
            textPrimary = OnGlassTextPrimary,
            textSecondary = OnGlassTextSecondary
        )
        "Sunset Crimson" -> CustomThemePalette(
            primary = SunsetPrimary,
            secondary = SunsetSecondary,
            backgroundStart = SunsetBackgroundStart,
            backgroundEnd = SunsetBackgroundEnd,
            surface = SunsetSurface,
            border = GlassBorder,
            textPrimary = OnGlassTextPrimary,
            textSecondary = OnGlassTextSecondary
        )
        "Emerald Forest" -> CustomThemePalette(
            primary = EmeraldPrimary,
            secondary = EmeraldSecondary,
            backgroundStart = EmeraldBackgroundStart,
            backgroundEnd = EmeraldBackgroundEnd,
            surface = EmeraldSurface,
            border = GlassBorder,
            textPrimary = OnGlassTextPrimary,
            textSecondary = OnGlassTextSecondary
        )
        else -> CustomThemePalette(
            primary = SophisticatedPrimary,
            secondary = SophisticatedSecondary,
            backgroundStart = SophisticatedBackgroundStart,
            backgroundEnd = SophisticatedBackgroundEnd,
            surface = SophisticatedSurface,
            border = SophisticatedBorder,
            textPrimary = SophisticatedTextPrimary,
            textSecondary = SophisticatedTextSecondary
        )
    }

    val colorScheme = darkColorScheme(
        primary = palette.primary,
        secondary = palette.secondary,
        background = palette.backgroundStart,
        surface = palette.surface,
        onPrimary = Color.Black,
        onSecondary = Color.White,
        onBackground = palette.textPrimary,
        onSurface = palette.textPrimary
    )

    CompositionLocalProvider(LocalThemePalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
