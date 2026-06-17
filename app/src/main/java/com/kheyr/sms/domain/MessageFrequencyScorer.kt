package com.kheyr.sms.domain

object MessageFrequencyScorer {
    fun score(recentMessageCount: Int, windowMinutes: Int, threshold: Int = 5): Int = when {
        windowMinutes <= 0 -> 0
        recentMessageCount >= threshold -> 25
        recentMessageCount >= threshold / 2 -> 10
        else -> 0
    }
}
