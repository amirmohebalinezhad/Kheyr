package com.kheyr.sms.settings

data class LocalDataDeletePlan(val deleteMessages: Boolean, val deleteCredentials: Boolean, val deleteCaches: Boolean) {
    val deletesEverything: Boolean get() = deleteMessages && deleteCredentials && deleteCaches
}
