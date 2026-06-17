package com.kheyr.sms.spam

import org.junit.Assert.assertEquals
import org.junit.Test

class SpamClassificationThresholdsTest {
    @Test fun honorsConfiguredThreshold() {
        assertEquals(SpamClassification.Suspicious, SpamClassificationThresholds.classify(score = 80, threshold = 90))
        assertEquals(SpamClassification.Spam, SpamClassificationThresholds.classify(score = 95, threshold = 90))
    }
}
