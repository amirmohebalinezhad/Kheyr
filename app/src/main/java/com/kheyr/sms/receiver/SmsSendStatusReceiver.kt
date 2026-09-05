package com.kheyr.sms.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log
import com.kheyr.sms.data.SmsRepository
import com.kheyr.sms.telephony.SmsSender

class SmsSendStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getLongExtra(SmsSender.EXTRA_MESSAGE_ID, -1L).takeIf { it > 0L } ?: return
        val partIndex = intent.getIntExtra(SmsSender.EXTRA_PART_INDEX, 0)
        val partCount = intent.getIntExtra(SmsSender.EXTRA_PART_COUNT, 1).coerceAtLeast(1)
        val callbackResult = resultCode
        val reportStatus = deliveryReportStatus(intent)
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        Thread {
            try {
                val repository = SmsRepository(appContext)
                when (intent.action) {
                    SmsSender.ACTION_SMS_SENT -> {
                        if (SmsSendStatusDecider.sentSucceeded(callbackResult)) {
                            if (recordSuccessfulPart(appContext, "sent", messageId, partIndex, partCount)) {
                                repository.markSent(messageId)
                                repository.notifyRefreshForTelephonyId(messageId)
                            }
                        } else {
                            clearPartProgress(appContext, "sent", messageId)
                            clearPartProgress(appContext, "delivered", messageId)
                            repository.markFailed(messageId)
                            repository.notifyRefreshForTelephonyId(messageId)
                        }
                    }
                    SmsSender.ACTION_SMS_DELIVERED -> when (SmsSendStatusDecider.deliveryOutcome(reportStatus)) {
                        // The network is still trying; the message keeps whatever status it has.
                        DeliveryOutcome.Pending -> Unit
                        DeliveryOutcome.Delivered ->
                            if (SmsSendStatusDecider.sentSucceeded(callbackResult) && recordSuccessfulPart(appContext, "delivered", messageId, partIndex, partCount)) {
                                repository.markDelivered(messageId)
                                repository.notifyRefreshForTelephonyId(messageId)
                            }
                        DeliveryOutcome.Failed -> {
                            clearPartProgress(appContext, "delivered", messageId)
                            repository.markFailed(messageId)
                            repository.notifyRefreshForTelephonyId(messageId)
                        }
                    }
                }
            } catch (t: Throwable) {
                // Opening the database or writing to the provider (after the default-SMS role was
                // lost) can throw; an uncaught exception on this bare thread would kill the process.
                Log.e("SmsSendStatus", "Failed to handle send status broadcast", t)
            } finally {
                pendingResult.finish()
            }
        }.start()
    }

    /**
     * The TP-Status byte of the delivery report, or null when the broadcast carries no report PDU or
     * it cannot be parsed - in which case the caller falls back to the platform result code.
     */
    private fun deliveryReportStatus(intent: Intent): Int? {
        val pdu = intent.getByteArrayExtra("pdu") ?: return null
        return runCatching { SmsMessage.createFromPdu(pdu, intent.getStringExtra("format"))?.status }.getOrNull()
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
        synchronized(LOCK) {
            val deliveredParts = prefs.getStringSet(key, emptySet()).orEmpty().toMutableSet().apply {
                add(partIndex.toString())
            }
            return if (deliveredParts.size >= partCount) {
                prefs.edit().remove(key).commit()
                true
            } else {
                prefs.edit().putStringSet(key, deliveredParts).commit()
                false
            }
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

        /** Process-wide lock guarding the non-atomic part-progress read-modify-write. */
        private val LOCK = Any()
    }
}

internal enum class DeliveryOutcome { Delivered, Pending, Failed }

internal object SmsSendStatusDecider {
    fun sentSucceeded(resultCode: Int): Boolean = resultCode == Activity.RESULT_OK

    /**
     * Classifies the TP-Status byte of a delivery report (3GPP TS 23.040 section 9.2.3.15): 0x00-0x1F
     * means the message reached the recipient, 0x20-0x3F that the network is still trying, and 0x40
     * and above a permanent failure. The platform always reports RESULT_OK for a delivery broadcast,
     * so without this a negative report would be shown as "Delivered". A null status (no report PDU,
     * or one that could not be parsed) keeps the previous behaviour of trusting the result code.
     */
    fun deliveryOutcome(reportStatus: Int?): DeliveryOutcome = when {
        reportStatus == null -> DeliveryOutcome.Delivered
        reportStatus < 0x20 -> DeliveryOutcome.Delivered
        reportStatus < 0x40 -> DeliveryOutcome.Pending
        else -> DeliveryOutcome.Failed
    }
}
