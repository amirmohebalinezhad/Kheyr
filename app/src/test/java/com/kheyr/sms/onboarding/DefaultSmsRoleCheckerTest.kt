package com.kheyr.sms.onboarding

import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowTelephony

@RunWith(RobolectricTestRunner::class)
class DefaultSmsRoleCheckerTest {
    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun isDefaultWhenTelephonyReportsMatchingPackage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ShadowTelephony.ShadowSms.setDefaultSmsPackage(context.packageName)

        assertTrue(DefaultSmsRoleChecker.isDefaultSmsApp(context))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun isNotDefaultWhenTelephonyReportsDifferentPackage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ShadowTelephony.ShadowSms.setDefaultSmsPackage("com.other.sms")

        assertFalse(DefaultSmsRoleChecker.isDefaultSmsApp(context))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun isDefaultWhenRoleHeldOnApi29Plus() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val roleManager = context.getSystemService(RoleManager::class.java)
        Shadows.shadowOf(roleManager).addHeldRole(RoleManager.ROLE_SMS)

        assertTrue(DefaultSmsRoleChecker.isDefaultSmsApp(context))
    }
}
