package com.kheyr.sms.auth

data class OtpVerificationState(
    val code: String = "",
    val resendSecondsRemaining: Int = 0,
    val verified: Boolean = false,
    val error: String? = null,
) {
    val canSubmit: Boolean get() = code.length >= 4 && error == null
}
