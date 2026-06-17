package com.kheyr.sms.spam

data class SpamRulesDownloadState(val latestVersion: Int?, val cachedVersion: Int?, val downloading: Boolean, val lastError: String? = null) {
    val canClassify: Boolean get() = latestVersion != null || cachedVersion != null
    val visibleVersion: Int? get() = latestVersion ?: cachedVersion
}
