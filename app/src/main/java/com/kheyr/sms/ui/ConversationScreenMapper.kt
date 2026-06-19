package com.kheyr.sms.ui

import com.kheyr.sms.data.MessageDirection
import com.kheyr.sms.data.MessageStatus
import com.kheyr.sms.util.JalaliDateFormatter
import com.kheyr.sms.util.OtpDetector
import com.kheyr.sms.data.SmsMessage
import com.kheyr.sms.data.SmsThread
import com.kheyr.sms.telephony.SimCard

data class ConversationMessageRow(
    val id: Long,
    val body: String,
    val layout: ConversationBubbleLayout,
    val timeLabel: String,
    val showRetry: Boolean,
    val copyableCode: String?,
)

data class ConversationScreenModel(
    val header: ConversationHeader,
    val messages: List<ConversationMessageRow>,
    val composer: SmsComposerState,
)

class ConversationScreenMapper(
    private val headerMapper: ConversationHeaderMapper = ConversationHeaderMapper(),
) {
    fun map(thread: SmsThread, messages: List<SmsMessage>, sims: List<SimCard>, composer: SmsComposerState): ConversationScreenModel =
        ConversationScreenModel(
            header = header(thread, messages.size, sims),
            messages = messages.map(::mapMessage),
            composer = composer,
        )

    // Header-only mapping for the conversation top bar, so it doesn't map every message body
    // (date formatting + OTP regex) just to read the subtitle.
    fun header(thread: SmsThread, messageCount: Int, sims: List<SimCard>): ConversationHeader =
        headerMapper.map(
            ConversationHeaderInput(
                address = thread.address,
                displayName = thread.displayName,
                subscriptionId = thread.simSlot,
                messageCount = messageCount,
                photoUri = thread.contactPhotoUri,
            ),
            sims,
        )

    private fun mapMessage(message: SmsMessage): ConversationMessageRow = ConversationMessageRow(
        id = message.id,
        body = message.body,
        layout = ConversationBubbleLayoutResolver.resolve(message.direction, message.body),
        timeLabel = JalaliDateFormatter.format(message.timestamp),
        showRetry = message.direction == MessageDirection.Outgoing && message.status == MessageStatus.Failed,
        copyableCode = if (message.direction == MessageDirection.Incoming) {
            OtpDetector.findCopyableCode(message.body)
        } else {
            null
        },
    )
}
