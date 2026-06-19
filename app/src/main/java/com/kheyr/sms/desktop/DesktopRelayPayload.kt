package com.kheyr.sms.desktop

/**
 * The `DesktopSmsRequest` realtime event the backend pushes to this Android device when a paired
 * desktop asks the phone to send an SMS. Body and target number arrive encrypted with the shared
 * sync content key (the phone decrypts them locally before sending).
 */
data class DesktopRelayPayload(
    val requestId: String,
    val encryptedBody: String,
    val encryptedTargetNumber: String,
    val simId: String?,
    val threadId: Long?,
    val clientMessageId: String?,
)
