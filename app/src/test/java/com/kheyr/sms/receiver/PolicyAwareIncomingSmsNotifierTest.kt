package com.kheyr.sms.receiver

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kheyr.sms.contacts.ContactRepository
import com.kheyr.sms.data.AppDatabase
import com.kheyr.sms.preferences.AppPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class PolicyAwareIncomingSmsNotifierTest {
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var notifier: PolicyAwareIncomingSmsNotifier
    private lateinit var notificationManager: NotificationManager

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shadowOf(ApplicationProvider.getApplicationContext<Application>())
            .grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        notifier = PolicyAwareIncomingSmsNotifier(context, AppPreferences(context), database, ContactRepository(context))
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @After fun tearDown() = database.close()

    @Test fun postsHeadsUpNotificationWithFullTextAndReplyMarkReadCopyActions() {
        notifier.show(storedSms(threadId = 1, body = "Your verification code is 123456"), senderIsContact = false)

        val notification = shadowOf(notificationManager).allNotifications.single()

        // Heads-up: high-importance channel + high priority + message category.
        val channel = notificationManager.getNotificationChannel("incoming_sms_messages")
        assertNotNull(channel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
        assertTrue(notification.priority >= Notification.PRIORITY_HIGH)
        assertEquals(Notification.CATEGORY_MESSAGE, notification.category)

        // Full text shown when expanded.
        assertEquals("Your verification code is 123456", notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString())

        // Tapping the notification deep-links into the originating thread.
        val contentIntent = shadowOf(notification.contentIntent).savedIntent
        assertEquals(1L, contentIntent.getLongExtra(com.kheyr.sms.MainActivity.EXTRA_THREAD_ID, -1L))

        // OTP message: Copy code (primary) + Reply + Mark as read.
        assertEquals(listOf("Copy code", "Reply", "Mark as read"), notification.actions.map { it.title.toString() })

        // Reply action carries an inline text box.
        val replyInputs = notification.actions[1].remoteInputs
        assertNotNull(replyInputs)
        assertEquals(NotificationActionReceiver.KEY_REPLY_TEXT, replyInputs!!.single().resultKey)
    }

    @Test fun plainMessageHasReplyAndMarkReadButNoCopyCode() {
        notifier.show(storedSms(threadId = 2, body = "Hey, are we still on for lunch?"), senderIsContact = false)

        val notification = shadowOf(notificationManager).allNotifications.single()
        assertEquals(listOf("Reply", "Mark as read"), notification.actions.map { it.title.toString() })
    }

    private fun storedSms(threadId: Long, body: String) = StoredIncomingSms(
        threadId = threadId,
        sender = "+15551234567",
        body = body,
        receivedAtMillis = 1_700_000_000_000L,
        simSlot = null,
        subscriptionId = null,
    )
}
