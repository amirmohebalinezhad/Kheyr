package com.kheyr.sms.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kheyr.sms.settings.ThemePreference
import com.kheyr.sms.settings.ThemePreferenceResolver

private val KheyrShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

private val LightColorScheme = lightColorScheme(
    primary = KheyrColors.Primary,
    onPrimary = KheyrColors.OnPrimary,
    primaryContainer = KheyrColors.OutgoingBubbleLight,
    onPrimaryContainer = Color(0xFF003738),
    surface = KheyrColors.GlassSurfaceLight,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = KheyrColors.SearchFieldLight,
    onSurfaceVariant = Color(0xFF49454F),
    background = Color(0xFFF7F7F7),
    onBackground = Color(0xFF1C1B1F),
)

private val DarkColorScheme = darkColorScheme(
    primary = KheyrColors.Primary,
    onPrimary = KheyrColors.OnPrimary,
    primaryContainer = KheyrColors.OutgoingBubbleDark,
    onPrimaryContainer = Color(0xFFB8E8E9),
    surface = KheyrColors.GlassSurfaceDark,
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = KheyrColors.SearchFieldDark,
    onSurfaceVariant = Color(0xFFCAC4D0),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE6E1E5),
)

@Composable
fun KheyrTheme(
    themePreference: ThemePreference = ThemePreference.System,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = ThemePreferenceResolver.isDark(themePreference, systemDark)
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KheyrTypography.typography,
        shapes = KheyrShapes,
        content = content,
    )
}

@Composable
fun isKheyrDarkTheme(themePreference: ThemePreference = ThemePreference.System): Boolean {
    val systemDark = isSystemInDarkTheme()
    return ThemePreferenceResolver.isDark(themePreference, systemDark)
}
