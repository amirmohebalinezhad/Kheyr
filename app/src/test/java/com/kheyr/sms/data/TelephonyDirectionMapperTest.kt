package com.kheyr.sms.data

import android.provider.Telephony
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    // Because a draft maps to Outgoing above (and to status Received), it must never reach the
    // importer in the first place: it would render as a sent bubble (B-29).
    @Test
    fun draftsAndTheAllBucketAreNotImportable() {
        assertFalse(TelephonyDirectionMapper.isImportableType(Telephony.Sms.MESSAGE_TYPE_DRAFT))
        assertFalse(TelephonyDirectionMapper.isImportableType(Telephony.Sms.MESSAGE_TYPE_ALL))
    }

    @Test
    fun realMessageTypesAreImportable() {
        assertTrue(TelephonyDirectionMapper.isImportableType(Telephony.Sms.MESSAGE_TYPE_INBOX))
        assertTrue(TelephonyDirectionMapper.isImportableType(Telephony.Sms.MESSAGE_TYPE_SENT))
        assertTrue(TelephonyDirectionMapper.isImportableType(Telephony.Sms.MESSAGE_TYPE_OUTBOX))
        assertTrue(TelephonyDirectionMapper.isImportableType(Telephony.Sms.MESSAGE_TYPE_FAILED))
        assertTrue(TelephonyDirectionMapper.isImportableType(Telephony.Sms.MESSAGE_TYPE_QUEUED))
    }

    @Test
    fun importSelectionExcludesDraftAndAllTypes() {
        assertEquals(
            "${Telephony.Sms.TYPE} NOT IN (${Telephony.Sms.MESSAGE_TYPE_DRAFT},${Telephony.Sms.MESSAGE_TYPE_ALL})",
            TelephonyDirectionMapper.IMPORTABLE_TYPE_SELECTION,
        )
    }
}
