package com.kheyr.sms.telephony

import android.content.Context
import android.telephony.SubscriptionManager
import com.kheyr.sms.contacts.PhoneNumberNormalizer

class OwnNumberResolver(private val context: Context) {
    fun isOwnNumber(number: String): Boolean {
        if (number.isBlank()) return false
        val simRepository = SimRepository(context)
        simRepository.activeSims().forEach { sim ->
            sim.phoneNumber?.let { own ->
                if (PhoneNumberNormalizer.matches(own, number)) return true
            }
        }
        val manager = context.getSystemService(SubscriptionManager::class.java) ?: return false
        val activeSubscriptions = try {
            manager.activeSubscriptionInfoList
        } catch (_: SecurityException) {
            // READ_PHONE_STATE / READ_PHONE_NUMBERS may be missing; treat as no match instead of crashing.
            null
        }
        return activeSubscriptions.orEmpty().any { info ->
            // READ_SMS (held as the default SMS app) authorizes getNumber() on all API levels.
            @Suppress("DEPRECATION")
            val own = info.number
            !own.isNullOrBlank() && PhoneNumberNormalizer.matches(own, number)
        }
    }
}
