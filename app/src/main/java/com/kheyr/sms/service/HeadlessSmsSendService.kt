package com.kheyr.sms.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.kheyr.sms.data.SmsRepository
import com.kheyr.sms.telephony.SmsSender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Handles respond-via-message intents when Kheyr is the default SMS app. */
class HeadlessSmsSendService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != "android.intent.action.RESPOND_VIA_MESSAGE") {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        val data = intent.data ?: run {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        val recipient = data.schemeSpecificPart.orEmpty()
        val body = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        if (recipient.isBlank() || body.isBlank()) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        scope.launch {
            val repository = SmsRepository(this@HeadlessSmsSendService)
            val sender = SmsSender(this@HeadlessSmsSendService)
            val messageId = repository.persistOutgoing(recipient, body, null)
            try {
                repository.markSending(messageId)
                sender.send(com.kheyr.sms.telephony.SmsSendRequest(recipient, body, null, messageId))
            } catch (t: Throwable) {
                repository.markFailed(messageId)
            } finally {
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }
}
