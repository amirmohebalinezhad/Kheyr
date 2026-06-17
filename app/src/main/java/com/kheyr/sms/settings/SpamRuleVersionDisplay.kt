package com.kheyr.sms.settings

object SpamRuleVersionDisplay {
    fun label(version: Int?): String = version?.let { "Spam rules v$it" } ?: "Spam rules not downloaded"
}
