package br.com.terras.app.dsm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_surface,
    primaryContainer = md_theme_light_primary,
    onPrimaryContainer = md_theme_light_surface,
    secondary = md_theme_light_secondary,
    onSecondary = Color.White,
    secondaryContainer = md_theme_light_secondary_container,
    onSecondaryContainer = md_theme_light_on_secondary_container,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_surface,
    error = md_theme_light_error,
    onError = Color.White,
    background = md_theme_light_background,
    onBackground = md_theme_light_surface,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_surface,
    surfaceVariant = md_theme_light_tertiary,
    onSurfaceVariant = md_theme_light_on_surface_variant,
    surfaceContainer = md_theme_light_primary,
    surfaceContainerHigh = md_theme_light_primary,
    surfaceContainerLow = md_theme_light_background,
    outline = md_theme_light_outline,
    inverseSurface = md_theme_light_inverseSurface,
    inverseOnSurface = Color.White,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
    outlineVariant = md_theme_light_outlineVariant,
    scrim = md_theme_light_scrim,
)

private val DarkColors = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_surface,
    primaryContainer = md_theme_dark_primary,
    onPrimaryContainer = md_theme_dark_surface,
    secondary = md_theme_dark_secondary,
    onSecondary = Color.White,
    secondaryContainer = md_theme_dark_secondary_container,
    onSecondaryContainer = md_theme_dark_on_secondary_container,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_surface,
    error = md_theme_dark_error,
    onError = Color.White,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_surface,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_surface,
    surfaceVariant = md_theme_dark_tertiary,
    onSurfaceVariant = md_theme_dark_on_surface_variant,
    surfaceContainer = md_theme_dark_primary,
    surfaceContainerHigh = md_theme_dark_primary,
    surfaceContainerLow = md_theme_dark_background,
    outline = md_theme_dark_outline,
    inverseSurface = md_theme_dark_inverseSurface,
    inverseOnSurface = md_theme_dark_background,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint,
    outlineVariant = md_theme_dark_outlineVariant,
    scrim = md_theme_dark_scrim,
)

@Composable
fun DSMTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable() () -> Unit
) {
    val colors = if (!useDarkTheme) {
        LightColors
    } else {
        DarkColors
    }

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}