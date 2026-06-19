package com.kheyr.sms.ui

import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageTextDirectionTest {
    @Test
    fun resolvesRtlForPersianText() {
        assertEquals(TextDirection.Rtl, MessageTextDirection.resolve("سلام"))
        assertEquals(LayoutDirection.Rtl, MessageTextDirection.resolveLayoutDirection("سلام", LayoutDirection.Ltr))
    }

    @Test
    fun resolvesRtlForArabicText() {
        assertEquals(TextDirection.Rtl, MessageTextDirection.resolve("مرحبا"))
        assertEquals(LayoutDirection.Rtl, MessageTextDirection.resolveLayoutDirection("مرحبا", LayoutDirection.Ltr))
    }

    @Test
    fun resolvesLtrForEnglishText() {
        assertEquals(TextDirection.Ltr, MessageTextDirection.resolve("Hello"))
        assertEquals(LayoutDirection.Ltr, MessageTextDirection.resolveLayoutDirection("Hello", LayoutDirection.Rtl))
    }

    @Test
    fun fallsBackForNumbersOnlyText() {
        assertEquals(TextDirection.Content, MessageTextDirection.resolve("12345"))
        assertEquals(LayoutDirection.Rtl, MessageTextDirection.resolveLayoutDirection("12345", LayoutDirection.Rtl))
    }

    @Test
    fun fallsBackForPersianDigitsOnlyText() {
        assertEquals(TextDirection.Content, MessageTextDirection.resolve("۱۲۳۴۵"))
        assertEquals(LayoutDirection.Ltr, MessageTextDirection.resolveLayoutDirection("۱۲۳۴۵", LayoutDirection.Ltr))
    }

    @Test
    fun resolvesLtrForEnglishPrefixedWithArabicPunctuation() {
        assertEquals(TextDirection.Ltr, MessageTextDirection.resolve("، Hello"))
        assertEquals(LayoutDirection.Ltr, MessageTextDirection.resolveLayoutDirection("، Hello", LayoutDirection.Rtl))
    }

    @Test
    fun fallsBackForEmojiOnlyText() {
        assertEquals(TextDirection.Content, MessageTextDirection.resolve("🙂🎉"))
        assertEquals(LayoutDirection.Ltr, MessageTextDirection.resolveLayoutDirection("🙂🎉", LayoutDirection.Ltr))
    }

    @Test
    fun resolvesDirectionFromFirstStrongCharacterInMixedText() {
        assertEquals(TextDirection.Rtl, MessageTextDirection.resolve("123 سلام Hello"))
        assertEquals(LayoutDirection.Rtl, MessageTextDirection.resolveLayoutDirection("123 سلام Hello", LayoutDirection.Ltr))
        assertEquals(TextDirection.Ltr, MessageTextDirection.resolve("... Hello سلام"))
        assertEquals(LayoutDirection.Ltr, MessageTextDirection.resolveLayoutDirection("... Hello سلام", LayoutDirection.Rtl))
    }
}
