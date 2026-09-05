package com.kheyr.sms.receiver

import com.kheyr.sms.data.SmsRefreshEvents
import com.kheyr.sms.domain.SpamClassification
import com.kheyr.sms.domain.SpamRuleSet
import com.kheyr.sms.domain.SpamScorer

/** Coordinates incoming SMS spam suppression before any notification policy is evaluated. */
class SmsReceiveHandler(
    private val spamRules: SpamRulesProvider,
    private val contactLookup: SenderContactLookup,
    private val spamStore: SpamMessageStore,
    private val inboxStore: InboxMessageStore,
    private val notifier: IncomingSmsNotifier,
    private val shouldNotify: (IncomingSms) -> Boolean = { true },
) {
    fun handle(message: IncomingSms): IncomingSmsResult {
        val senderIsContact = contactLookup.isKnownContact(message.sender)
        val score = SpamScorer(spamRules.activeRuleSet()).score(message.sender, message.body, senderIsContact)
        return if (score.classification == SpamClassification.Spam) {
            val outcome = spamStore.persistSpam(message, score.total, score.triggeredRuleIds)
            // An open Spam folder is driven by the same refresh events as the inbox, so it needs one
            // here too or newly arrived spam is invisible until the screen is reopened.
            SmsRefreshEvents.notifyThreadChanged(outcome.stored.threadId)
            if (outcome.treatedAsSpam) {
                IncomingSmsResult.SpamSuppressed
            } else {
                // The user restored this thread with "Not spam", so the classifier neither hides it
                // nor gets to swallow the notification - otherwise every later message from that
                // sender would arrive silently and the correction would be worse than useless.
                if (shouldNotify(message)) {
                    notifier.show(outcome.stored, senderIsContact)
                }
                IncomingSmsResult.NotificationPosted
            }
        } else {
            val storedMessage = inboxStore.persistInbox(message)
            SmsRefreshEvents.notifyThreadChanged(storedMessage.threadId)
            if (shouldNotify(message)) {
                notifier.show(storedMessage, senderIsContact)
            }
            IncomingSmsResult.NotificationPosted
        }
    }
}

/**
 * [receivedAtMillis] is the device clock at reception and [sentAtMillis] the SMSC timestamp from the
 * PDU. Android's convention is DATE = device receive time, DATE_SENT = SMSC time; a network-delayed
 * message would otherwise sort into the past and its thread would not rise to the top of the inbox.
 */
data class IncomingSms(
    val sender: String,
    val body: String,
    val receivedAtMillis: Long,
    val sentAtMillis: Long?,
    val simSlot: Int?,
    val subscriptionId: Int?,
)

data class StoredIncomingSms(
    val threadId: Long,
    val sender: String,
    val body: String,
    val receivedAtMillis: Long,
    val sentAtMillis: Long?,
    val simSlot: Int?,
    val subscriptionId: Int?,
)

enum class IncomingSmsResult { SpamSuppressed, NotificationPosted }

/**
 * Result of storing a message the scorer called spam. [treatedAsSpam] is false when the user has
 * previously restored the thread with "Not spam", in which case the message belongs in the inbox.
 */
data class SpamPersistOutcome(val stored: StoredIncomingSms, val treatedAsSpam: Boolean)

fun interface SpamRulesProvider { fun activeRuleSet(): SpamRuleSet }
fun interface SenderContactLookup { fun isKnownContact(sender: String): Boolean }
fun interface SpamMessageStore { fun persistSpam(message: IncomingSms, score: Int, triggeredRuleIds: List<String>): SpamPersistOutcome }
fun interface InboxMessageStore { fun persistInbox(message: IncomingSms): StoredIncomingSms }
fun interface IncomingSmsNotifier { fun show(message: StoredIncomingSms, senderIsContact: Boolean) }
