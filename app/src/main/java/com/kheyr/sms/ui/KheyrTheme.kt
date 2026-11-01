package com.kheyr.sms.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.kheyr.sms.settings.ThemePreference
import com.kheyr.sms.settings.ThemePreferenceResolver

/** True when the active theme is dark. Lets custom-drawn surfaces pick the right gradient. */
val LocalKheyrDarkTheme = staticCompositionLocalOf { false }

private val KheyrShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0C7A6D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD2F4EA),
    onPrimaryContainer = Color(0xFF04302A),
    inversePrimary = Color(0xFF6FE7CF),
    secondary = Color(0xFF4E625C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCEAE4),
    onSecondaryContainer = Color(0xFF0C1F1A),
    tertiary = Color(0xFFB4772B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFBE8CF),
    onTertiaryContainer = Color(0xFF3B2300),
    background = Color(0xFFF5F8F6),
    onBackground = Color(0xFF0F1A17),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F1A17),
    surfaceVariant = Color(0xFFE3EAE6),
    onSurfaceVariant = Color(0xFF53625C),
    surfaceTint = Color(0xFF0C7A6D),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F5F2),
    surfaceContainer = Color(0xFFEBF1ED),
    surfaceContainerHigh = Color(0xFFE5ECE8),
    surfaceContainerHighest = Color(0xFFDFE7E2),
    outline = Color(0xFFB4C1BA),
    outlineVariant = Color(0xFFD8E1DC),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFE9E5),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = Color(0xFF22302B),
    inverseOnSurface = Color(0xFFEAF3EE),
    scrim = Color(0xFF000000),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF64E3CC),
    onPrimary = Color(0xFF003730),
    primaryContainer = Color(0xFF0B4A42),
    onPrimaryContainer = Color(0xFF9FF5E4),
    inversePrimary = Color(0xFF0C7A6D),
    secondary = Color(0xFFB6CCC4),
    onSecondary = Color(0xFF1B2A25),
    secondaryContainer = Color(0xFF283B35),
    onSecondaryContainer = Color(0xFFD2E8DF),
    tertiary = Color(0xFFE9B872),
    onTertiary = Color(0xFF3F2A00),
    tertiaryContainer = Color(0xFF5A3F12),
    onTertiaryContainer = Color(0xFFFFDDA8),
    background = Color(0xFF070C0B),
    onBackground = Color(0xFFE2EDE8),
    surface = Color(0xFF0D1412),
    onSurface = Color(0xFFE2EDE8),
    surfaceVariant = Color(0xFF24322D),
    onSurfaceVariant = Color(0xFF9BADA5),
    surfaceTint = Color(0xFF64E3CC),
    surfaceContainerLowest = Color(0xFF070B0A),
    surfaceContainerLow = Color(0xFF101816),
    surfaceContainer = Color(0xFF141E1B),
    surfaceContainerHigh = Color(0xFF1A2621),
    surfaceContainerHighest = Color(0xFF21302A),
    outline = Color(0xFF3B4B45),
    outlineVariant = Color(0xFF27352F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFE2EDE8),
    inverseOnSurface = Color(0xFF1B2A25),
    scrim = Color(0xFF000000),
)

@Composable
fun KheyrTheme(
    themePreference: ThemePreference = ThemePreference.System,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = ThemePreferenceResolver.isDark(themePreference, systemDark)
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    SyncSystemBarAppearance(useDarkTheme = darkTheme)

    CompositionLocalProvider(LocalKheyrDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = KheyrTypography.typography,
            shapes = KheyrShapes,
            content = content,
        )
    }
}

@Composable
fun isKheyrDarkTheme(themePreference: ThemePreference = ThemePreference.System): Boolean {
    val systemDark = isSystemInDarkTheme()
    return ThemePreferenceResolver.isDark(themePreference, systemDark)
}

@Composable
private fun SyncSystemBarAppearance(useDarkTheme: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !useDarkTheme
                isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }
}
