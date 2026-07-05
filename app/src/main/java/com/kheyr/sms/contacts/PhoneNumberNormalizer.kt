package com.kheyr.sms.contacts

object PhoneNumberNormalizer {
    /**
     * Alphanumeric sender IDs (e.g. "VERIFY", "Chase", "ICICIB") are used by banks/OTP services and
     * are not phone numbers. Digit-stripping them collapses every one to "" and makes unrelated
     * senders collide, so they are treated as opaque identifiers instead.
     */
    fun isAlphanumericSender(address: String): Boolean = address.any { it.isLetter() }

    fun normalize(phone: String): String {
        if (isAlphanumericSender(phone)) return phone.trim()
        return phone.filter { it.isDigit() || it == '+' }
    }

    fun matchKey(phone: String): String {
        if (isAlphanumericSender(phone)) return phone.trim().uppercase()
        val normalized = normalize(phone)
        return if (normalized.length >= 10) normalized.takeLast(10) else normalized
    }

    fun matches(first: String, second: String): Boolean {
        if (isAlphanumericSender(first) || isAlphanumericSender(second)) {
            // An alphanumeric sender ID only ever matches an identical (case-insensitive) sender ID;
            // it must never match a phone number or another, different sender ID.
            return isAlphanumericSender(first) && isAlphanumericSender(second) &&
                first.trim().equals(second.trim(), ignoreCase = true)
        }
        val a = normalize(first)
        val b = normalize(second)
        // Two blank/undialable addresses are not "the same sender" — never collapse them together.
        if (a.isEmpty() || b.isEmpty()) return false
        if (a == b) return true
        if (a.length >= 10 && b.length >= 10 && a.takeLast(10) == b.takeLast(10)) return true
        return false
    }
}
