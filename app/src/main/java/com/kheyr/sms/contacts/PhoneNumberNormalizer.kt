package com.kheyr.sms.contacts

object PhoneNumberNormalizer {
    fun normalize(phone: String): String = phone.filter { it.isDigit() || it == '+' }

    fun matches(first: String, second: String): Boolean {
        val a = normalize(first)
        val b = normalize(second)
        if (a == b) return true
        if (a.length >= 10 && b.length >= 10 && a.takeLast(10) == b.takeLast(10)) return true
        return false
    }
}
