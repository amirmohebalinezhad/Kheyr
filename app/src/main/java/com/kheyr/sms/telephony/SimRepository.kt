package com.kheyr.sms.telephony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat

class SimRepository(private val context: Context) {
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
                phoneNumber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) null else info.number,
            )
        }.sortedBy { it.slotIndex }
    }
}
