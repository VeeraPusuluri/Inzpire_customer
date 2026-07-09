package com.inzpire.customer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    primaryContainer = SkySoft,
    onPrimaryContainer = Navy,
    secondary = Gold,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE1EC),
    onSecondaryContainer = GoldDeep,
    background = Background,
    onBackground = Foreground,
    surface = Color.White,
    onSurface = Foreground,
    surfaceVariant = Surface,
    onSurfaceVariant = MutedForeground,
    outline = Border,
    error = Destructive,
    onError = Color.White,
)

@Composable
fun InzpireCustomerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = InzpireTypography,
        content = content,
    )
}
