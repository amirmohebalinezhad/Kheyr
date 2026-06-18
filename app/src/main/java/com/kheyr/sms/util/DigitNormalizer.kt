package com.kheyr.sms.util

object DigitNormalizer {
    fun toAsciiDigits(value: String): String = buildString(value.length) {
        value.forEach { char ->
            append(
                when (char) {
                    in '۰'..'۹' -> '0' + (char - '۰')
                    in '٠'..'٩' -> '0' + (char - '٠')
                    else -> char
                },
            )
        }
    }

    fun digitsOnly(value: String): String = toAsciiDigits(value).filter { it.isDigit() }
}
