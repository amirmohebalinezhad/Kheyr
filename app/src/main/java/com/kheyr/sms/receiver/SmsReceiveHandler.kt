package com.kheyr.sms.receiver

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
            spamStore.persistSpam(message, score.total, score.triggeredRuleIds)
            IncomingSmsResult.SpamSuppressed
        } else {
            val storedMessage = inboxStore.persistInbox(message)
            if (shouldNotify(message)) {
                notifier.show(storedMessage, senderIsContact)
            }
            IncomingSmsResult.NotificationPosted
        }
    }
}

data class IncomingSms(
    val sender: String,
    val body: String,
    val receivedAtMillis: Long,
    val simSlot: Int?,
    val subscriptionId: Int?,
)

data class StoredIncomingSms(
    val threadId: Long,
    val sender: String,
    val body: String,
    val receivedAtMillis: Long,
    val simSlot: Int?,
    val subscriptionId: Int?,
)

enum class IncomingSmsResult { SpamSuppressed, NotificationPosted }

fun interface SpamRulesProvider { fun activeRuleSet(): SpamRuleSet }
fun interface SenderContactLookup { fun isKnownContact(sender: String): Boolean }
fun interface SpamMessageStore { fun persistSpam(message: IncomingSms, score: Int, triggeredRuleIds: List<String>) }
fun interface InboxMessageStore { fun persistInbox(message: IncomingSms): StoredIncomingSms }
fun interface IncomingSmsNotifier { fun show(message: StoredIncomingSms, senderIsContact: Boolean) }
