package com.kheyr.sms.ui

import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection

object MessageTextDirection {
    fun resolve(text: String): TextDirection = when (resolveFirstStrongDirection(text)) {
        StrongTextDirection.Rtl -> TextDirection.Rtl
        StrongTextDirection.Ltr -> TextDirection.Ltr
        null -> TextDirection.Content
    }

    fun resolveLayoutDirection(text: String, fallback: LayoutDirection): LayoutDirection =
        when (resolveFirstStrongDirection(text)) {
            StrongTextDirection.Rtl -> LayoutDirection.Rtl
            StrongTextDirection.Ltr -> LayoutDirection.Ltr
            null -> fallback
        }

    private fun resolveFirstStrongDirection(text: String): StrongTextDirection? {
        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            when {
                codePoint.isRtlScriptCodePoint() -> return StrongTextDirection.Rtl
                codePoint.isRtlDirectionality() -> return StrongTextDirection.Rtl
                codePoint.isLtrDirectionality() -> return StrongTextDirection.Ltr
            }
            index += Character.charCount(codePoint)
        }
        return null
    }

    private fun Int.isRtlScriptCodePoint(): Boolean =
        this in 0x0590..0x05FF || // Hebrew
            this in 0x0600..0x06FF || // Arabic, Persian, Urdu
            this in 0x0750..0x077F || // Arabic Supplement
            this in 0x08A0..0x08FF || // Arabic Extended-A
            this in 0xFB1D..0xFDFF || // Hebrew/Arabic presentation forms
            this in 0xFE70..0xFEFF // Arabic Presentation Forms-B

    private fun Int.isRtlDirectionality(): Boolean =
        Character.getDirectionality(this) == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
            Character.getDirectionality(this) == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC

    private fun Int.isLtrDirectionality(): Boolean =
        Character.getDirectionality(this) == Character.DIRECTIONALITY_LEFT_TO_RIGHT

    private enum class StrongTextDirection { Ltr, Rtl }
}
