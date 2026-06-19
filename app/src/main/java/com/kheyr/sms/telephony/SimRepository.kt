package com.kheyr.sms.telephony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat

class SimRepository(private val context: Context) {
    // getNumber() is deprecated but, with READ_SMS (held as the default SMS app), it is the
    // authorized own-number source on all API levels including 33+.
    @Suppress("DEPRECATION")
    fun activeSims(): List<SimCard> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }
        val manager = context.getSystemService(SubscriptionManager::class.java) ?: return emptyList()
        return manager.activeSubscriptionInfoList.orEmpty().map { info ->
            SimCard(
                subscriptionId = info.subscriptionId,
                slotIndex = info.simSlotIndex,
                displayName = info.displayName?.toString().orEmpty().ifBlank { "SIM ${info.simSlotIndex + 1}" },
                carrierName = info.carrierName?.toString().orEmpty(),
                // Blank/empty results on some carriers are handled by downstream guards.
                phoneNumber = info.number,
            )
        }.sortedBy { it.slotIndex }
    }
}
