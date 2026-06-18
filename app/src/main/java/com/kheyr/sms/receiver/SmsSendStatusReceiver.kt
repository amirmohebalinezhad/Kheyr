package com.kheyr.sms.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kheyr.sms.data.SmsRefreshEvents
import com.kheyr.sms.data.SmsRepository
import com.kheyr.sms.telephony.SmsSender

class SmsSendStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getLongExtra(SmsSender.EXTRA_MESSAGE_ID, -1L).takeIf { it > 0L } ?: return
        val partIndex = intent.getIntExtra(SmsSender.EXTRA_PART_INDEX, 0)
        val partCount = intent.getIntExtra(SmsSender.EXTRA_PART_COUNT, 1).coerceAtLeast(1)
        val pendingResult = goAsync()
        Thread {
            try {
                val repository = SmsRepository(context)
                when (intent.action) {
                    SmsSender.ACTION_SMS_SENT -> {
                        if (resultCode == Activity.RESULT_OK) {
                            if (recordSuccessfulPart(context, "sent", messageId, partIndex, partCount)) {
                                repository.markSent(messageId)
                                repository.notifyRefreshForTelephonyId(messageId)
                            }
                        } else {
                            clearPartProgress(context, "sent", messageId)
                            clearPartProgress(context, "delivered", messageId)
                            repository.markFailed(messageId)
                            repository.notifyRefreshForTelephonyId(messageId)
                        }
                    }
                    SmsSender.ACTION_SMS_DELIVERED -> {
                        if (resultCode == Activity.RESULT_OK && recordSuccessfulPart(context, "delivered", messageId, partIndex, partCount)) {
                            repository.markDelivered(messageId)
                            repository.notifyRefreshForTelephonyId(messageId)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }.start()
    }

    private fun recordSuccessfulPart(
        context: Context,
        status: String,
        messageId: Long,
        partIndex: Int,
        partCount: Int,
    ): Boolean {
        val key = progressKey(status, messageId)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val deliveredParts = prefs.getStringSet(key, emptySet()).orEmpty().toMutableSet().apply {
            add(partIndex.toString())
        }
        return if (deliveredParts.size >= partCount) {
            prefs.edit().remove(key).apply()
            true
        } else {
            prefs.edit().putStringSet(key, deliveredParts).apply()
            false
        }
    }

    private fun clearPartProgress(context: Context, status: String, messageId: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(progressKey(status, messageId))
            .apply()
    }

    private fun progressKey(status: String, messageId: Long): String = "$status:$messageId"

    companion object {
        private const val PREFS_NAME = "sms_send_status_parts"
    }
}
