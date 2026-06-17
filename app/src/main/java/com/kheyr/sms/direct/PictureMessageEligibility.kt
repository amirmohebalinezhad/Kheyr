package com.kheyr.sms.direct

object PictureMessageEligibility {
    fun canSendPicture(mode: MessageTransportMode): Boolean = mode == MessageTransportMode.Direct
}
