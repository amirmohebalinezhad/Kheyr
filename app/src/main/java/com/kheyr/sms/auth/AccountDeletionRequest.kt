package com.kheyr.sms.auth

data class AccountDeletionRequest(val confirmed: Boolean, val wipeLocalData: Boolean) {
    val canProceed: Boolean get() = confirmed
}
