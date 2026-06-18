package com.kheyr.sms.ui

import androidx.compose.ui.graphics.Color

object AvatarColor {
    private val palette = listOf(
        Color(0xFFE17076),
        Color(0xFF7BC862),
        Color(0xFFE5CA77),
        Color(0xFF65AADD),
        Color(0xFF8E7AE6),
        Color(0xFFE0854A),
        Color(0xFF77CCC1),
    )

    fun forKey(key: String): Color {
        val normalized = key.trim()
        if (normalized.isEmpty()) return palette[0]
        var hash = 0L
        normalized.codePoints().forEach { codePoint ->
            hash = (hash * 31 + codePoint) and 0xFFFFFFFFL
        }
        return palette[(hash % palette.size).toInt()]
    }
}
