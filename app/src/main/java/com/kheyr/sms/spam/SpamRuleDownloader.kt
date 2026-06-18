package com.kheyr.sms.spam

import com.kheyr.sms.domain.SpamRuleSet
import com.kheyr.sms.domain.SpamRuleUpdateDecision
import com.kheyr.sms.domain.SpamRuleUpdatePolicy

data class SpamRuleDownloadResult(val accepted: Boolean, val ruleSet: SpamRuleSet?, val error: String? = null)

class SpamRuleDownloader(private val policy: SpamRuleUpdatePolicy = SpamRuleUpdatePolicy()) {
    fun validate(current: SpamRuleSet?, candidate: SpamRuleSet?): SpamRuleDownloadResult {
        if (candidate == null) return SpamRuleDownloadResult(false, null, "no_rules")
        return when (val decision = policy.evaluate(current, candidate)) {
            SpamRuleUpdateDecision.Accept -> SpamRuleDownloadResult(true, candidate)
            SpamRuleUpdateDecision.RejectOlderOrSameVersion -> SpamRuleDownloadResult(false, null, "older_or_same_version")
            is SpamRuleUpdateDecision.RejectInvalid -> SpamRuleDownloadResult(false, null, decision.reason)
        }
    }
}
