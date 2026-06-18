package com.kheyr.sms.telephony

import android.content.Context
import android.os.Build
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
        return manager.activeSubscriptionInfoList.orEmpty().any { info ->
            val own = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                info.number
            } else {
                @Suppress("DEPRECATION")
                info.number
            }
            !own.isNullOrBlank() && PhoneNumberNormalizer.matches(own, number)
        }
    }
}
