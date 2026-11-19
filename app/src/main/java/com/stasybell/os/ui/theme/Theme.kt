package com.stasybell.os.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ForestLightColorScheme = lightColorScheme(
    primary = ForestPalette.Primary,
    onPrimary = Color.White,
    primaryContainer = ForestPalette.MintSoft,
    onPrimaryContainer = ForestPalette.PrimaryDeep,
    secondary = ForestPalette.Green,
    onSecondary = Color.White,
    secondaryContainer = ForestPalette.Mint,
    onSecondaryContainer = ForestPalette.Forest,
    tertiary = ForestPalette.Info,
    onTertiary = Color.White,
    background = ForestPalette.Canvas,
    onBackground = ForestPalette.Ink,
    surface = ForestPalette.Surface,
    onSurface = ForestPalette.Ink,
    surfaceVariant = ForestPalette.CanvasSoft,
    onSurfaceVariant = ForestPalette.Body,
    outline = ForestPalette.Border,
    outlineVariant = ForestPalette.Sage,
    error = ForestPalette.Error,
    onError = Color.White,
    errorContainer = ForestPalette.ErrorBg,
    onErrorContainer = ForestPalette.Error
)

private val ForestShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

@Composable
fun StasyBellTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ForestLightColorScheme,
        typography = StasyBellTypography,
        shapes = ForestShapes,
        content = content
    )
}
