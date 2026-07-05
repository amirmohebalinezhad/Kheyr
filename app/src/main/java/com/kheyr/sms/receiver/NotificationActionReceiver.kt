package com.kheyr.sms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.kheyr.sms.data.SmsRefreshEvents
import com.kheyr.sms.data.SmsRepository
import com.kheyr.sms.notifications.IncomingNotificationActions
import com.kheyr.sms.telephony.SmsSendRequest
import com.kheyr.sms.telephony.SmsSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/** Handles the inline "Reply" and "Mark as read" actions on incoming-SMS notifications. */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val threadId = intent.getLongExtra(EXTRA_THREAD_ID, -1L).takeIf { it >= 0 } ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val recipient = intent.getStringExtra(EXTRA_RECIPIENT).orEmpty()
        val subscriptionId = intent.getIntExtra(EXTRA_SUBSCRIPTION_ID, -1).takeIf { it >= 0 }
        val appContext = context.applicationContext
        val reply = IncomingNotificationActions.sanitizeReply(
            RemoteInput.getResultsFromIntent(intent)?.getCharSequence(KEY_REPLY_TEXT),
        )

        val pendingResult = goAsync()
        Thread {
            try {
                val repository = SmsRepository(appContext)
                when (action) {
                    ACTION_MARK_READ -> runBlocking(Dispatchers.IO) {
                        repository.markThreadRead(threadId)
                        SmsRefreshEvents.notifyThreadChanged(threadId)
                    }
                    ACTION_REPLY -> if (reply != null && recipient.isNotBlank()) {
                        runBlocking(Dispatchers.IO) {
                            val messageId = repository.persistOutgoing(recipient, reply, subscriptionId)
                            repository.markSending(messageId)
                            repository.syncTelephonyMessagesByIds(listOf(messageId))
                            SmsSender(appContext).send(SmsSendRequest(recipient, reply, subscriptionId, messageId))
                            repository.markThreadRead(threadId)
                            SmsRefreshEvents.notifyThreadChanged(threadId)
                        }
                    }
                }
                NotificationManagerCompat.from(appContext).cancel(notificationId)
            } catch (t: Throwable) {
                // Sending/recording requires being the default SMS app; if the user has since changed
                // it, the provider write throws. Log instead of crashing the process from a background
                // broadcast thread.
                Log.e("NotificationAction", "Failed to handle notification action", t)
            } finally {
                pendingResult.finish()
            }
        }.start()
    }

    companion object {
        const val ACTION_MARK_READ = "com.kheyr.sms.action.NOTIF_MARK_READ"
        const val ACTION_REPLY = "com.kheyr.sms.action.NOTIF_REPLY"
        const val EXTRA_THREAD_ID = "thread_id"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_RECIPIENT = "recipient"
        const val EXTRA_SUBSCRIPTION_ID = "subscription_id"
        const val KEY_REPLY_TEXT = "reply_text"
    }
}
