package com.kheyr.sms.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HighlightRangeTest {
    @Test
    fun findsAsciiMatch() {
        assertEquals(listOf(6 until 11), highlightRanges("hello world", "world"))
    }

    @Test
    fun matchesIgnoringCase() {
        assertEquals(listOf(0 until 5), highlightRanges("Hello world", "hELLo"))
    }

    @Test
    fun findsEveryOccurrence() {
        assertEquals(listOf(0 until 2, 4 until 6, 8 until 10), highlightRanges("ab-.AB-.aB", "ab"))
    }

    @Test
    fun keepsIndicesInsideOriginalTextForTurkishDottedCapital() {
        // "İ" lowercases to two code units, which used to push the ranges past text.length.
        val text = "İstanbul kart"
        val ranges = highlightRanges(text, "kart")
        assertTrue(ranges.isNotEmpty())
        ranges.forEach { range ->
            assertTrue(range.first >= 0 && range.last < text.length)
            assertEquals("kart", text.substring(range.first, range.last + 1))
        }
    }

    @Test
    fun returnsEmptyForBlankOrMissingHighlight() {
        assertEquals(emptyList<IntRange>(), highlightRanges("hello", null))
        assertEquals(emptyList<IntRange>(), highlightRanges("hello", ""))
        assertEquals(emptyList<IntRange>(), highlightRanges("hello", "   "))
        assertEquals(emptyList<IntRange>(), highlightRanges("hello", "zz"))
    }
}
