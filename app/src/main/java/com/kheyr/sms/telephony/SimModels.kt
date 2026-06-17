package com.kheyr.sms.telephony

data class SimCard(
    val subscriptionId: Int,
    val slotIndex: Int,
    val displayName: String,
    val carrierName: String,
    val phoneNumber: String? = null,
)

data class SmsSendRequest(
    val recipient: String,
    val body: String,
    val subscriptionId: Int? = null,
)

data class SmsSendResult(
    val parts: Int,
    val subscriptionId: Int?,
)
