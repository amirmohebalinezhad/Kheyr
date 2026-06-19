package com.kheyr.sms.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
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
    surface = KheyrColors.SurfaceLight,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = KheyrColors.SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF49454F),
    surfaceContainer = KheyrColors.SurfaceContainerLight,
    surfaceContainerHigh = KheyrColors.GlassSurfaceLight,
    background = KheyrColors.BackgroundLight,
    onBackground = Color(0xFF1C1B1F),
)

private val DarkColorScheme = darkColorScheme(
    primary = KheyrColors.Primary,
    onPrimary = KheyrColors.OnPrimary,
    primaryContainer = KheyrColors.OutgoingBubbleDark,
    onPrimaryContainer = Color(0xFFB8E8E9),
    surface = KheyrColors.SurfaceDark,
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = KheyrColors.SurfaceVariantDark,
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceContainer = KheyrColors.SurfaceContainerDark,
    surfaceContainerHigh = KheyrColors.GlassSurfaceDark,
    background = KheyrColors.BackgroundDark,
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

    SyncSystemBarAppearance(useDarkTheme = darkTheme)

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
