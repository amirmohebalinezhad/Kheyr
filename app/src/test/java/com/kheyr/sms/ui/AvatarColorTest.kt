package com.kheyr.sms.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AvatarColorTest {
    @Test
    fun sameKeyProducesSameColor() {
        assertEquals(AvatarColor.forKey("Alice"), AvatarColor.forKey("Alice"))
    }

    @Test
    fun differentKeysCanProduceDifferentColors() {
        val colors = (0 until 20).map { AvatarColor.forKey("user$it") }.toSet()
        assertNotEquals(1, colors.size)
    }

    @Test
    fun emojiNameUsesStableColor() {
        assertEquals(AvatarColor.forKey("😀 Team"), AvatarColor.forKey("😀 Team"))
    }
}
