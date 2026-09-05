package com.kheyr.sms

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kheyr.sms.ui.KheyrAppShell

/**
 * An `sms:` / `smsto:` / `mms:` / `mmsto:` send request handed to us by another app (contact card,
 * dialer, browser link, or our own "send SMS to this number" copy action). [address] is null when the
 * link carries no recipient, in which case the shell lets the user pick one.
 */
data class SendToRequest(val address: String?, val body: String?)

class MainActivity : ComponentActivity() {
    private var openThreadId by mutableStateOf<Long?>(null)
    private var sendToRequest by mutableStateOf<SendToRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openThreadId = threadIdFrom(intent)
        sendToRequest = sendToRequestFrom(intent)
        setContent {
            KheyrAppShell(
                openThreadId = openThreadId,
                onThreadConsumed = { openThreadId = null },
                sendTo = sendToRequest,
                onSendToConsumed = { sendToRequest = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openThreadId = threadIdFrom(intent)
        sendToRequest = sendToRequestFrom(intent)
    }

    private fun threadIdFrom(intent: Intent?): Long? =
        intent?.getLongExtra(EXTRA_THREAD_ID, -1L)?.takeIf { it >= 0 }

    /**
     * Parses ACTION_SENDTO data. Manifest-registered schemes only, so a LAUNCHER start (no data)
     * and any other deep link fall through to the plain inbox.
     */
    private fun sendToRequestFrom(intent: Intent?): SendToRequest? {
        val data = intent?.data ?: return null
        val scheme = data.scheme?.lowercase() ?: return null
        if (scheme !in SMS_SCHEMES) return null
        // "sms:+15551234?body=hi" is an opaque URI, so Uri.getQuery() is null and the query rides
        // along in the scheme-specific part; split it off by hand.
        // The ENCODED form: Uri.schemeSpecificPart is already percent-decoded, so decoding the pieces
        // again below would corrupt any body containing an escaped '&' or '%' - "%26" would become a
        // real '&' and split the query in the wrong place.
        val schemeSpecificPart = data.encodedSchemeSpecificPart.orEmpty()
        // Some senders write the hierarchical "sms://+15551234" form, which keeps the slashes here.
        val recipients = schemeSpecificPart.substringBefore('?').removePrefix("//")
        val query = schemeSpecificPart.substringAfter('?', "")
        // A multi-recipient link ("smsto:+1555,+1666") is legal; this app opens a 1:1 thread, so
        // only the first recipient is used. The part is percent-encoded ("%2B" for a leading plus).
        val address = Uri.decode(recipients.substringBefore(',')).trim().takeIf { it.isNotEmpty() }
        val body = (
            intent.getStringExtra(EXTRA_SMS_BODY)
                ?: intent.getStringExtra(Intent.EXTRA_TEXT)
                ?: bodyFromQuery(query)
            )?.takeIf { it.isNotBlank() }
        return if (address == null && body == null) null else SendToRequest(address, body)
    }

    private fun bodyFromQuery(query: String): String? = query
        .split('&')
        .firstOrNull { it.startsWith("$QUERY_BODY=", ignoreCase = true) }
        ?.substringAfter('=')
        ?.let { Uri.decode(it) }

    companion object {
        const val EXTRA_THREAD_ID = "open_thread_id"
        private const val EXTRA_SMS_BODY = "sms_body"
        private const val QUERY_BODY = "body"
        private val SMS_SCHEMES = setOf("sms", "smsto", "mms", "mmsto")
    }
}
