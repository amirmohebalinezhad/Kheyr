package com.kheyr.sms.receiver

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kheyr.sms.MainActivity
import com.kheyr.sms.data.AppDatabase
import com.kheyr.sms.data.SmsRepository
import com.kheyr.sms.domain.SpamClassification
import com.kheyr.sms.domain.SpamRuleSet
import com.kheyr.sms.preferences.AppPreferences
import com.kheyr.sms.settings.NotificationPolicyResolver
import com.kheyr.sms.settings.ThreadNotificationSettings
import java.time.Instant

class PreferencesSpamRulesProvider(private val preferences: AppPreferences, private val defaults: SpamRuleSet) : SpamRulesProvider {
    override fun activeRuleSet(): SpamRuleSet = preferences.loadSpamRuleSet(defaults)
}

class RoomIncomingSmsStore(
    private val context: Context,
    private val repository: SmsRepository,
    private val preferences: AppPreferences,
    private val markSpam: Boolean,
) : SpamMessageStore, InboxMessageStore {
    override fun persistSpam(message: IncomingSms, score: Int, triggeredRuleIds: List<String>) {
        persist(message, spam = true)
    }

    override fun persistInbox(message: IncomingSms): StoredIncomingSms = persist(message, spam = false)

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
        val telephonyId = uri?.let(ContentUris::parseId)
        val threadId = telephonyId?.let(::threadIdForMessageRow)
            ?: Telephony.Threads.getOrCreateThreadId(context, setOf(message.sender))

        repository.insertIncomingSms(
            threadId = threadId,
            address = message.sender,
            body = message.body,
            timestamp = Instant.ofEpochMilli(message.receivedAtMillis),
            telephonyId = telephonyId,
            read = false,
            simSlot = message.simSlot,
        )
        if (spam || markSpam) repository.updateSpam(threadId, true)
        return StoredIncomingSms(threadId, message.sender, message.body, message.receivedAtMillis, message.simSlot, message.subscriptionId)
    }

    private fun threadIdForMessageRow(messageRowId: Long): Long? = context.contentResolver.query(
        ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, messageRowId),
        arrayOf(Telephony.Sms.THREAD_ID),
        null,
        null,
        null,
    )?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null }
}

class PolicyAwareIncomingSmsNotifier(
    private val context: Context,
    private val preferences: AppPreferences,
    private val dao: AppDatabase,
) : IncomingSmsNotifier {
    private val resolver = NotificationPolicyResolver()

    override fun show(message: StoredIncomingSms, senderIsContact: Boolean) {
        val globalSettings = preferences.notificationSettings()
        val muted = dao.smsDao().isThreadMuted(message.threadId) == true
        val decision = resolver.resolve(
            com.kheyr.sms.settings.NotificationPolicyInput(
                sender = message.sender,
                displayName = message.sender,
                preview = message.body,
                senderIsContact = senderIsContact,
                senderBlocked = preferences.isBlockedSender(message.sender),
                spamClassification = SpamClassification.Normal,
                globalSettings = globalSettings,
                threadSettings = ThreadNotificationSettings(message.threadId, muted = muted),
            ),
        )
        if (decision is com.kheyr.sms.settings.NotificationPolicyDecision.Suppress) return
        decision as com.kheyr.sms.settings.NotificationPolicyDecision.Post
        ensureChannel()
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, message.threadId.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setContentTitle(decision.title)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        decision.body?.let(builder::setContentText)
        if (decision.soundMode == com.kheyr.sms.settings.NotificationSoundMode.Silent) {
            builder.setSilent(true)
        }
        NotificationManagerCompat.from(context).notify(message.threadId.toInt(), builder.build())
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(NotificationChannel(CHANNEL_ID, "Messages", NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    private companion object { const val CHANNEL_ID = "incoming_sms" }
}

class AndroidContactLookup(private val context: Context) : SenderContactLookup {
    override fun isKnownContact(sender: String): Boolean {
        if (sender.isBlank()) return false
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return false
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(sender))
        return context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)?.use { it.moveToFirst() } == true
    }
}

object DefaultSpamRuleSet {
    val rules = com.kheyr.sms.domain.SpamRuleSet(
        version = 1,
        threshold = 70,
        rules = listOf(
            com.kheyr.sms.domain.SpamRule("spam-keyword-winner", com.kheyr.sms.domain.SpamRuleType.MessageKeyword, "winner", 35),
            com.kheyr.sms.domain.SpamRule("spam-keyword-prize", com.kheyr.sms.domain.SpamRuleType.MessageKeyword, "prize", 35),
            com.kheyr.sms.domain.SpamRule("spam-url", com.kheyr.sms.domain.SpamRuleType.UrlDetected, score = 35),
            com.kheyr.sms.domain.SpamRule("spam-suspicious-link", com.kheyr.sms.domain.SpamRuleType.SuspiciousLinkPattern, score = 45),
            com.kheyr.sms.domain.SpamRule("unknown-sender", com.kheyr.sms.domain.SpamRuleType.SenderNotInContacts, score = 25),
            com.kheyr.sms.domain.SpamRule("otp-code", com.kheyr.sms.domain.SpamRuleType.OtpRegex, "\\b(code|otp)\\b.*\\b\\d{4,8}\\b|\\b\\d{4,8}\\b.*\\b(code|otp)\\b", -50),
        ),
    )
}
