package com.kheyr.sms.util

/**
 * Folds a message body onto the orthography the published spam rules are written against.
 *
 * Iranian SMS arrives with the Arabic yeh/kaf as often as the Persian ones, with ZWNJ inside
 * compounds the rules spell with an optional space, and with Persian or Arabic-Indic digits. The
 * rule pack was tuned against normalized text, so matching raw PDU text loses roughly 9% of the
 * detections the pack is measured to make.
 *
 * Deliberately narrow: only the four foldings the rules depend on. Anything wider (alef variants,
 * tatweel, diacritics) would change what the measured patterns match and invalidate their numbers.
 *
 * Never apply this to a sender string - sender matching is keyed by
 * [com.kheyr.sms.contacts.PhoneNumberNormalizer.matchKey], which has its own normalization.
 */
object PersianTextNormalizer {
    private const val ARABIC_YEH = 'ي'
    private const val FARSI_YEH = 'ی'
    private const val ARABIC_KAF = 'ك'
    private const val KEHEH = 'ک'
    private const val ZERO_WIDTH_NON_JOINER = '\u200C'

    /**
     * Idempotent, and length-preserving except for the ZWNJ it drops. Callers must run it once per
     * message before any rule is evaluated, never once per rule.
     */
    fun normalize(text: String): String {
        if (text.isEmpty()) return text
        val folded = buildString(text.length) {
            text.forEach { char ->
                when (char) {
                    ARABIC_YEH -> append(FARSI_YEH)
                    ARABIC_KAF -> append(KEHEH)
                    ZERO_WIDTH_NON_JOINER -> Unit
                    else -> append(char)
                }
            }
        }
        return DigitNormalizer.toAsciiDigits(folded)
    }
}
