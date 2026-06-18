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
            statusIntent(ACTION_SMS_SENT, request, index, parts.size)
        }
        smsManager.sendMultipartTextMessage(
            request.recipient,
            null,
            parts,
            ArrayList(sentIntents),
            null,
        )
        return SmsSendResult(parts = parts.size, subscriptionId = request.subscriptionId)
    }

    private fun statusIntent(action: String, request: SmsSendRequest, partIndex: Int, partCount: Int): PendingIntent {
        val requestCode = ((request.messageId ?: 0L) % Int.MAX_VALUE).toInt() * 31 + partIndex + action.hashCode()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(action)
                .setPackage(context.packageName)
                .putExtra(EXTRA_MESSAGE_ID, request.messageId)
                .putExtra(EXTRA_RECIPIENT, request.recipient)
                .putExtra(EXTRA_BODY, request.body)
                .putExtra(EXTRA_SUBSCRIPTION_ID, request.subscriptionId)
                .putExtra(EXTRA_PART_INDEX, partIndex)
                .putExtra(EXTRA_PART_COUNT, partCount),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_SMS_SENT = "com.kheyr.sms.action.SMS_SENT"
        const val ACTION_SMS_DELIVERED = "com.kheyr.sms.action.SMS_DELIVERED"
        const val EXTRA_MESSAGE_ID = "message_id"
        const val EXTRA_RECIPIENT = "recipient"
        const val EXTRA_BODY = "body"
        const val EXTRA_SUBSCRIPTION_ID = "subscription_id"
        const val EXTRA_PART_INDEX = "part_index"
        const val EXTRA_PART_COUNT = "part_count"
    }
}
