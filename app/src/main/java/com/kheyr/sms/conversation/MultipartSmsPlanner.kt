package com.kheyr.sms.conversation

data class MultipartSmsPlan(val body: String, val segmentLength: Int = 160) {
    val segmentCount: Int get() = if (body.isEmpty()) 0 else ((body.length - 1) / segmentLength) + 1
    val requiresMultipart: Boolean get() = segmentCount > 1
}
