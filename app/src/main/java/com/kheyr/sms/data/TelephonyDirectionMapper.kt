package com.kheyr.sms.data

import android.provider.Telephony

object TelephonyDirectionMapper {
    /**
     * Row types the importer must skip. A draft left behind by the previous SMS app is not a message
     * the user sent, but [directionFromType] maps every non-inbox type to outgoing and the status
     * mapper falls through to Received, so an imported draft renders as a sent bubble (B-29).
     * `MESSAGE_TYPE_ALL` is a query bucket rather than a real row type and is excluded for the same
     * reason. Drafts are excluded at the provider query instead of being rendered.
     */
    private val SKIPPED_IMPORT_TYPES = intArrayOf(Telephony.Sms.MESSAGE_TYPE_DRAFT, Telephony.Sms.MESSAGE_TYPE_ALL)

    /** SQL fragment for the exclusion above; safe to inline because both values are integer constants. */
    val IMPORTABLE_TYPE_SELECTION: String =
        "${Telephony.Sms.TYPE} NOT IN (${SKIPPED_IMPORT_TYPES.joinToString(",")})"

    fun isImportableType(messageType: Int): Boolean = messageType !in SKIPPED_IMPORT_TYPES

    fun directionFromType(messageType: Int): MessageDirection =
        if (messageType == Telephony.Sms.MESSAGE_TYPE_INBOX) {
            MessageDirection.Incoming
        } else {
            MessageDirection.Outgoing
        }
}
