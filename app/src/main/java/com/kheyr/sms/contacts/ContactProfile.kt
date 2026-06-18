package com.kheyr.sms.contacts

import android.net.Uri

data class ContactProfile(
    val displayName: String?,
    val photoUri: Uri?,
    val contactId: Long?,
)
