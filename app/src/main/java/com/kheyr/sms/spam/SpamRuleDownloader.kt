package com.kheyr.sms.spam

import com.kheyr.sms.domain.SpamRuleSet

data class SpamRuleDownloadResult(val accepted: Boolean, val ruleSet: SpamRuleSet?, val error: String? = null)

class SpamRuleDownloader {
    fun validate(ruleSet: SpamRuleSet?): SpamRuleDownloadResult =
        if (ruleSet != null && ruleSet.version > 0) SpamRuleDownloadResult(true, ruleSet) else SpamRuleDownloadResult(false, null, "invalid_rules")
}
