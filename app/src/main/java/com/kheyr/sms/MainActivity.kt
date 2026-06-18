package com.kheyr.sms

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kheyr.sms.ui.KheyrAppShell

class MainActivity : ComponentActivity() {
    private var openThreadId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openThreadId = threadIdFrom(intent)
        setContent { KheyrAppShell(openThreadId = openThreadId, onThreadConsumed = { openThreadId = null }) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openThreadId = threadIdFrom(intent)
    }

    private fun threadIdFrom(intent: Intent?): Long? =
        intent?.getLongExtra(EXTRA_THREAD_ID, -1L)?.takeIf { it >= 0 }

    companion object {
        const val EXTRA_THREAD_ID = "open_thread_id"
    }
}
