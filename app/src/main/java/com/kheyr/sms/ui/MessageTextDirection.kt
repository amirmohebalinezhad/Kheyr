package com.kheyr.sms.ui

import androidx.compose.ui.text.style.TextDirection

object MessageTextDirection {
    fun resolve(text: String): TextDirection {
        for (char in text) {
            when (Character.getDirectionality(char)) {
                Character.DIRECTIONALITY_RIGHT_TO_LEFT,
                Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC,
                -> return TextDirection.Rtl

                Character.DIRECTIONALITY_LEFT_TO_RIGHT -> return TextDirection.Ltr
            }
        }
        return TextDirection.Content
    }
}
