package com.kheyr.sms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val message = SmsReceiveHandlerFactory.parseIncoming(intent) ?: return
        val pendingResult = goAsync()
        Thread {
            try {
                SmsReceiveHandlerFactory.create(context).handle(message)
            } catch (t: Throwable) {
                // Never let a single message (e.g. an alphanumeric sender ID some providers choke on)
                // take down the receive thread silently; log so it is diagnosable instead of lost.
                Log.e("SmsReceiver", "Failed to handle incoming SMS", t)
            } finally {
                pendingResult.finish()
            }
        }.start()
    }
}
