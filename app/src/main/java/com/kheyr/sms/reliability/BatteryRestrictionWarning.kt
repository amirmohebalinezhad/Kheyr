package com.kheyr.sms.reliability

data class BatteryRestrictionWarning(val restricted: Boolean, val manufacturer: String?) {
    val shouldShow: Boolean get() = restricted
    val message: String get() = if (manufacturer.isNullOrBlank()) "Battery restrictions may delay sync." else "$manufacturer battery restrictions may delay sync."
}
