package com.kheyr.sms.api

import com.kheyr.sms.BuildConfig

/** Backend base URL — replace `YOUR-BASE-URL` in build.gradle or override at build time. */
object ApiConfig {
    const val BASE_URL_PLACEHOLDER = "https://YOUR-BASE-URL.example.com"

    val baseUrl: String
        get() {
            val configured = BuildConfig.API_BASE_URL.trim().trimEnd('/')
            return if (configured.isBlank() || configured.contains("YOUR-BASE-URL")) BASE_URL_PLACEHOLDER else configured
        }

    fun endpoint(path: String): String = baseUrl + path

    val isConfigured: Boolean get() = !baseUrl.contains("YOUR-BASE-URL")
}
