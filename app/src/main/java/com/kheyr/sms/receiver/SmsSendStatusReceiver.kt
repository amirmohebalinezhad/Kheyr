package com.kheyr.sms.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kheyr.sms.data.SmsRepository
import com.kheyr.sms.telephony.SmsSender

class SmsSendStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getLongExtra(SmsSender.EXTRA_MESSAGE_ID, -1L).takeIf { it > 0L } ?: return
        val repository = SmsRepository(context)
        when (intent.action) {
            SmsSender.ACTION_SMS_SENT -> {
                if (resultCode == Activity.RESULT_OK) {
                    repository.markSent(messageId)
                } else {
                    repository.markFailed(messageId)
                }
            }
            SmsSender.ACTION_SMS_DELIVERED -> {
                if (resultCode == Activity.RESULT_OK) {
                    repository.markDelivered(messageId)
                }
            }
        }
    }
}
