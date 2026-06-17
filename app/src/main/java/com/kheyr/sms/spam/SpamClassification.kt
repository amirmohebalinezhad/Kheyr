package com.kheyr.sms.spam

enum class SpamClassification { Normal, Suspicious, Spam }
object SpamClassificationThresholds {
    fun classify(score: Int): SpamClassification = when {
        score >= 70 -> SpamClassification.Spam
        score >= 40 -> SpamClassification.Suspicious
        else -> SpamClassification.Normal
    }
}
