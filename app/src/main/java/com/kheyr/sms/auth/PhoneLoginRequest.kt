package com.kheyr.sms.auth

data class PhoneLoginRequest(val rawNumber: String) {
    val normalized: String get() = rawNumber.filter { it.isDigit() || it == '+' }.let { if (it.startsWith("+")) it else "+$it" }
    val isValid: Boolean get() = normalized.length >= 8
}
