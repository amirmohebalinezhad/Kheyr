package com.kheyr.sms.util

object OtpDetector {
    private val labeledCode = Regex("""(?i)(?:code|otp|pin|verification|verify|کد|رمز|رمز\s*یکبار|یکبار\s*مصرف)[:\s\-]*([0-9۰-۹٠-٩]{4,8})""")
    // Java/Kotlin \b word boundaries are ASCII-only and do not form a boundary around non-ASCII digits
    // (e.g. Arabic-Indic ١٢٣٤), so use explicit lookarounds. They exclude adjacent letters (\p{L}) and
    // digits of any script, keeping the original intent of matching only standalone codes.
    private val standaloneCode = Regex("""(?<![\p{L}0-9۰-۹٠-٩])([0-9۰-۹٠-٩]{4,8})(?![\p{L}0-9۰-۹٠-٩])""")

    fun findCopyableCode(body: String): String? {
        labeledCode.find(body)?.groupValues?.getOrNull(1)?.let { return DigitNormalizer.toAsciiDigits(it) }
        val candidates = standaloneCode.findAll(body).map { DigitNormalizer.toAsciiDigits(it.groupValues[1]) }.toList()
        return candidates.singleOrNull()
    }
}
