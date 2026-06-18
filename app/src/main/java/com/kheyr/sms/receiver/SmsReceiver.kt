package com.kheyr.sms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val message = SmsReceiveHandlerFactory.parseIncoming(intent) ?: return
        val pendingResult = goAsync()
        Thread {
            try {
                SmsReceiveHandlerFactory.create(context).handle(message)
            } finally {
                pendingResult.finish()
            }
        }.start()
    }
}
