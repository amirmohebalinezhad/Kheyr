package com.kheyr.sms.ui

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    tonalElevation: Dp = 0.dp,
    blurRadius: Float = 20f,
    content: @Composable () -> Unit,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.graphicsLayer {
            renderEffect = RenderEffect
                .createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP)
                .asComposeRenderEffect()
        }
    } else {
        Modifier
    }

    Surface(
        modifier = modifier.then(blurModifier),
        color = surfaceColor,
        tonalElevation = tonalElevation,
        content = content,
    )
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
