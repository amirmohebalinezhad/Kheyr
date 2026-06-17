package com.kheyr.sms.ui

class ConversationHeaderMapper {
    fun map(input: ConversationHeaderInput): ConversationHeader = ConversationHeader(
        title = input.displayName.ifBlank { input.address },
        subtitle = input.simSlot?.let { "SIM ${it + 1}" },
        callEnabled = input.address.any(Char::isDigit),
        infoEnabled = true,
        searchEnabled = input.messageCount > 0,
    )
}

data class ConversationHeaderInput(
    val address: String,
    val displayName: String,
    val simSlot: Int?,
    val messageCount: Int,
)

data class ConversationHeader(
    val title: String,
    val subtitle: String?,
    val callEnabled: Boolean,
    val infoEnabled: Boolean,
    val searchEnabled: Boolean,
)
