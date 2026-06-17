package com.kheyr.sms.desktop

data class DesktopRelayRequest(val requestId: String, val recipient: String, val body: String, val simId: Int?)

class DesktopRelayRequestHandler {
    fun canProcess(phoneOnline: Boolean, request: DesktopRelayRequest): Boolean =
        phoneOnline && request.recipient.isNotBlank() && request.body.isNotBlank()
}
