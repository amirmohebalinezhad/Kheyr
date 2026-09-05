package com.kheyr.sms.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kheyr.sms.api.KheyrApiService
import com.kheyr.sms.data.AppDatabase
import com.kheyr.sms.data.SmsRepository
import com.kheyr.sms.preferences.AppPreferences
import com.kheyr.sms.receiver.DefaultSpamRuleSet
import com.kheyr.sms.reliability.BackgroundSyncScheduler
import com.kheyr.sms.spam.SpamRuleDownloader
import com.kheyr.sms.sync.RoomSyncQueueStore
import com.kheyr.sms.sync.SyncDownloader
import com.kheyr.sms.sync.SyncEncryptionKeyStore
import com.kheyr.sms.sync.SyncUploader
import com.kheyr.sms.sync.crypto.SmsBodyEncryptor
import java.time.Instant
import javax.crypto.SecretKey

class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val preferences = AppPreferences(applicationContext)
        val syncSettings = preferences.syncSettings()
        if (!BackgroundSyncScheduler.shouldSchedule(syncSettings.enabled)) return Result.success()

        val api = KheyrApiService.create(preferences)
        // Queue the existing history once, before draining, so the first upload-capable run carries it
        // (B-03). The flag is only set after the backfill actually completed, so a failure retries.
        if (syncSettings.canUpload && !preferences.initialBackfillDone) {
            val queued = runCatching { SmsRepository(applicationContext).enqueueInitialBackfill() }
                .getOrElse { return Result.retry() }
            preferences.initialBackfillDone = true
            if (queued > 0) Log.i(TAG, "Queued $queued message(s) for the initial sync backfill")
        }
        val queueStore = RoomSyncQueueStore(AppDatabase.getInstance(applicationContext).syncQueueDao())
        val encryptionKey: SecretKey = runCatching { SyncEncryptionKeyStore(applicationContext).getOrCreateKey() }
            .getOrElse { return Result.retry() }
        val encryptor = SmsBodyEncryptor(encryptionKey)
        val uploader = SyncUploader({ syncSettings }, queueStore, api, encryptor)
        val uploaded = uploader.uploadPending()
        // Only stamp on a CONFIRMED upload. uploadPending returns 0 both when the upload failed and
        // when there was nothing queued; in neither case has anything newly succeeded, and stamping
        // on a failed run would show the user a fresh "last synced" time while their queue backs up.
        if (syncSettings.canUpload && uploaded > 0) {
            preferences.saveSyncSettings(syncSettings.copy(lastSuccessfulUploadAt = Instant.now()))
        }

        val cursor = preferences.syncCursor()
        api.downloadSyncUpdates(cursor)?.let { response ->
            val result = SyncDownloader().parse(cursor, response.changes.length(), response.nextCursor, response.hasMore)
            preferences.saveSyncCursor(result.nextCursor)
        }
        return Result.success()
    }

    private companion object {
        const val TAG = "SyncWorker"
    }
}

/**
 * Enforces the Spam-protection screen's "auto-delete spam after N days" slider, which was persisted
 * but never acted on (B-18). Scheduled daily; a no-op while the slider sits at 0 ("never").
 */
class SpamCleanupWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val retentionDays = AppPreferences(applicationContext).spamAutoDeleteDays
        if (retentionDays <= 0) return Result.success()
        val deleted = runCatching { SmsRepository(applicationContext).deleteSpamOlderThan(retentionDays) }
            .getOrElse { return Result.retry() }
        Log.i(TAG, "Auto-deleted $deleted spam message(s) older than $retentionDays day(s)")
        return Result.success()
    }

    private companion object {
        const val TAG = "SpamCleanupWorker"
    }
}

class SpamRulesWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val preferences = AppPreferences(applicationContext)
        val api = KheyrApiService.create(preferences)
        val downloaded = api.fetchSpamRules() ?: return Result.success()
        val current = preferences.loadSpamRuleSet(DefaultSpamRuleSet.rules)
        val result = SpamRuleDownloader().validate(current, downloaded)
        if (result.accepted && result.ruleSet != null) preferences.saveSpamRuleSet(result.ruleSet)
        return Result.success()
    }
}
