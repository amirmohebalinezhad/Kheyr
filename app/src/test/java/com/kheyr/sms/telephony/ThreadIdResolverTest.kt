package com.kheyr.sms.telephony

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadIdResolverTest {
    @Test
    fun syntheticThreadIdIsStableForSameSender() {
        // The receive-path fallback relies on this: repeated messages from one sender must group.
        assertEquals(
            ThreadIdResolver.syntheticThreadId("VERIFY"),
            ThreadIdResolver.syntheticThreadId("verify "),
        )
    }

    @Test
    fun syntheticThreadIdIsPositiveSoNotificationDeepLinksWork() {
        // MainActivity treats a negative thread-id extra as "no thread"; synthetic ids must stay positive.
        assertTrue(ThreadIdResolver.syntheticThreadId("VERIFY") > 0)
        assertTrue(ThreadIdResolver.syntheticThreadId("A") >= ThreadIdResolver.SYNTHETIC_THREAD_ID_BASE)
    }

    @Test
    fun distinctSendersGetDistinctThreads() {
        assertNotEquals(
            ThreadIdResolver.syntheticThreadId("CHASE"),
            ThreadIdResolver.syntheticThreadId("VERIFY"),
        )
    }
}
