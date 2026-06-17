package com.kheyr.sms.util

object OtpDetector {
    private val labeledCode = Regex("""(?i)(?:code|otp|pin|verification|verify|کد|رمز|رمز\s*یکبار|یکبار\s*مصرف)[:\s\-]*([0-9۰-۹]{4,8})""")
    private val standaloneCode = Regex("""\b([0-9۰-۹]{4,8})\b""")

    fun findCopyableCode(body: String): String? {
        labeledCode.find(body)?.groupValues?.getOrNull(1)?.let { return normalizeDigits(it) }
        val candidates = standaloneCode.findAll(body).map { normalizeDigits(it.groupValues[1]) }.toList()
        return candidates.singleOrNull()
    }

    private fun normalizeDigits(value: String): String = buildString(value.length) {
        value.forEach { char ->
            append(
                when (char) {
                    in '۰'..'۹' -> '0' + (char - '۰')
                    else -> char
                },
            )
        }
    }
}
