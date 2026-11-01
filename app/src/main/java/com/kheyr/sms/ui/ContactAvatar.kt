package com.kheyr.sms.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
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
    val targetPx = with(LocalDensity.current) { size.roundToPx() }
    // Rows are recycled while scrolling, so a decoded avatar is kept per uri+size instead of
    // being decoded again every time the composable re-enters composition.
    val cacheKey = photoUri?.let { "$it@$targetPx" }
    var imageBitmap by remember(cacheKey) { mutableStateOf(cacheKey?.let(AvatarBitmapCache::get)) }
    LaunchedEffect(cacheKey) {
        val uri = photoUri
        if (cacheKey == null || uri == null) return@LaunchedEffect
        if (AvatarBitmapCache.get(cacheKey) != null) return@LaunchedEffect
        val decoded = withContext(Dispatchers.IO) { decodeScaledAvatar(context, uri, targetPx) }
        if (decoded != null) {
            AvatarBitmapCache.put(cacheKey, decoded)
            imageBitmap = decoded
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
        val base = AvatarColor.forKey(label)
        val phoneLike = isPhoneLike(label)
        val initialsSize = with(LocalDensity.current) { (size * 0.36f).toSp() }
        Box(
            modifier = avatarModifier
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(base, lerp(base, Color.Black, 0.22f)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (phoneLike) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.size(size * 0.44f),
                )
            } else {
                Text(
                    text = AvatarInitials.from(label),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = initialsSize,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

/** Names made only of digits and phone punctuation get a glyph instead of a lonely "+". */
internal fun isPhoneLike(label: String): Boolean {
    val trimmed = label.trim()
    if (trimmed.isEmpty()) return false
    if (trimmed.none(Char::isDigit)) return false
    return trimmed.all { char -> char.isDigit() || char in "+-() ." }
}

private object AvatarBitmapCache {
    private const val MAX_ENTRIES = 64

    private val cache = LruCache<String, ImageBitmap>(MAX_ENTRIES)

    fun get(key: String): ImageBitmap? = cache.get(key)

    fun put(key: String, bitmap: ImageBitmap) {
        cache.put(key, bitmap)
    }
}

private fun decodeScaledAvatar(context: Context, uri: Uri, targetPx: Int): ImageBitmap? = runCatching {
    // Read the bounds first so the real decode can subsample: contact photos are full
    // resolution and decoding them at 1:1 for a ~54dp circle wastes memory on every row.
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
    val options = BitmapFactory.Options().apply {
        inSampleSize = avatarSampleSize(bounds.outWidth, bounds.outHeight, targetPx)
    }
    context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, options)?.asImageBitmap()
    }
}.getOrNull()

internal fun avatarSampleSize(width: Int, height: Int, targetPx: Int): Int {
    if (targetPx <= 0) return 1
    var sample = 1
    while (minOf(width, height) / (sample * 2) >= targetPx) {
        sample *= 2
    }
    return sample
}
