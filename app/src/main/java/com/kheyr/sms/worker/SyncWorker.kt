package com.kheyr.sms.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kheyr.sms.api.KheyrApiService
import com.kheyr.sms.data.AppDatabase
import com.kheyr.sms.preferences.AppPreferences
import com.kheyr.sms.receiver.DefaultSpamRuleSet
import com.kheyr.sms.reliability.BackgroundSyncScheduler
import com.kheyr.sms.spam.SpamRuleDownloader
import com.kheyr.sms.sync.RoomSyncQueueStore
import com.kheyr.sms.sync.SyncDownloader
import com.kheyr.sms.sync.SyncUploader
import com.kheyr.sms.sync.crypto.SmsBodyEncryptor
import java.time.Instant
import javax.crypto.spec.SecretKeySpec

class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val preferences = AppPreferences(applicationContext)
        val syncSettings = preferences.syncSettings()
        if (!BackgroundSyncScheduler.shouldSchedule(syncSettings.enabled)) return Result.success()

        val api = KheyrApiService(tokenProvider = { preferences.authTokens().first })
        val queueStore = RoomSyncQueueStore(AppDatabase.getInstance(applicationContext).syncQueueDao())
        val encryptor = SmsBodyEncryptor(SecretKeySpec(ByteArray(32) { 1 }, "AES"))
        val uploader = SyncUploader({ syncSettings }, queueStore, api, encryptor)
        uploader.uploadPending()
        // Only record success for upload-capable runs. When sync is enabled but the device is not yet
        // paired, canUpload is false and uploadPending() is a no-op, so recording would make a skipped
        // run look successful and mask the pending setup state.
        if (syncSettings.canUpload) {
            preferences.saveSyncSettings(syncSettings.copy(lastSuccessfulUploadAt = Instant.now()))
        }

        val cursor = preferences.syncCursor()
        api.downloadSyncUpdates(cursor)?.let { response ->
            val result = SyncDownloader().parse(cursor, response.changes.length(), response.nextCursor, response.hasMore)
            preferences.saveSyncCursor(result.nextCursor)
        }
        return Result.success()
    }
}

class SpamRulesWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val preferences = AppPreferences(applicationContext)
        val api = KheyrApiService(tokenProvider = { preferences.authTokens().first })
        val downloaded = api.fetchSpamRules() ?: return Result.success()
        val current = preferences.loadSpamRuleSet(DefaultSpamRuleSet.rules)
        val result = SpamRuleDownloader().validate(current, downloaded)
        if (result.accepted && result.ruleSet != null) preferences.saveSpamRuleSet(result.ruleSet)
        return Result.success()
    }
}
