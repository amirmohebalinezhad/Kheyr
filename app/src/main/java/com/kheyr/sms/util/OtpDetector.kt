package com.kheyr.sms.util

object OtpDetector {
    private val labeledCode = Regex("""(?i)(?:code|otp|pin|verification|verify|کد|رمز|رمز\s*یکبار|یکبار\s*مصرف)[:\s\-]*([0-9۰-۹٠-٩]{4,8})""")
    private val standaloneCode = Regex("""\b([0-9۰-۹٠-٩]{4,8})\b""")

    fun findCopyableCode(body: String): String? {
        labeledCode.find(body)?.groupValues?.getOrNull(1)?.let { return DigitNormalizer.toAsciiDigits(it) }
        val candidates = standaloneCode.findAll(body).map { DigitNormalizer.toAsciiDigits(it.groupValues[1]) }.toList()
        return candidates.singleOrNull()
    }
}
