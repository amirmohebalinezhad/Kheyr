package com.kheyr.sms.messaging

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kheyr.sms.KheyrApplication

/**
 * Receives FCM pushes used to wake the app for desktop-SMS relay and background sync.
 *
 * Currently INERT end-to-end: it needs a Firebase `google-services.json` + the `google-services`
 * Gradle plugin to receive anything, and the backend must actually send pushes (it stores the token
 * but has no sender yet — backend hardening, out of scope). Until then this captures the token so it
 * is ready to register, and the realtime SignalR connection (while the app is foregrounded) carries
 * relay requests.
 */
class KheyrFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        // Persist locally. Sending it to the backend needs a device-push-token update endpoint
        // (out of scope); registering again here would create duplicate device rows.
        (application as? KheyrApplication)?.preferences?.pushToken = token
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // A data push simply ensures the process is alive; the UI lifecycle reconnects the realtime
        // client, which then drains any queued desktop-SMS relay requests. No work needed here yet.
    }
}
