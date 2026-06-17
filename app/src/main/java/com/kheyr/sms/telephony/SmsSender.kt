package com.kheyr.sms.telephony

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager

class SmsSender(private val context: Context) {
    fun send(request: SmsSendRequest): SmsSendResult {
        require(request.recipient.isNotBlank()) { "recipient is required" }
        require(request.body.isNotBlank()) { "message body is required" }

        val smsManager = request.subscriptionId?.let { subscriptionId ->
            @Suppress("DEPRECATION")
            SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
        } ?: run {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

        val parts = smsManager.divideMessage(request.body)
        val sentIntents = parts.indices.map { index ->
            PendingIntent.getBroadcast(
                context,
                index,
                Intent(ACTION_SMS_SENT).putExtra(EXTRA_PART_INDEX, index),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        smsManager.sendMultipartTextMessage(request.recipient, null, parts, ArrayList(sentIntents), null)
        return SmsSendResult(parts = parts.size, subscriptionId = request.subscriptionId)
    }

    companion object {
        const val ACTION_SMS_SENT = "com.kheyr.sms.action.SMS_SENT"
        const val EXTRA_PART_INDEX = "part_index"
    }
}
