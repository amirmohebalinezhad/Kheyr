package com.kheyr.sms.data

import android.provider.Telephony
import org.junit.Assert.assertEquals
import org.junit.Test

class TelephonyDirectionMapperTest {
    @Test
    fun inboxIsIncoming() {
        assertEquals(MessageDirection.Incoming, TelephonyDirectionMapper.directionFromType(Telephony.Sms.MESSAGE_TYPE_INBOX))
    }

    @Test
    fun sentIsOutgoing() {
        assertEquals(MessageDirection.Outgoing, TelephonyDirectionMapper.directionFromType(Telephony.Sms.MESSAGE_TYPE_SENT))
    }

    @Test
    fun outboxIsOutgoing() {
        assertEquals(MessageDirection.Outgoing, TelephonyDirectionMapper.directionFromType(Telephony.Sms.MESSAGE_TYPE_OUTBOX))
    }

    @Test
    fun failedIsOutgoing() {
        assertEquals(MessageDirection.Outgoing, TelephonyDirectionMapper.directionFromType(Telephony.Sms.MESSAGE_TYPE_FAILED))
    }

    @Test
    fun queuedIsOutgoing() {
        assertEquals(MessageDirection.Outgoing, TelephonyDirectionMapper.directionFromType(Telephony.Sms.MESSAGE_TYPE_QUEUED))
    }

    @Test
    fun draftIsOutgoing() {
        assertEquals(MessageDirection.Outgoing, TelephonyDirectionMapper.directionFromType(Telephony.Sms.MESSAGE_TYPE_DRAFT))
    }
}
