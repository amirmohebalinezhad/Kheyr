package com.kheyr.sms.spam

data class SpamReason(val ruleId: String, val label: String, val score: Int)
object SpamReasonFormatter {
    fun format(reasons: List<SpamReason>): String = if (reasons.isEmpty()) "No spam rules matched." else reasons.joinToString("; ") { "${it.label} (${it.score})" }
}
