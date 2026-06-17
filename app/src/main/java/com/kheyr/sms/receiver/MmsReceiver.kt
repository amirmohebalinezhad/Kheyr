package com.kheyr.sms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Minimal MMS deliver receiver required for default-SMS role eligibility. */
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Unit
}
