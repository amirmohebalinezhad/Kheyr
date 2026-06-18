package com.kheyr.sms.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Frosted chrome: a translucent scrim sits behind sharp foreground content.
 * The scrim is painted on the same box as the content so chrome height follows
 * its children instead of expanding to the parent's max constraints.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    scrimColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.background(scrimColor)) {
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
