package com.kheyr.sms.realtime

import com.google.gson.annotations.SerializedName
import com.kheyr.sms.desktop.DesktopRelayPayload
import com.kheyr.sms.sync.RealtimeConnectionState
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import io.reactivex.rxjava3.core.Single

/**
 * Thin wrapper over the SignalR hub at `/hubs/kheyr`. Receives the realtime events the backend pushes
 * to this device — most importantly `DesktopSmsRequest`, which drives the desktop-SMS relay. Kept
 * isolated from the (unit-tested) relay logic so the external client API can change without touching it.
 *
 * The connection authenticates with the account access token; start/stop should be driven from the
 * app lifecycle (see KheyrApplication / MainActivity) on a background thread.
 */
class KheyrRealtimeClient(
    private val baseUrl: String,
    private val accessTokenProvider: () -> String?,
    private val onDesktopSmsRequest: (DesktopRelayPayload) -> Unit,
    private val onDeviceRevoked: () -> Unit = {},
) {
    private var connection: HubConnection? = null

    @Volatile
    var state: RealtimeConnectionState = RealtimeConnectionState.Disconnected
        private set

    @Synchronized
    fun start() {
        if (connection != null) return
        runCatching {
            val hub = HubConnectionBuilder
                .create("$baseUrl/hubs/kheyr")
                .withAccessTokenProvider(Single.defer { Single.just(accessTokenProvider().orEmpty()) })
                .build()
            hub.on("DesktopSmsRequest", { event -> onDesktopSmsRequest(event.toPayload()) }, RelayEvent::class.java)
            hub.on("DeviceRevoked") { onDeviceRevoked() }
            hub.onClosed { _ -> state = RealtimeConnectionState.Disconnected }
            connection = hub
            state = RealtimeConnectionState.Connecting
            hub.start().subscribe(
                { state = RealtimeConnectionState.Connected },
                { _ -> state = RealtimeConnectionState.Disconnected },
            )
        }.onFailure { state = RealtimeConnectionState.Disconnected }
    }

    @Synchronized
    fun stop() {
        connection?.let { hub -> runCatching { hub.stop().subscribe() } }
        connection = null
        state = RealtimeConnectionState.Disconnected
    }

    /** Wire shape of the `DesktopSmsRequest` event (snake_case, deserialized by the client's Gson). */
    private data class RelayEvent(
        @SerializedName("request_id") val requestId: String = "",
        @SerializedName("encrypted_message_body") val encryptedBody: String = "",
        @SerializedName("encrypted_target_number") val encryptedTargetNumber: String = "",
        @SerializedName("sim_id") val simId: String? = null,
        @SerializedName("thread_id") val threadId: Long? = null,
        @SerializedName("client_message_id") val clientMessageId: String? = null,
    ) {
        fun toPayload() = DesktopRelayPayload(
            requestId = requestId,
            encryptedBody = encryptedBody,
            encryptedTargetNumber = encryptedTargetNumber,
            simId = simId,
            threadId = threadId,
            clientMessageId = clientMessageId,
        )
    }
}
