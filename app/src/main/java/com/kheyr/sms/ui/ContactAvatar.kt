package com.kheyr.sms.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ContactAvatar(
    displayName: String,
    photoUri: Uri?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val context = LocalContext.current
    var imageBitmap by remember(photoUri) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(photoUri) {
        imageBitmap = withContext(Dispatchers.IO) {
            photoUri?.let { uri ->
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }
    }
    val avatarModifier = modifier.size(size)
    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap!!,
            contentDescription = displayName,
            modifier = avatarModifier.clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
    } else {
        val label = displayName.ifBlank { "?" }
        Surface(
            modifier = avatarModifier,
            shape = CircleShape,
            color = AvatarColor.forKey(label),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = AvatarInitials.from(label),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
