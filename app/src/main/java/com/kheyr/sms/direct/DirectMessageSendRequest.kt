package com.kheyr.sms.direct

data class DirectMessageSendRequest(val recipientHash: String, val body: String, val clientMessageId: String)
