package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = PureWhiteSurface,
    primaryContainer = Color(0xFFD6F5E6),
    onPrimaryContainer = EmeraldDark,
    secondary = OpticVolt,
    onSecondary = OpticVoltDarkText,
    secondaryContainer = OpticVoltMuted,
    onSecondaryContainer = OpticVoltDarkText,
    tertiary = PeakAmber,
    onTertiary = PureWhiteSurface,
    tertiaryContainer = PeakAmberBg,
    onTertiaryContainer = Color(0xFF78350F),
    background = AlabasterCanvas,
    onBackground = SlateInk,
    surface = PureWhiteSurface,
    onSurface = SlateInk,
    surfaceVariant = CourtstoneGray,
    onSurfaceVariant = MutedMoss,
    outline = HairlineBorder,
    outlineVariant = SubtleAsh,
    error = MaintenanceRed,
    onError = PureWhiteSurface,
    errorContainer = MaintenanceRedBg,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkColorScheme = darkColorScheme(
    primary = OpticVolt,
    onPrimary = OpticVoltDarkText,
    primaryContainer = EmeraldPrimary,
    onPrimaryContainer = PureWhiteSurface,
    secondary = EmeraldPrimary,
    onSecondary = PureWhiteSurface,
    secondaryContainer = Color(0xFF1A3A2E),
    onSecondaryContainer = Color(0xFFD6F5E6),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF451A03),
    background = EmeraldDeepSurface,
    onBackground = Color(0xFFF2F6F4),
    surface = EmeraldCardDark,
    onSurface = Color(0xFFF2F6F4),
    surfaceVariant = Color(0xFF1C332B),
    onSurfaceVariant = Color(0xFFA3B8B0),
    outline = Color(0xFF284238),
    outlineVariant = Color(0xFF4A6359),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

@Composable
fun PicklePlayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
