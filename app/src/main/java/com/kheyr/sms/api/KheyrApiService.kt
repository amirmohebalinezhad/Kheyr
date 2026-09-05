package com.kheyr.sms.api

import android.util.Log
import com.kheyr.sms.auth.DeviceRegistrationPayload
import com.kheyr.sms.domain.SpamRule
import com.kheyr.sms.domain.SpamRuleSet
import com.kheyr.sms.domain.SpamRuleType
import com.kheyr.sms.preferences.AppPreferences
import com.kheyr.sms.sync.SyncApiClient
import com.kheyr.sms.sync.SyncEventDto
import com.kheyr.sms.sync.SyncUploadDto
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class AuthTokenResponse(val accessToken: String, val refreshToken: String, val expiresInSeconds: Long, val deviceId: String? = null)

data class SyncDownloadResponse(val changes: JSONArray, val nextCursor: String?, val hasMore: Boolean)

data class PairingSessionResponse(val sessionId: String, val qrPayload: String, val expiresAtEpochSeconds: Long)

/** HTTP client for all PRD backend endpoints. Calls fail gracefully when the base URL is still a placeholder. */
class KheyrApiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
    private val tokenProvider: () -> String? = { null },
    private val refreshTokenProvider: () -> String? = { null },
    private val onTokensRefreshed: (AuthTokenResponse) -> Unit = { },
) : com.kheyr.sms.sync.SyncApiClient {
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    fun fetchSpamRules(): SpamRuleSet? = getJson("/api/v1/spam-rules/latest")?.let(::parseSpamRuleSet)

    fun submitSpamFeedback(payload: JSONObject): Boolean = postJson("/api/v1/spam-feedback", payload) != null

    fun requestOtp(phoneE164: String): Boolean = postJson("/api/v1/auth/otp/request", JSONObject().put("phone", phoneE164)) != null

    fun verifyOtp(phoneE164: String, code: String): AuthTokenResponse? =
        postJson("/api/v1/auth/otp/verify", JSONObject().put("phone", phoneE164).put("code", code))?.let(::parseAuthTokens)

    /** A 401 from the refresh endpoint itself means the refresh token is dead, so never retry it (allowRefresh = false). */
    fun refreshToken(refreshToken: String): AuthTokenResponse? =
        postJson("/api/v1/auth/refresh", JSONObject().put("refresh_token", refreshToken), allowRefresh = false)
            ?.let(::parseAuthTokens)

    fun registerDevice(payload: DeviceRegistrationPayload): JSONObject? =
        postJson("/api/v1/devices", JSONObject().apply {
            put("device_name", payload.deviceName)
            put("device_type", payload.deviceType)
            put("platform", payload.platform)
            put("push_token", payload.pushToken)
            put("public_key", payload.publicKey)
        })

    fun uploadInitialSync(deviceId: String, threads: JSONArray, messages: JSONArray): JSONObject? =
        postJson("/api/v1/sync/initial", JSONObject().apply {
            put("device_id", deviceId)
            put("encrypted_threads", threads)
            put("encrypted_messages", messages)
            put("sync_started_at", System.currentTimeMillis())
        })

    fun downloadSyncUpdates(cursor: String?): SyncDownloadResponse? {
        // Cursors are opaque and usually base64, so '+', '=', '&' and '/' must be percent-encoded (B-25).
        val path = if (cursor.isNullOrBlank()) {
            "/api/v1/sync/updates"
        } else {
            "/api/v1/sync/updates?cursor=" + URLEncoder.encode(cursor, Charsets.UTF_8.name())
        }
        val json = getJson(path) ?: return null
        return SyncDownloadResponse(
            changes = json.optJSONArray("changes") ?: JSONArray(),
            nextCursor = json.optString("next_cursor").takeIf { it.isNotBlank() },
            hasMore = json.optBoolean("has_more"),
        )
    }

    fun createPairingSession(): PairingSessionResponse? =
        postJson("/api/v1/pairing/session", JSONObject())?.let { json ->
            PairingSessionResponse(
                sessionId = json.getString("session_id"),
                qrPayload = json.getString("qr_payload"),
                expiresAtEpochSeconds = json.getLong("expires_at"),
            )
        }

    fun approvePairing(sessionId: String, deviceName: String): JSONObject? =
        postJson("/api/v1/pairing/approve", JSONObject().put("session_id", sessionId).put("device_name", deviceName))

    fun revokeDevice(deviceId: String): Boolean = postJson("/api/v1/pairing/revoke", JSONObject().put("device_id", deviceId)) != null

    /** Active (non-revoked) devices for the signed-in account. Empty when unauthenticated/unconfigured. */
    fun listDevices(): List<RemoteDevice> {
        val devices = getJson("/api/v1/devices")?.optJSONArray("devices") ?: return emptyList()
        return buildList {
            for (index in 0 until devices.length()) {
                val obj = devices.getJSONObject(index)
                add(
                    RemoteDevice(
                        id = obj.optString("device_id"),
                        name = obj.optString("device_name"),
                        type = obj.optString("device_type"),
                        platform = obj.optString("platform"),
                        lastActiveAtEpochSeconds = obj.optLong("last_active_at").takeIf { it > 0L },
                    ),
                )
            }
        }
    }

    /** Revokes the current device's refresh tokens server-side. */
    fun logout(): Boolean = postJson("/api/v1/auth/logout", JSONObject()) != null

    /** Soft-deletes the account and its cloud data server-side. */
    fun deleteAccount(): Boolean = deleteJson("/api/v1/account") != null

    /** Android reports the outcome of a relayed desktop SMS. [status] matches the backend enum (e.g. "Sent"/"Failed"). */
    fun updateDesktopSmsStatus(requestId: String, status: String, failureReason: String? = null): Boolean =
        postJson(
            "/api/v1/desktop/sms/status",
            JSONObject().put("request_id", requestId).put("status", status).apply {
                if (failureReason != null) put("failure_reason", failureReason)
            },
        ) != null

    fun sendDesktopSms(payload: JSONObject): JSONObject? = postJson("/api/v1/desktop/sms/send", payload)

    fun sendDirectMessage(payload: JSONObject): JSONObject? = postJson("/api/v1/direct/messages", payload)

    fun deleteCloudData(): Boolean = postJson("/api/v1/privacy/delete", JSONObject()) != null

    fun exportCloudData(): JSONObject? = getJson("/api/v1/privacy/export")

    override fun upload(payloads: List<SyncUploadDto>): Boolean {
        if (payloads.isEmpty()) return true
        val changes = JSONArray()
        payloads.forEach { dto ->
            changes.put(JSONObject().put("queue_id", dto.queueId).put("event", syncEventToJson(dto.event)))
        }
        // postJson is null on any HTTP error, timeout or exception; the caller must keep the rows
        // queued in that case instead of deleting them (B-06).
        return postJson("/api/v1/sync/upload", JSONObject().put("changes", changes)) != null
    }

    private fun syncEventToJson(event: SyncEventDto): JSONObject = when (event) {
        is com.kheyr.sms.sync.EncryptedSmsMessageDto -> JSONObject().apply {
            put("type", "message_created")
            put("message_id", event.messageId)
            put("thread_id", event.threadId)
            // Send the full envelope (algorithm + nonce + ciphertext); the nonce is required to
            // decrypt and was previously dropped, making every synced body undecryptable.
            put("encrypted_body", event.encryptedBody.wireFormat())
            // The DTO carries these and they were all being dropped, so a receiving device had no
            // way to place a message in time or tell an incoming one from an outgoing one - every
            // synced message would render as "incoming, received now". The address is the salted
            // hash, never the raw number (EncryptedFieldPolicy treats it as protected).
            put("hashed_address", event.hashedAddress)
            put("timestamp", event.timestamp.toString())
            put("direction", event.direction.name)
            put("status", event.status.name)
            put("is_spam", event.isSpam)
            event.simSlot?.let { put("sim_slot", it) }
        }
        is com.kheyr.sms.sync.DeleteEventDto -> JSONObject().apply {
            put("type", "message_deleted")
            put("message_id", event.messageId)
        }
        is com.kheyr.sms.sync.SpamStatusDto -> JSONObject().apply {
            put("type", "spam_status_changed")
            put("thread_id", event.threadId)
            put("is_spam", event.isSpam)
        }
        is com.kheyr.sms.sync.PinnedStatusDto -> JSONObject().apply {
            put("type", "pin_changed")
            put("thread_id", event.threadId)
            put("is_pinned", event.isPinned)
        }
        is com.kheyr.sms.sync.ArchiveStatusDto -> JSONObject().apply {
            put("type", "archive_changed")
            put("thread_id", event.threadId)
            put("is_archived", event.isArchived)
        }
        is com.kheyr.sms.sync.NotificationSettingsDto -> JSONObject().apply {
            put("type", "notification_setting_changed")
            put("thread_id", event.threadId)
            put("muted", event.muted)
        }
    }

    private fun getJson(path: String): JSONObject? {
        if (!ApiConfig.isConfigured) return null
        val request = authorized(Request.Builder().url(ApiConfig.endpoint(path)).get().build()) ?: return null
        return executeJson(request)
    }

    private fun postJson(path: String, body: JSONObject, allowRefresh: Boolean = true): JSONObject? {
        if (!ApiConfig.isConfigured) return null
        val request = authorized(
            Request.Builder()
                .url(ApiConfig.endpoint(path))
                .post(body.toString().toRequestBody(jsonMedia))
                .build(),
        ) ?: return null
        return executeJson(request, allowRefresh)
    }

    private fun deleteJson(path: String): JSONObject? {
        if (!ApiConfig.isConfigured) return null
        val request = authorized(Request.Builder().url(ApiConfig.endpoint(path)).delete().build()) ?: return null
        return executeJson(request)
    }

    private fun authorized(request: Request): Request? {
        val token = tokenProvider()
        return if (token.isNullOrBlank()) request else request.newBuilder().header("Authorization", "Bearer $token").build()
    }

    /**
     * [allowRefresh] is false for the replayed request and for the refresh call itself, so a request
     * triggers at most one refresh and a 401 from `/auth/refresh` can never recurse (B-04).
     */
    private fun executeJson(request: Request, allowRefresh: Boolean = true): JSONObject? = runCatching {
        client.newCall(request).execute().use { response ->
            if (response.code == HTTP_UNAUTHORIZED && allowRefresh) return replayAfterRefresh(request)
            if (!response.isSuccessful) return null
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) JSONObject() else JSONObject(body)
        }
    }.getOrNull()

    /** Refreshes the token pair once and replays [original] with the new access token. */
    private fun replayAfterRefresh(original: Request): JSONObject? {
        if (!refreshAccessToken(original.header("Authorization"))) return null
        val token = tokenProvider()
        if (token.isNullOrBlank()) return null
        return executeJson(original.newBuilder().header("Authorization", "Bearer $token").build(), allowRefresh = false)
    }

    /**
     * Serialises concurrent refreshes across every instance in the process (see [REFRESH_LOCK]): a
     * caller that finds the stored access token already changed from the one its request sent skips
     * the refresh and simply replays with the newer token.
     */
    private fun refreshAccessToken(staleAuthorizationHeader: String?): Boolean = synchronized(REFRESH_LOCK) {
        val current = tokenProvider()
        // Another caller refreshed while we waited for the lock; the refresh token is single-use, so
        // do not spend it again — just replay with the token that refresh produced.
        if (!current.isNullOrBlank() && staleAuthorizationHeader != "Bearer $current") return@synchronized true
        val refresh = refreshTokenProvider()
        if (refresh.isNullOrBlank()) return@synchronized false
        val tokens = refreshToken(refresh)
        if (tokens == null) {
            Log.w(TAG, "Access token refresh failed; request stays unauthenticated")
            return@synchronized false
        }
        onTokensRefreshed(tokens)
        true
    }

    private fun parseAuthTokens(json: JSONObject): AuthTokenResponse = AuthTokenResponse(
        accessToken = json.getString("access_token"),
        refreshToken = json.getString("refresh_token"),
        expiresInSeconds = json.optLong("expires_in", 3600),
        deviceId = json.optString("device_id").takeIf { it.isNotBlank() },
    )

    /**
     * Total by construction (B-26): a rule this client cannot understand (new `type`, missing `id`,
     * non-integer `score`) is skipped instead of throwing out of SpamRulesWorker.doWork(). Returns
     * null when the payload is unusable, so a rule set that yields zero rules is rejected, never applied.
     */
    private fun parseSpamRuleSet(json: JSONObject): SpamRuleSet? {
        val version = json.optInt("version", INVALID_VERSION)
        if (version == INVALID_VERSION) {
            Log.w(TAG, "Spam rule set rejected: missing or non-integer version")
            return null
        }
        val rulesArray = json.optJSONArray("rules")
        if (rulesArray == null) {
            Log.w(TAG, "Spam rule set v$version rejected: no rules array")
            return null
        }
        var skipped = 0
        val rules = buildList {
            for (index in 0 until rulesArray.length()) {
                val rule = parseSpamRule(rulesArray.optJSONObject(index))
                if (rule == null) skipped++ else add(rule)
            }
        }
        if (skipped > 0) Log.w(TAG, "Skipped $skipped unsupported spam rule(s) in rule set v$version")
        if (rules.isEmpty()) {
            Log.w(TAG, "Spam rule set v$version rejected: no rule is usable by this client")
            return null
        }
        return SpamRuleSet(version = version, threshold = json.optInt("threshold", 70), rules = rules)
    }

    private fun parseSpamRule(json: JSONObject?): SpamRule? {
        if (json == null) return null
        val id = json.optString("id")
        if (id.isBlank()) return null
        val type = json.optString("type").toRuleTypeOrNull() ?: return null
        if (!json.has("score")) return null
        return runCatching {
            SpamRule(
                id = id,
                type = type,
                pattern = json.optString("pattern").takeIf { it.isNotBlank() },
                score = json.getInt("score"),
                enabled = json.optBoolean("enabled", true),
            )
        }.getOrNull()
    }

    private fun String.toRuleTypeOrNull(): SpamRuleType? {
        if (isBlank()) return null
        val enumName = split('_').joinToString("") { part -> part.replaceFirstChar { it.uppercaseChar() } }
        return SpamRuleType.entries.firstOrNull { it.name == enumName }
    }

    companion object {
        private const val TAG = "KheyrApiService"
        private const val HTTP_UNAUTHORIZED = 401
        private const val INVALID_VERSION = Int.MIN_VALUE

        /**
         * The only sanctioned way to build the service: it wires reading, refreshing and persisting
         * the token pair together so every caller survives the 60-minute access-token lifetime (B-04).
         */
        /**
         * Process-wide, deliberately: the UI and each worker call [create] for themselves and so hold
         * separate [KheyrApiService] instances over the SAME persisted token pair; an instance-local lock
         * would let two of them hit a 401 at once and both spend the single-use refresh token. The
         * second spend is rejected by the backend, and on a server that rotates the whole token
         * family it can log the device out. Serialising here means the loser of the race finds the
         * freshly stored access token in [refreshAccessToken] and simply replays with it.
         */
        private val REFRESH_LOCK = Any()

        fun create(preferences: AppPreferences): KheyrApiService = KheyrApiService(
            tokenProvider = { preferences.authTokens().first },
            refreshTokenProvider = { preferences.authTokens().second },
            onTokensRefreshed = { tokens -> preferences.saveAuthTokens(tokens.accessToken, tokens.refreshToken) },
        )
    }
}
