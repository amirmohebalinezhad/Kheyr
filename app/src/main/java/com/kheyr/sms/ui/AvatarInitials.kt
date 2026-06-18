package com.kheyr.sms.ui

import java.text.BreakIterator

object AvatarInitials {
    fun from(displayName: String): String {
        val trimmed = displayName.trim()
        if (trimmed.isEmpty()) return "?"
        val iterator = BreakIterator.getCharacterInstance()
        iterator.setText(trimmed)
        val start = iterator.first()
        val end = iterator.next()
        if (end == BreakIterator.DONE) return trimmed.take(1)
        val firstGrapheme = trimmed.substring(start, end)
        return if (firstGrapheme.length == 1 && firstGrapheme[0].isLetter()) {
            firstGrapheme.uppercase()
        } else {
            firstGrapheme
        }
    }
}
