package com.kheyr.sms.settings

import com.kheyr.sms.domain.SpamClassification

/** Resolves final notification behavior after spam and blocked-sender checks. */
class NotificationPolicyResolver {
    fun resolve(input: NotificationPolicyInput): NotificationPolicyDecision {
        if (input.spamClassification == SpamClassification.Spam || input.senderBlocked) {
            return NotificationPolicyDecision.Suppress
        }

        if (!input.senderIsContact && input.globalSettings.unknownSenderMode == UnknownSenderNotificationMode.None) {
            return NotificationPolicyDecision.Suppress
        }

        val soundMode = when {
            input.threadSettings?.muted == true -> NotificationSoundMode.Silent
            !input.senderIsContact && input.globalSettings.unknownSenderMode == UnknownSenderNotificationMode.Silent -> NotificationSoundMode.Silent
            input.threadSettings?.customRingtoneUri != null -> NotificationSoundMode.Custom(input.threadSettings.customRingtoneUri)
            input.globalSettings.globalRingtoneUri != null -> NotificationSoundMode.Custom(input.globalSettings.globalRingtoneUri)
            else -> NotificationSoundMode.Default
        }

        return NotificationPolicyDecision.Post(
            title = title(input),
            body = body(input),
            soundMode = soundMode,
            vibrate = input.globalSettings.vibrate && soundMode != NotificationSoundMode.Silent,
        )
    }

    private fun title(input: NotificationPolicyInput): String = when (input.globalSettings.contentMode) {
        NotificationContentMode.HideSenderAndPreview -> HIDDEN_TITLE
        else -> input.displayName.ifBlank { input.sender }
    }

    private fun body(input: NotificationPolicyInput): String? = when (input.globalSettings.contentMode) {
        NotificationContentMode.ShowSenderAndPreview -> input.preview
        NotificationContentMode.ShowSenderOnly -> null
        NotificationContentMode.HideSenderAndPreview -> HIDDEN_BODY
    }

    private companion object {
        const val HIDDEN_TITLE = "New message"
        const val HIDDEN_BODY = "Open Kheyr to read it"
    }
}

data class NotificationPolicyInput(
    val sender: String,
    val displayName: String,
    val preview: String,
    val senderIsContact: Boolean,
    val senderBlocked: Boolean = false,
    val spamClassification: SpamClassification = SpamClassification.Normal,
    val globalSettings: NotificationSettings = NotificationSettings(),
    val threadSettings: ThreadNotificationSettings? = null,
)

sealed interface NotificationPolicyDecision {
    data object Suppress : NotificationPolicyDecision

    data class Post(
        val title: String,
        val body: String?,
        val soundMode: NotificationSoundMode,
        val vibrate: Boolean,
    ) : NotificationPolicyDecision
}

sealed interface NotificationSoundMode {
    data object Default : NotificationSoundMode
    data object Silent : NotificationSoundMode
    data class Custom(val ringtoneUri: String) : NotificationSoundMode
}
