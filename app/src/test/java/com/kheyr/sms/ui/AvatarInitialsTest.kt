package com.kheyr.sms.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AvatarInitialsTest {
    @Test
    fun latinNameUsesFirstLetter() {
        assertEquals("A", AvatarInitials.from("Alice"))
    }

    @Test
    fun emojiNameUsesEmojiGrapheme() {
        assertEquals("😀", AvatarInitials.from("😀 Alice"))
    }

    @Test
    fun persianNameUsesFirstGrapheme() {
        assertEquals("ع", AvatarInitials.from("علی"))
    }
}
