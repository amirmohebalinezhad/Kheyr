package com.kheyr.sms.util

enum class CopyableSegmentKind {
    Phone,
    Iban,
    CardNumber,
}

data class CopyableSegment(
    val kind: CopyableSegmentKind,
    val raw: String,
    val normalized: String,
    val start: Int,
    val end: Int,
)

object MessageCopyableSegmentDetector {
    private val digit = """[0-9۰-۹٠-٩]"""
    private val ibanPattern = Regex("""(?i)([A-Z]{2}[\s\-]?$digit{2}(?:[\s\-]*[0-9A-Z۰-۹٠-٩]){11,28})""")
    private val cardPattern = Regex("""(?:($digit{4}[\s\-]?){3}$digit{4,7}|$digit{13,19})""")
    private val phonePattern = Regex("""(\+?$digit[\s\-().]*$digit(?:[\s\-().]*$digit){5,14})""")

    fun findAll(text: String): List<CopyableSegment> {
        val segments = mutableListOf<CopyableSegment>()
        ibanPattern.findAll(text).forEach { match ->
            val raw = match.value
            if (isIban(raw) && !overlaps(segments, match.range)) {
                segments += segment(CopyableSegmentKind.Iban, raw, match.range.first, match.range.last + 1)
            }
        }
        cardPattern.findAll(text).forEach { match ->
            val raw = match.value
            if (isCardNumber(raw) && !overlaps(segments, match.range)) {
                segments += segment(CopyableSegmentKind.CardNumber, raw, match.range.first, match.range.last + 1)
            }
        }
        phonePattern.findAll(text).forEach { match ->
            val raw = match.value.trim()
            if (isPhoneNumber(raw) && !overlaps(segments, match.range)) {
                segments += segment(CopyableSegmentKind.Phone, raw, match.range.first, match.range.last + 1)
            }
        }
        return segments.sortedBy { it.start }
    }

    private fun segment(kind: CopyableSegmentKind, raw: String, start: Int, end: Int): CopyableSegment =
        CopyableSegment(
            kind = kind,
            raw = raw,
            normalized = DigitNormalizer.toAsciiDigits(raw),
            start = start,
            end = end,
        )

    private fun overlaps(segments: List<CopyableSegment>, range: IntRange): Boolean =
        segments.any { range.first < it.end && range.last + 1 > it.start }

    private fun isIban(raw: String): Boolean {
        val compact = DigitNormalizer.toAsciiDigits(raw).replace(Regex("[\\s\\-]"), "").uppercase()
        if (compact.length !in 15..34) return false
        if (!compact.take(2).all { it.isLetter() }) return false
        if (!compact.substring(2, 4).all { it.isDigit() }) return false
        return compact.substring(4).all { it.isLetterOrDigit() }
    }

    private fun isCardNumber(raw: String): Boolean {
        val digits = DigitNormalizer.digitsOnly(raw)
        return digits.length in 13..19
    }

    private fun isPhoneNumber(raw: String): Boolean {
        val digits = DigitNormalizer.digitsOnly(raw)
        if (digits.length !in 7..15) return false
        val normalized = DigitNormalizer.toAsciiDigits(raw)
        if (normalized.startsWith("+") || normalized.startsWith("00") || normalized.startsWith("0")) return true
        if (digits.length in 13..19) return false
        return normalized.any { it.isWhitespace() || it == '-' || it == '.' || it == '(' || it == ')' }
    }
}
