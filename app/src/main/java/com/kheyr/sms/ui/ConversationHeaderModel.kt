package com.kheyr.sms.ui

import android.net.Uri
import com.kheyr.sms.telephony.SimBadgeResolver
import com.kheyr.sms.telephony.SimCard

class ConversationHeaderMapper {
    fun map(input: ConversationHeaderInput, activeSims: List<SimCard> = emptyList()): ConversationHeader = ConversationHeader(
        title = input.displayName.ifBlank { input.address },
        subtitle = SimBadgeResolver.badge(input.subscriptionId, activeSims),
        callEnabled = input.address.any(Char::isDigit),
        infoEnabled = true,
        searchEnabled = input.messageCount > 0,
        photoUri = input.photoUri,
    )
}

data class ConversationHeaderInput(
    val address: String,
    val displayName: String,
    val subscriptionId: Int?,
    val messageCount: Int,
    val photoUri: Uri? = null,
)

data class ConversationHeader(
    val title: String,
    val subtitle: String?,
    val callEnabled: Boolean,
    val infoEnabled: Boolean,
    val searchEnabled: Boolean,
    val photoUri: Uri? = null,
)
