package com.kheyr.sms.receiver

import android.annotation.SuppressLint
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.kheyr.sms.CopyCodeActivity
import com.kheyr.sms.MainActivity
import com.kheyr.sms.R
import com.kheyr.sms.contacts.ContactRepository
import com.kheyr.sms.contacts.PhoneNumberNormalizer
import com.kheyr.sms.data.AppDatabase
import com.kheyr.sms.data.SmsRepository
import com.kheyr.sms.domain.SpamClassification
import com.kheyr.sms.domain.SpamRuleSet
import com.kheyr.sms.notifications.IncomingNotificationActions
import com.kheyr.sms.preferences.AppPreferences
import com.kheyr.sms.settings.NotificationContentMode
import com.kheyr.sms.settings.NotificationPolicyResolver
import com.kheyr.sms.settings.ThreadNotificationSettings
import com.kheyr.sms.telephony.OwnNumberResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.time.Instant

class PreferencesSpamRulesProvider(private val preferences: AppPreferences, private val defaults: SpamRuleSet) : SpamRulesProvider {
    override fun activeRuleSet(): SpamRuleSet = preferences.loadSpamRuleSet(defaults)
}

class RoomIncomingSmsStore(
    private val context: Context,
    private val repository: SmsRepository,
    private val preferences: AppPreferences,
    private val markSpam: Boolean,
    private val ownNumberResolver: OwnNumberResolver,
) : SpamMessageStore, InboxMessageStore {
    override fun persistSpam(message: IncomingSms, score: Int, triggeredRuleIds: List<String>) {
        persist(message, spam = true)
    }

    override fun persistInbox(message: IncomingSms): StoredIncomingSms = persist(message, spam = false)

    private fun persist(message: IncomingSms, spam: Boolean): StoredIncomingSms {
        if (!spam && ownNumberResolver.isOwnNumber(message.sender)) {
            runBlocking(Dispatchers.IO) {
                repository.recentOutgoingThreadId(message.sender, message.body)
            }?.let { threadId ->
                return StoredIncomingSms(
                    threadId,
                    message.sender,
                    message.body,
                    message.receivedAtMillis,
                    message.simSlot,
                    message.subscriptionId,
                )
            }
        }
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
        if (spam || markSpam) runBlocking(Dispatchers.IO) { repository.updateSpam(threadId, true) }
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
    private val contactRepository: ContactRepository,
) : IncomingSmsNotifier {
    private val resolver = NotificationPolicyResolver()

    // notify() is guarded by canPostNotifications(), which checks POST_NOTIFICATIONS on API 33+.
    @SuppressLint("MissingPermission")
    override fun show(message: StoredIncomingSms, senderIsContact: Boolean) {
        if (!canPostNotifications()) return
        val profile = runBlocking(Dispatchers.IO) { contactRepository.lookupProfile(message.sender) }
        val displayName = profile?.displayName?.takeIf { it.isNotBlank() } ?: message.sender
        val senderIsKnown = senderIsContact || profile != null
        val globalSettings = preferences.notificationSettings()
        val muted = dao.smsDao().isThreadMuted(message.threadId) == true
        val decision = resolver.resolve(
            com.kheyr.sms.settings.NotificationPolicyInput(
                sender = message.sender,
                displayName = displayName,
                preview = message.body,
                senderIsContact = senderIsKnown,
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
            .putExtra(MainActivity.EXTRA_THREAD_ID, message.threadId)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val notificationId = stableNotificationId(message.threadId)
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(decision.title)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            // PRIORITY_HIGH drives heads-up (pop-up) display on pre-O; the HIGH-importance channel does it on O+.
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        decision.body?.let {
            builder.setContentText(it)
            // BigTextStyle expands to show the full message text instead of a single truncated line.
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(it))
        }
        if (decision.soundMode == com.kheyr.sms.settings.NotificationSoundMode.Silent) {
            builder.setSilent(true)
        } else if (decision.vibrate) {
            builder.setDefaults(NotificationCompat.DEFAULT_VIBRATE)
        }
        profile?.photoUri?.let { loadNotificationIcon(it) }?.let(builder::setLargeIcon)
        val previewVisible = globalSettings.contentMode == NotificationContentMode.ShowSenderAndPreview
        addActions(builder, message, notificationId, previewVisible)
        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    private fun addActions(
        builder: NotificationCompat.Builder,
        message: StoredIncomingSms,
        notificationId: Int,
        previewVisible: Boolean,
    ) {
        // "Copy code" is the primary action for OTP messages, but only when previews are visible so a
        // hidden-content notification never leaks a verification code.
        val code = if (previewVisible) IncomingNotificationActions.copyableCode(message.body) else null
        if (code != null) {
            val copyIntent = Intent(context, CopyCodeActivity::class.java).putExtra(CopyCodeActivity.EXTRA_CODE, code)
            val copyPending = PendingIntent.getActivity(
                context,
                notificationId * 4 + 1,
                copyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(android.R.drawable.ic_menu_save, context.getString(R.string.notification_action_copy_code), copyPending)
        }

        val remoteInput = RemoteInput.Builder(NotificationActionReceiver.KEY_REPLY_TEXT)
            .setLabel(context.getString(R.string.notification_reply_hint))
            .build()
        val replyIntent = actionIntent(NotificationActionReceiver.ACTION_REPLY, message, notificationId)
        val replyPending = PendingIntent.getBroadcast(
            context,
            notificationId * 4 + 2,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            context.getString(R.string.notification_action_reply),
            replyPending,
        )
            .addRemoteInput(remoteInput)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .setAllowGeneratedReplies(true)
            .build()
        builder.addAction(replyAction)

        val markReadIntent = actionIntent(NotificationActionReceiver.ACTION_MARK_READ, message, notificationId)
        val markReadPending = PendingIntent.getBroadcast(
            context,
            notificationId * 4 + 3,
            markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        builder.addAction(android.R.drawable.ic_menu_view, context.getString(R.string.notification_action_mark_read), markReadPending)
    }

    private fun actionIntent(action: String, message: StoredIncomingSms, notificationId: Int): Intent =
        Intent(context, NotificationActionReceiver::class.java)
            .setAction(action)
            .putExtra(NotificationActionReceiver.EXTRA_THREAD_ID, message.threadId)
            .putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            .putExtra(NotificationActionReceiver.EXTRA_RECIPIENT, message.sender)
            .apply { message.subscriptionId?.let { putExtra(NotificationActionReceiver.EXTRA_SUBSCRIPTION_ID, it) } }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun loadNotificationIcon(photoUri: Uri) = runCatching {
        context.contentResolver.openInputStream(photoUri)?.use(BitmapFactory::decodeStream)
    }.getOrNull()

    private fun stableNotificationId(threadId: Long): Int = (threadId xor (threadId ushr 32)).toInt()

    private fun ensureChannel() {
        // IMPORTANCE_HIGH is required for heads-up (pop-up) notifications on API 26+. A channel's
        // importance is fixed once created, so a new id is used to upgrade existing installs.
        val channel = NotificationChannel(CHANNEL_ID, "Messages", NotificationManager.IMPORTANCE_HIGH)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object { const val CHANNEL_ID = "incoming_sms_messages" }
}

class AndroidContactLookup(private val context: Context) : SenderContactLookup {
    override fun isKnownContact(sender: String): Boolean {
        if (sender.isBlank()) return false
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return false
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(sender))
        val found = context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)?.use { it.moveToFirst() } == true
        if (found) return true
        val normalized = PhoneNumberNormalizer.normalize(sender)
        if (normalized == sender) return false
        val normalizedUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(normalized))
        return context.contentResolver.query(normalizedUri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)?.use { it.moveToFirst() } == true
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
