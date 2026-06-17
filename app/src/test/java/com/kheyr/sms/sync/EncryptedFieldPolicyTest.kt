package com.kheyr.sms.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class EncryptedFieldPolicyTest {
    @Test fun addressFieldUsesEncryptedProtection() {
        assertEquals(SyncFieldProtection.Encrypted, EncryptedFieldPolicy.protectionFor("address"))
    }
}
