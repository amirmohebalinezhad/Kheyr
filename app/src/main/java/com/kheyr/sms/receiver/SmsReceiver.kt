package com.kheyr.sms.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SmsMessage
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kheyr.sms.MainActivity
import com.kheyr.sms.domain.SpamRule
import com.kheyr.sms.domain.SpamRuleSet
import com.kheyr.sms.domain.SpamRuleType

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Telephony.Sms.Intents.SMS_RECEIVED_ACTION, Telephony.Sms.Intents.SMS_DELIVER_ACTION)) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return

        val sender = messages.firstOrNull()?.originatingAddress.orEmpty()
        val body = messages.joinToString(separator = "") { it.messageBody.orEmpty() }
        if (sender.isBlank() && body.isBlank()) return

        val receivedAt = messages.minOfOrNull(SmsMessage::getTimestampMillis) ?: System.currentTimeMillis()
        val subscriptionId = intent.getIntExtra("subscription", -1).takeIf { it >= 0 }
            ?: intent.getIntExtra("android.telephony.extra.SUBSCRIPTION_INDEX", -1).takeIf { it >= 0 }
        val simSlot = intent.getIntExtra("slot", -1).takeIf { it >= 0 }
            ?: intent.getIntExtra("simSlot", -1).takeIf { it >= 0 }
            ?: intent.getIntExtra("android.telephony.extra.SLOT_INDEX", -1).takeIf { it >= 0 }

        val handler = SmsReceiveHandler(
            spamRules = SharedPreferencesSpamRulesProvider(context),
            contactLookup = AndroidContactLookup(context),
            spamStore = AndroidSmsStore(context, isSpam = true),
            inboxStore = AndroidSmsStore(context, isSpam = false),
            notifier = AndroidIncomingSmsNotifier(context),
        )
        handler.handle(IncomingSms(sender, body, receivedAt, simSlot, subscriptionId))
    }
}

private class SharedPreferencesSpamRulesProvider(context: Context) : SpamRulesProvider {
    private val preferences = context.getSharedPreferences("spam_rules", Context.MODE_PRIVATE)

    override fun activeRuleSet(): SpamRuleSet {
        val threshold = preferences.getInt(KEY_THRESHOLD, DEFAULT_RULE_SET.threshold)
        val encodedRules = preferences.getStringSet(KEY_RULES, null).orEmpty()
        val rules = encodedRules.mapNotNull(::decodeRule).ifEmpty { DEFAULT_RULE_SET.rules }
        return SpamRuleSet(version = preferences.getInt(KEY_VERSION, DEFAULT_RULE_SET.version), threshold = threshold, rules = rules)
    }

    private fun decodeRule(encoded: String): SpamRule? {
        val parts = encoded.split('|')
        if (parts.size != 5) return null
        return runCatching {
            SpamRule(
                id = Uri.decode(parts[0]),
                type = SpamRuleType.valueOf(parts[1]),
                pattern = Uri.decode(parts[2]).ifBlank { null },
                score = parts[3].toInt(),
                enabled = parts[4].toBoolean(),
            )
        }.getOrNull()
    }

    private companion object {
        const val KEY_VERSION = "active_version"
        const val KEY_THRESHOLD = "threshold"
        const val KEY_RULES = "active_rules"
        val DEFAULT_RULE_SET = SpamRuleSet(1, threshold = 70, rules = listOf(
            SpamRule("spam-keyword-winner", SpamRuleType.MessageKeyword, "winner", 35),
            SpamRule("spam-keyword-prize", SpamRuleType.MessageKeyword, "prize", 35),
            SpamRule("spam-url", SpamRuleType.UrlDetected, score = 35),
            SpamRule("spam-suspicious-link", SpamRuleType.SuspiciousLinkPattern, score = 45),
            SpamRule("unknown-sender", SpamRuleType.SenderNotInContacts, score = 25),
            SpamRule("otp-code", SpamRuleType.OtpRegex, "\\b(code|otp)\\b.*\\b\\d{4,8}\\b|\\b\\d{4,8}\\b.*\\b(code|otp)\\b", -50),
        ))
    }
}

private class AndroidContactLookup(private val context: Context) : SenderContactLookup {
    override fun isKnownContact(sender: String): Boolean {
        if (sender.isBlank()) return false
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(sender))
        return context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)?.use { it.moveToFirst() } == true
    }
}

private class AndroidSmsStore(private val context: Context, private val isSpam: Boolean) : SpamMessageStore, InboxMessageStore {
    override fun persistSpam(message: IncomingSms, score: Int, triggeredRuleIds: List<String>) {
        persist(message, true)
    }

    override fun persistInbox(message: IncomingSms): StoredIncomingSms = persist(message, false)

    private fun persist(message: IncomingSms, spam: Boolean): StoredIncomingSms {
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, message.sender)
            put(Telephony.Sms.BODY, message.body)
            put(Telephony.Sms.DATE, message.receivedAtMillis)
            put(Telephony.Sms.READ, 0)
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
            message.subscriptionId?.let { put("sub_id", it) }
        }
        val uri = context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
        val threadId = uri?.lastPathSegment?.toLongOrNull() ?: message.receivedAtMillis
        context.getSharedPreferences("sms_classification", Context.MODE_PRIVATE).edit()
            .putBoolean("thread:$threadId:spam", spam || isSpam)
            .apply()
        return StoredIncomingSms(threadId, message.sender, message.body, message.receivedAtMillis, message.simSlot, message.subscriptionId)
    }
}

private class AndroidIncomingSmsNotifier(private val context: Context) : IncomingSmsNotifier {
    override fun show(message: StoredIncomingSms, senderIsContact: Boolean) {
        ensureChannel()
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setContentTitle(message.sender)
            .setContentText(message.body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(message.threadId.toInt(), notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Messages", NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    private companion object { const val CHANNEL_ID = "incoming_sms" }
}
