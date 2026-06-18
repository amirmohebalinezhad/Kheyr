package com.kheyr.sms.data

import android.provider.Telephony

object TelephonyDirectionMapper {
    fun directionFromType(messageType: Int): MessageDirection =
        if (messageType == Telephony.Sms.MESSAGE_TYPE_INBOX) {
            MessageDirection.Incoming
        } else {
            MessageDirection.Outgoing
        }
}
