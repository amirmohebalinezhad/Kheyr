package com.kheyr.sms.desktop

import com.kheyr.sms.api.KheyrApiService
import com.kheyr.sms.data.SmsRepository
import com.kheyr.sms.telephony.SmsSendRequest
import com.kheyr.sms.telephony.SmsSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android glue that executes a relayed desktop SMS: resolve -> persist locally -> send via SIM ->
 * report status back to the backend (`POST /api/v1/desktop/sms/status`). Status strings match the
 * backend `DesktopSmsRequestStatus` enum. Reporting "Sent" means the message was handed to the
 * telephony stack; delivery is tracked separately by the normal send-status receiver.
 */
class DesktopRelayExecutor(
    private val resolver: DesktopRelayResolver,
    private val sender: SmsSender,
    private val repository: SmsRepository,
    private val api: KheyrApiService,
) {
    suspend fun handle(payload: DesktopRelayPayload) = withContext(Dispatchers.IO) {
        when (val resolution = resolver.resolve(payload)) {
            is DesktopRelayResolution.Reject ->
                api.updateDesktopSmsStatus(payload.requestId, STATUS_FAILED, resolution.reason)

            is DesktopRelayResolution.Send -> {
                val result = runCatching {
                    val telephonyId = repository.persistOutgoing(resolution.recipient, resolution.body, resolution.subscriptionId)
                    repository.markSending(telephonyId)
                    sender.send(SmsSendRequest(resolution.recipient, resolution.body, resolution.subscriptionId, telephonyId))
                }
                if (result.isSuccess) {
                    api.updateDesktopSmsStatus(payload.requestId, STATUS_SENT)
                } else {
                    api.updateDesktopSmsStatus(payload.requestId, STATUS_FAILED, result.exceptionOrNull()?.message)
                }
            }
        }
    }

    private companion object {
        const val STATUS_SENT = "Sent"
        const val STATUS_FAILED = "Failed"
    }
}
