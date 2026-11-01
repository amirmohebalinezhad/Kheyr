package com.kheyr.sms.ui

import androidx.compose.ui.graphics.Color

/**
 * Stable, human-friendly avatar tints. The same key always maps to the same color, so a sender's
 * avatar never changes between screens or launches.
 */
object AvatarColor {
    private val palette = listOf(
        Color(0xFFC25E4F),
        Color(0xFF2F7D6C),
        Color(0xFF6A5ACD),
        Color(0xFFB4772B),
        Color(0xFF3577AE),
        Color(0xFFB04F70),
        Color(0xFF4F7D44),
        Color(0xFF247A8C),
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
