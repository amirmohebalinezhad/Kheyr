package com.kheyr.sms.ui

import androidx.compose.ui.text.style.TextDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageTextDirectionTest {
    @Test
    fun resolvesRtlForPersianText() {
        assertEquals(TextDirection.Rtl, MessageTextDirection.resolve("سلام"))
    }

    @Test
    fun resolvesLtrForEnglishText() {
        assertEquals(TextDirection.Ltr, MessageTextDirection.resolve("Hello"))
    }

    @Test
    fun fallsBackToContentForNeutralText() {
        assertEquals(TextDirection.Content, MessageTextDirection.resolve("12345"))
    }
}
