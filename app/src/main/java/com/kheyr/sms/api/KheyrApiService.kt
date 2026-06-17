package com.kheyr.sms.api

import com.kheyr.sms.auth.DeviceRegistrationPayload
import com.kheyr.sms.domain.SpamRule
import com.kheyr.sms.domain.SpamRuleSet
import com.kheyr.sms.domain.SpamRuleType
import com.kheyr.sms.sync.SyncApiClient
import com.kheyr.sms.sync.SyncEventDto
import com.kheyr.sms.sync.SyncUploadDto
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AuthTokenResponse(val accessToken: String, val refreshToken: String, val expiresInSeconds: Long)

data class SyncDownloadResponse(val changes: JSONArray, val nextCursor: String?, val hasMore: Boolean)

data class PairingSessionResponse(val sessionId: String, val qrPayload: String, val expiresAtEpochSeconds: Long)

/** HTTP client for all PRD backend endpoints. Calls fail gracefully when the base URL is still a placeholder. */
class KheyrApiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
    private val tokenProvider: () -> String? = { null },
) : com.kheyr.sms.sync.SyncApiClient {
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    fun fetchSpamRules(): SpamRuleSet? = getJson("/api/v1/spam-rules/latest")?.let(::parseSpamRuleSet)

    fun submitSpamFeedback(payload: JSONObject): Boolean = postJson("/api/v1/spam-feedback", payload) != null

    fun requestOtp(phoneE164: String): Boolean = postJson("/api/v1/auth/otp/request", JSONObject().put("phone", phoneE164)) != null

    fun verifyOtp(phoneE164: String, code: String): AuthTokenResponse? =
        postJson("/api/v1/auth/otp/verify", JSONObject().put("phone", phoneE164).put("code", code))?.let(::parseAuthTokens)

    fun refreshToken(refreshToken: String): AuthTokenResponse? =
        postJson("/api/v1/auth/refresh", JSONObject().put("refresh_token", refreshToken))?.let(::parseAuthTokens)

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
        val path = if (cursor.isNullOrBlank()) "/api/v1/sync/updates" else "/api/v1/sync/updates?cursor=$cursor"
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

    fun sendDesktopSms(payload: JSONObject): JSONObject? = postJson("/api/v1/desktop/sms/send", payload)

    fun sendDirectMessage(payload: JSONObject): JSONObject? = postJson("/api/v1/direct/messages", payload)

    fun deleteCloudData(): Boolean = postJson("/api/v1/privacy/delete", JSONObject()) != null

    fun exportCloudData(): JSONObject? = getJson("/api/v1/privacy/export")

    override fun upload(payloads: List<SyncUploadDto>) {
        if (payloads.isEmpty()) return
        val changes = JSONArray()
        payloads.forEach { dto ->
            changes.put(JSONObject().put("queue_id", dto.queueId).put("event", syncEventToJson(dto.event)))
        }
        postJson("/api/v1/sync/upload", JSONObject().put("changes", changes))
    }

    private fun syncEventToJson(event: SyncEventDto): JSONObject = when (event) {
        is com.kheyr.sms.sync.EncryptedSmsMessageDto -> JSONObject().apply {
            put("type", "message_created")
            put("message_id", event.messageId)
            put("thread_id", event.threadId)
            put("encrypted_body", event.encryptedBody.ciphertextBase64)
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

    private fun postJson(path: String, body: JSONObject): JSONObject? {
        if (!ApiConfig.isConfigured) return null
        val request = authorized(
            Request.Builder()
                .url(ApiConfig.endpoint(path))
                .post(body.toString().toRequestBody(jsonMedia))
                .build(),
        ) ?: return null
        return executeJson(request)
    }

    private fun authorized(request: Request): Request? {
        val token = tokenProvider()
        return if (token.isNullOrBlank()) request else request.newBuilder().header("Authorization", "Bearer $token").build()
    }

    private fun executeJson(request: Request): JSONObject? = runCatching {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) JSONObject() else JSONObject(body)
        }
    }.getOrNull()

    private fun parseAuthTokens(json: JSONObject): AuthTokenResponse = AuthTokenResponse(
        accessToken = json.getString("access_token"),
        refreshToken = json.getString("refresh_token"),
        expiresInSeconds = json.optLong("expires_in", 3600),
    )

    private fun parseSpamRuleSet(json: JSONObject): SpamRuleSet {
        val rulesArray = json.getJSONArray("rules")
        val rules = buildList {
            for (index in 0 until rulesArray.length()) {
                val ruleJson = rulesArray.getJSONObject(index)
                add(
                    SpamRule(
                        id = ruleJson.getString("id"),
                        type = ruleJson.getString("type").toRuleType(),
                        pattern = ruleJson.optString("pattern").takeIf { it.isNotBlank() },
                        score = ruleJson.getInt("score"),
                        enabled = ruleJson.optBoolean("enabled", true),
                    ),
                )
            }
        }
        return SpamRuleSet(version = json.getInt("version"), threshold = json.optInt("threshold", 70), rules = rules)
    }

    private fun String.toRuleType(): SpamRuleType {
        val enumName = split('_').joinToString("") { part -> part.replaceFirstChar { it.uppercaseChar() } }
        return SpamRuleType.valueOf(enumName)
    }
}
