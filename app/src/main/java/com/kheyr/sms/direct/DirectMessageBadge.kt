package com.kheyr.sms.direct

object DirectMessageBadge {
    fun label(mode: MessageTransportMode): String = when (mode) {
        MessageTransportMode.Sms -> "SMS"
        MessageTransportMode.Direct -> "Direct"
    }
}
