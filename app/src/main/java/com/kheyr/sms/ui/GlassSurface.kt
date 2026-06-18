package com.kheyr.sms.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Frosted chrome: a translucent scrim sits behind sharp foreground content.
 * Blur is not applied to the chrome itself — scrollable content shows through the tint.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    scrimColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        Box(
            Modifier
                .fillMaxSize()
                .background(scrimColor),
        )
        content()
    }
}

@Composable
fun GlassTopBar(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    GlassSurface(modifier = modifier.fillMaxWidth()) {
        Box { content() }
    }
}
