package com.kheyr.sms.ui

import com.kheyr.sms.telephony.SimBadgeResolver
import com.kheyr.sms.telephony.SimCard

class ConversationHeaderMapper {
    fun map(input: ConversationHeaderInput, activeSims: List<SimCard> = emptyList()): ConversationHeader = ConversationHeader(
        title = input.displayName.ifBlank { input.address },
        subtitle = SimBadgeResolver.badge(input.subscriptionId, activeSims),
        callEnabled = input.address.any(Char::isDigit),
        infoEnabled = true,
        searchEnabled = input.messageCount > 0,
    )
}

data class ConversationHeaderInput(
    val address: String,
    val displayName: String,
    val subscriptionId: Int?,
    val messageCount: Int,
)

data class ConversationHeader(
    val title: String,
    val subtitle: String?,
    val callEnabled: Boolean,
    val infoEnabled: Boolean,
    val searchEnabled: Boolean,
)
