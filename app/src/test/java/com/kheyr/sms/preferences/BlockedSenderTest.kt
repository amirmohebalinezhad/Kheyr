package com.kheyr.sms.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * B-19 blocking is only useful if the address the user blocks in the UI matches the address the
 * carrier puts in the PDU. Those are formatted differently far more often than not, so the block
 * list is keyed by match key rather than by the raw string.
 */
@RunWith(RobolectricTestRunner::class)
class BlockedSenderTest {
    private val preferences = AppPreferences(ApplicationProvider.getApplicationContext<Context>())

    @Test fun blockingAFormattedNumberBlocksTheCarriersFormOfIt() {
        preferences.setBlockedSender("(555) 123-4567", blocked = true)

        assertTrue(preferences.isBlockedSender("+15551234567"))
        assertTrue(preferences.isBlockedSender("555-123-4567"))
    }

    @Test fun unblockingUsesTheSameKeySoItActuallyUnblocks() {
        preferences.setBlockedSender("+15551234567", blocked = true)
        preferences.setBlockedSender("(555) 123-4567", blocked = false)

        assertFalse(preferences.isBlockedSender("+15551234567"))
    }

    @Test fun distinctAlphanumericSendersDoNotBlockEachOther() {
        preferences.setBlockedSender("VERIFY", blocked = true)

        assertTrue(preferences.isBlockedSender("verify"))
        assertFalse(preferences.isBlockedSender("CHASE"))
        assertFalse(preferences.isBlockedSender("+15551234567"))
    }
}
