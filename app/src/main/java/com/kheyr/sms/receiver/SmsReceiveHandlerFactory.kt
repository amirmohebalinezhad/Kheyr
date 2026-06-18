package com.kheyr.sms.receiver

import android.content.Context
import android.content.Intent
import com.kheyr.sms.contacts.ContactRepository
import com.kheyr.sms.data.AppDatabase
import com.kheyr.sms.data.SmsRepository
import com.kheyr.sms.preferences.AppPreferences
import com.kheyr.sms.telephony.OwnNumberResolver

object SmsReceiveHandlerFactory {
    fun create(context: Context): SmsReceiveHandler {
        val appContext = context.applicationContext
        val preferences = AppPreferences(appContext)
        val repository = SmsRepository(appContext)
        val database = AppDatabase.getInstance(appContext)
        val ownNumberResolver = OwnNumberResolver(appContext)
        val contactRepository = ContactRepository(appContext)
        val store = RoomIncomingSmsStore(appContext, repository, preferences, markSpam = false, ownNumberResolver = ownNumberResolver)
        return SmsReceiveHandler(
            spamRules = PreferencesSpamRulesProvider(preferences, DefaultSpamRuleSet.rules),
            contactLookup = AndroidContactLookup(appContext),
            spamStore = RoomIncomingSmsStore(appContext, repository, preferences, markSpam = true, ownNumberResolver = ownNumberResolver),
            inboxStore = store,
            notifier = PolicyAwareIncomingSmsNotifier(appContext, preferences, database, contactRepository),
            shouldNotify = { message -> !ownNumberResolver.isOwnNumber(message.sender) },
        )
    }

    fun parseIncoming(intent: Intent): IncomingSms? {
        if (intent.action !in setOf(android.provider.Telephony.Sms.Intents.SMS_RECEIVED_ACTION, android.provider.Telephony.Sms.Intents.SMS_DELIVER_ACTION)) return null
        val messages = android.provider.Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return null
        val sender = messages.firstOrNull()?.originatingAddress.orEmpty()
        val body = messages.joinToString(separator = "") { it.messageBody.orEmpty() }
        if (sender.isBlank() && body.isBlank()) return null
        val receivedAt = messages.minOfOrNull(android.telephony.SmsMessage::getTimestampMillis) ?: System.currentTimeMillis()
        val subscriptionId = intent.getIntExtra("subscription", -1).takeIf { it >= 0 }
            ?: intent.getIntExtra("android.telephony.extra.SUBSCRIPTION_INDEX", -1).takeIf { it >= 0 }
        val simSlot = intent.getIntExtra("slot", -1).takeIf { it >= 0 }
            ?: intent.getIntExtra("simSlot", -1).takeIf { it >= 0 }
            ?: intent.getIntExtra("android.telephony.extra.SLOT_INDEX", -1).takeIf { it >= 0 }
        return IncomingSms(sender, body, receivedAt, simSlot, subscriptionId)
    }
}
