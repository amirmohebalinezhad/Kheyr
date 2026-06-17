package com.kheyr.sms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Phase 0 hook: incoming SMS will be scored before notifications and persisted locally.
    }
}
