package com.kheyr.sms.onboarding

import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import android.provider.Telephony

object DefaultSmsRoleChecker {
    fun isDefaultSmsApp(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager?.isRoleHeld(RoleManager.ROLE_SMS) == true) return true
        }
        return Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
    }
}
