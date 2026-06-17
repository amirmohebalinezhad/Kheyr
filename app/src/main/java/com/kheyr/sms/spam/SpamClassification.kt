package com.kheyr.sms.spam

enum class SpamClassification { Normal, Suspicious, Spam }
object SpamClassificationThresholds {
    fun classify(score: Int, threshold: Int = 70): SpamClassification = when {
        score >= threshold -> SpamClassification.Spam
        score >= 40 -> SpamClassification.Suspicious
        else -> SpamClassification.Normal
    }
}
