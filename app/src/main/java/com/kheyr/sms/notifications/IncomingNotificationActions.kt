package com.kheyr.sms.notifications

import com.kheyr.sms.util.OtpDetector

/**
 * Pure decisions backing the rich incoming-SMS notification (heads-up actions).
 * Kept free of Android types so the logic can be unit-tested on the JVM.
 */
object IncomingNotificationActions {
    /** The single copyable verification code in [body], or null when there is none / it is ambiguous. */
    fun copyableCode(body: String): String? = OtpDetector.findCopyableCode(body)

    /** Trimmed inline-reply text, or null when the user submitted only whitespace. */
    fun sanitizeReply(raw: CharSequence?): String? = raw?.toString()?.trim()?.takeIf { it.isNotEmpty() }
}
