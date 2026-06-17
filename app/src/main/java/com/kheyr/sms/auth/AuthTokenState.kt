package com.kheyr.sms.auth

data class AuthTokenState(
    val accessToken: String?,
    val refreshToken: String?,
    val expiresAtMillis: Long,
) {
    val isExpired: Boolean get() = System.currentTimeMillis() >= expiresAtMillis
    val canRefresh: Boolean get() = !refreshToken.isNullOrBlank()
}
