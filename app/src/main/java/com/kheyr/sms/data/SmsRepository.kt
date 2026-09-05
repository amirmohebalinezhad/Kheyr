package com.kheyr.sms.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import com.kheyr.sms.preferences.AppPreferences
import com.kheyr.sms.sync.RoomSyncQueueStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant
import java.time.temporal.ChronoUnit

class SmsRepository(
    private val context: Context,
    private val smsDao: SmsDao = AppDatabase.getInstance(context).smsDao(),
    injectedSyncQueueStore: RoomSyncQueueStore? = null,
) {
    // Every message write and every thread-state change funnels through this class, so this is where
    // sync_queue gets fed (B-03). Built lazily so a caller that injects only [smsDao] never forces the
    // encrypted store open, and so a sync-disabled install never touches the queue at all.
    private val syncQueueStore: RoomSyncQueueStore by lazy {
        injectedSyncQueueStore ?: RoomSyncQueueStore(AppDatabase.getInstance(context).syncQueueDao())
    }

    private val preferences: AppPreferences by lazy { AppPreferences(context) }

    /**
     * Telephony ids the user deleted. Android silently ignores a provider delete from a non-default
     * SMS app, so the system rows can survive the delete and the gap-window backfill in
     * [findMissingTelephonyIds] would re-import the conversation seconds later (B-13). Stored in this
     * class's own preferences file (never [AppPreferences]) as a bounded, newest-last id list.
     */
    private val deletedTelephonyIdPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(DELETED_TELEPHONY_IDS_PREFS, Context.MODE_PRIVATE)
    }

    suspend fun loadLocalThreads(): List<SmsThread> = withContext(Dispatchers.IO) {
        smsDao.inboxThreads().map { it.toModel() }
    }

    suspend fun syncTelephonyMessages() = withContext(Dispatchers.IO) {
        syncNewTelephonyMessages()
        refreshRecentOutgoingMessages()
    }

    /**
     * Imports exactly these provider rows. Callers use this right after writing a message to the
     * provider themselves (send, retry, notification reply), so it is a positive assertion that the
     * rows are live: any tombstone on those ids is stale and is dropped rather than honoured.
     */
    suspend fun syncTelephonyMessagesByIds(telephonyIds: List<Long>) = withContext(Dispatchers.IO) {
        if (telephonyIds.isEmpty()) return@withContext
        forgetDeletedTelephonyIds(telephonyIds)
        syncTelephonyMessages(telephonyIds = telephonyIds)
    }

    suspend fun loadPinnedThreads(): List<SmsThread> = withContext(Dispatchers.IO) { smsDao.pinnedThreads().map { it.toModel() } }

    suspend fun loadSpamThreads(): List<SmsThread> = withContext(Dispatchers.IO) { smsDao.spamThreads().map { it.toModel() } }

    suspend fun loadArchivedThreads(): List<SmsThread> = withContext(Dispatchers.IO) { smsDao.archivedThreads().map { it.toModel() } }

    fun insertIncomingSms(
        threadId: Long,
        address: String,
        body: String,
        timestamp: Instant = Instant.now(),
        telephonyId: Long? = null,
        read: Boolean = false,
        simSlot: Int? = null,
    ): Long {
        val message = SmsMessageEntity(
            telephonyId = telephonyId,
            threadId = threadId,
            address = address,
            body = body,
            timestamp = timestamp,
            direction = MessageDirection.Incoming,
            status = MessageStatus.Received,
            read = read,
            simSlot = simSlot,
        )
        val id = smsDao.insertIncomingSms(message)
        enqueueMessageChange(message.copy(id = id))
        return id
    }

    fun insertOutgoingSms(
        threadId: Long,
        address: String,
        body: String,
        timestamp: Instant = Instant.now(),
        status: MessageStatus = MessageStatus.Sending,
        simSlot: Int? = null,
    ): Long {
        val message = SmsMessageEntity(
            threadId = threadId,
            address = address,
            body = body,
            timestamp = timestamp,
            direction = MessageDirection.Outgoing,
            status = status,
            read = true,
            simSlot = simSlot,
        )
        val id = smsDao.insertOutgoingSms(message)
        enqueueMessageChange(message.copy(id = id))
        return id
    }

    suspend fun updatePinned(threadId: Long, pinned: Boolean, pinnedAt: Instant? = if (pinned) Instant.now() else null) =
        withContext(Dispatchers.IO) {
            smsDao.updatePinned(threadId, pinned, pinnedAt)
            enqueueForSync { store ->
                store.enqueueThreadState(
                    threadId,
                    "pin",
                    JSONObject()
                        .put("is_pinned", pinned)
                        // decode() reads this back with optString/takeIf, so "absent" is the empty string.
                        .put("pinned_at", pinnedAt?.toString().orEmpty()),
                )
            }
        }

    suspend fun updateArchived(threadId: Long, archived: Boolean) =
        withContext(Dispatchers.IO) {
            smsDao.updateArchived(threadId, archived)
            enqueueForSync { store ->
                store.enqueueThreadState(threadId, "archive", JSONObject().put("is_archived", archived))
            }
        }

    suspend fun updateSpam(threadId: Long, spam: Boolean) =
        withContext(Dispatchers.IO) {
            smsDao.updateSpam(threadId, spam)
            enqueueForSync { store ->
                store.enqueueThreadState(threadId, "spam", JSONObject().put("is_spam", spam))
            }
        }

    /**
     * Automatic (classifier-driven) spam flagging for the receive pipeline. Unlike [updateSpam] this
     * cannot stomp an explicit user "Not spam" correction (B-23), so the sync event is only queued
     * when the flag actually took.
     */
    /**
     * Flags [threadId] as spam unless the user has already corrected it with "Not spam".
     *
     * Returns whether the thread is spam afterwards, so the receive pipeline can tell "hidden as
     * spam" from "the user's correction stands" and still notify in the second case.
     */
    fun autoMarkSpam(threadId: Long): Boolean {
        val wasSpam = smsDao.isThreadSpam(threadId) == true
        if (!smsDao.autoMarkSpam(threadId)) return false
        // Only on the transition: re-enqueueing an identical event for every message on an
        // already-flagged spam thread would grow sync_queue without adding information.
        if (!wasSpam) {
            enqueueForSync { store ->
                store.enqueueThreadState(threadId, "spam", JSONObject().put("is_spam", true))
            }
        }
        return true
    }

    suspend fun updateMuted(threadId: Long, muted: Boolean) =
        withContext(Dispatchers.IO) {
            smsDao.updateMuted(threadId, muted)
            enqueueForSync { store ->
                store.enqueueThreadState(threadId, "notification", JSONObject().put("muted", muted))
            }
        }

    suspend fun markThreadRead(threadId: Long): Unit =
        withContext(Dispatchers.IO) {
            smsDao.markThreadRead(threadId)
            val values = ContentValues().apply { put(Telephony.Sms.READ, 1) }
            try {
                context.contentResolver.update(
                    Telephony.Sms.CONTENT_URI,
                    values,
                    "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0",
                    arrayOf(threadId.toString()),
                )
            } catch (e: SecurityException) {
                // Writing to the provider is refused once the app is no longer the default SMS
                // handler; the local read state is already committed, so this must not throw (B-22).
                Log.w(TAG, "Unable to mark the thread read in the SMS provider", e)
            }
        }

    fun updateSendStatus(messageId: Long, status: MessageStatus) {
        smsDao.updateSendStatus(messageId, status)
        enqueueMessageChangeById(messageId)
    }

    private fun syncNewTelephonyMessages() {
        val lastSyncedId = smsDao.latestSyncedTelephonyId()
        syncTelephonyMessages(newerThanId = lastSyncedId)
        // Also check for any missing messages in a recent window to handle out-of-order inserts/backups.
        if (lastSyncedId > 0) {
            val windowStart = (lastSyncedId - SYNC_ID_GAP_WINDOW).coerceAtLeast(0)
            val locallyPresent = smsDao.syncedTelephonyIdsInRange(windowStart, lastSyncedId).toSet()
            val missing = findMissingTelephonyIds(windowStart, lastSyncedId, locallyPresent)
            if (missing.isNotEmpty()) {
                syncTelephonyMessages(telephonyIds = missing)
            }
        }
    }

    private fun findMissingTelephonyIds(start: Long, end: Long, locallyPresent: Set<Long>): List<Long> {
        val projection = arrayOf(Telephony.Sms._ID)
        val cursor = try {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                "${Telephony.Sms._ID} >= ? AND ${Telephony.Sms._ID} <= ?",
                arrayOf(start.toString(), end.toString()),
                null,
            )
        } catch (_: SecurityException) {
            // Same withdrawal of READ_SMS that syncTelephonyMessages guards against: this backfill
            // runs a few lines later and must not throw out of the sync (B-22).
            return emptyList()
        }
        // Rows the user deleted must not come back even when the provider still holds them (B-13).
        val tombstoned = deletedTelephonyIds()
        return cursor?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            buildList {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    if (id !in locallyPresent && id !in tombstoned) add(id)
                }
            }
        }.orEmpty()
    }

    private fun refreshRecentOutgoingMessages() {
        smsDao.recentOutgoingTelephonyIds(RECENT_OUTGOING_REFRESH_LIMIT)
            .chunked(RECENT_OUTGOING_REFRESH_BATCH_SIZE)
            .forEach { ids -> syncTelephonyMessages(telephonyIds = ids) }
    }

    private fun syncTelephonyMessages(newerThanId: Long? = null, telephonyIds: List<Long>? = null) {
        val selection: String
        val selectionArgs: Array<String>
        when {
            telephonyIds != null -> {
                if (telephonyIds.isEmpty()) return
                val placeholders = telephonyIds.joinToString(",") { "?" }
                selection = "${Telephony.Sms._ID} IN ($placeholders)"
                selectionArgs = telephonyIds.map(Long::toString).toTypedArray()
            }
            newerThanId != null -> {
                selection = "${Telephony.Sms._ID} > ?"
                selectionArgs = arrayOf(newerThanId.toString())
            }
            else -> return
        }
        // Drafts left behind by the previous SMS app are not messages the user sent; importing them
        // renders them as sent bubbles, so they are excluded at the query instead (B-29).
        val importSelection = "($selection) AND ${TelephonyDirectionMapper.IMPORTABLE_TYPE_SELECTION}"
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.READ,
            Telephony.Sms.TYPE,
            Telephony.Sms.STATUS,
            SUBSCRIPTION_ID,
        )
        val cursor = try {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                importSelection,
                selectionArgs,
                "${Telephony.Sms._ID} ASC",
            )
        } catch (_: SecurityException) {
            // READ_SMS can be withdrawn when the app is no longer the default SMS handler. Keep the
            // already-synced local data instead of crashing the caller.
            return
        }
        cursor?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val thread = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val address = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val body = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val date = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val read = cursor.getColumnIndexOrThrow(Telephony.Sms.READ)
            val subId = cursor.getColumnIndex(SUBSCRIPTION_ID)
            val type = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            val statusColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.STATUS)
            val messages = mutableListOf<SmsMessageEntity>()
            // Rows the user deleted must never come back. Filtering only in findMissingTelephonyIds is
            // not enough: deleting the newest thread lowers latestSyncedTelephonyId, so the plain
            // "newer than" query reaches those rows again on the very next refresh (B-13).
            //
            // Only the discovery paths are filtered. An explicit id list means the caller just wrote
            // that row and wants it imported; the provider reuses rowids, so honouring a tombstone
            // there would silently drop a message the user actually sent.
            val tombstoned = if (telephonyIds == null) deletedTelephonyIds() else emptySet()
            while (cursor.moveToNext()) {
                val telephonyId = cursor.getLong(id)
                if (telephonyId in tombstoned) continue
                val messageType = cursor.getInt(type)
                // The query already excludes drafts; this keeps a provider that ignores the selection
                // from turning one into a sent bubble anyway (B-29).
                if (!TelephonyDirectionMapper.isImportableType(messageType)) continue
                val direction = TelephonyDirectionMapper.directionFromType(messageType)
                val status = if (direction == MessageDirection.Outgoing) {
                    messageStatus(messageType, cursor.getInt(statusColumn))
                } else {
                    MessageStatus.Received
                }
                messages += SmsMessageEntity(
                    telephonyId = telephonyId,
                    threadId = cursor.getLong(thread),
                    address = cursor.getString(address).orEmpty(),
                    simSlot = cursor.intOrNull(subId),
                    body = cursor.getString(body).orEmpty(),
                    timestamp = Instant.ofEpochMilli(cursor.getLong(date)),
                    direction = direction,
                    status = status,
                    read = cursor.getInt(read) != 0 || direction == MessageDirection.Outgoing,
                )
                if (messages.size == SYNC_INSERT_BATCH_SIZE) {
                    smsDao.upsertTelephonyMessageBatch(messages)
                    messages.clear()
                }
            }
            if (messages.isNotEmpty()) {
                smsDao.upsertTelephonyMessageBatch(messages)
            }
        }
    }

    suspend fun recentOutgoingThreadId(address: String, body: String, withinSeconds: Long = 120): Long? = withContext(Dispatchers.IO) {
        smsDao.recentOutgoingThreadId(address, body, Instant.now().minusSeconds(withinSeconds))
    }

    suspend fun deleteThreadMessages(threadId: Long) =
        withContext(Dispatchers.IO) {
            val deleted = if (syncEnabled()) smsDao.messagesForThread(threadId) else emptyList()
            // Read the provider ids BEFORE the delete: the delete itself is silently ignored when the
            // app is not the default SMS handler, and these ids are the tombstones that keep the
            // backfill from re-importing the thread on the next refresh (B-13).
            val telephonyIds = telephonyIdsForThread(threadId)
            smsDao.deleteThreadMessages(threadId)
            enqueueDeletes(deleted)
            try {
                context.contentResolver.delete(
                    Telephony.Sms.CONTENT_URI,
                    "${Telephony.Sms.THREAD_ID} = ?",
                    arrayOf(threadId.toString()),
                )
            } catch (e: SecurityException) {
                Log.w(TAG, "Unable to delete thread from the SMS provider", e)
            }
            recordDeletedTelephonyIds(telephonyIds)
        }

    /** Deletes from Room and from the system SMS store. See [deleteLocalMessagesByIds] for the split halves. */
    suspend fun deleteMessagesByIds(ids: List<Long>) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        val deleted = smsDao.messagesByIds(ids)
        smsDao.deleteMessagesByIds(ids)
        enqueueDeletes(deleted)
        deleteFromTelephony(deleted.mapNotNull { it.telephonyId })
    }

    /**
     * Room only, no provider write. Undo inside the snackbar window can therefore still restore
     * messages that have not left the system SMS store yet (B-14); [deleteTelephonyMessages] removes
     * them from the system store once the delete is committed.
     */
    suspend fun deleteLocalMessagesByIds(ids: List<Long>) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        val deleted = smsDao.messagesByIds(ids)
        smsDao.deleteMessagesByIds(ids)
        // Tombstone immediately, even though the provider rows survive until the undo window closes.
        // Without this the window is precisely the B-13 condition - gone from Room, still in the
        // provider - so a refresh during the snackbar, or simply the next launch if the process dies
        // before commit, re-imports the thread the user just deleted.
        recordDeletedTelephonyIds(deleted.mapNotNull { it.telephonyId })
        enqueueDeletes(deleted)
    }

    /** The system-SMS-store half of [deleteLocalMessagesByIds] (B-14). Records tombstones (B-13). */
    suspend fun deleteTelephonyMessages(telephonyIds: List<Long>) = withContext(Dispatchers.IO) {
        deleteFromTelephony(telephonyIds)
    }

    /**
     * Deletes spam messages older than [retentionDays] days, the "auto-delete spam after N days"
     * setting (B-18). Returns the number of messages deleted; 0 when [retentionDays] is 0 or less,
     * which the settings slider uses to mean "never".
     */
    suspend fun deleteSpamOlderThan(retentionDays: Int): Int = withContext(Dispatchers.IO) {
        if (retentionDays <= 0) return@withContext 0
        val cutoff = Instant.now().minus(retentionDays.toLong(), ChronoUnit.DAYS)
        val ids = smsDao.spamMessageIdsOlderThan(cutoff)
        if (ids.isEmpty()) return@withContext 0
        deleteMessagesByIds(ids)
        ids.size
    }

    private fun telephonyIdsForThread(threadId: Long): List<Long> {
        val cursor = try {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms._ID),
                "${Telephony.Sms.THREAD_ID} = ?",
                arrayOf(threadId.toString()),
                null,
            )
        } catch (_: SecurityException) {
            return emptyList()
        }
        return cursor?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            buildList {
                while (cursor.moveToNext()) add(cursor.getLong(idCol))
            }
        }.orEmpty()
    }

    private fun deleteFromTelephony(telephonyIds: List<Long>) {
        if (telephonyIds.isEmpty()) return
        val placeholders = telephonyIds.joinToString(",") { "?" }
        try {
            context.contentResolver.delete(
                Telephony.Sms.CONTENT_URI,
                "${Telephony.Sms._ID} IN ($placeholders)",
                telephonyIds.map(Long::toString).toTypedArray(),
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "Unable to delete messages from the SMS provider", e)
        }
        // Recorded whether or not the provider honoured the delete: a non-default SMS app's write is
        // ignored without any error, and the tombstones are what keep the rows deleted (B-13).
        recordDeletedTelephonyIds(telephonyIds)
    }

    private fun deletedTelephonyIds(): Set<Long> = readDeletedTelephonyIds().toSet()

    private fun readDeletedTelephonyIds(): List<Long> =
        deletedTelephonyIdPrefs.getString(KEY_DELETED_TELEPHONY_IDS, null)
            ?.splitToSequence(',')
            ?.mapNotNull(String::toLongOrNull)
            ?.toList()
            .orEmpty()

    private fun recordDeletedTelephonyIds(telephonyIds: List<Long>) {
        if (telephonyIds.isEmpty()) return
        // Read-modify-write, and this runs from the UI coroutine, the bulk-delete loop and the
        // notification-action thread, so serialise it or concurrent deletes drop each other's ids.
        synchronized(TOMBSTONE_LOCK) {
            val merged = LinkedHashSet(readDeletedTelephonyIds())
            // Re-added at the end so the newest ids are the ones that survive the bound below.
            merged.removeAll(telephonyIds.toSet())
            merged.addAll(telephonyIds)
            val bounded = merged.toList().takeLast(MAX_DELETED_TELEPHONY_IDS)
            deletedTelephonyIdPrefs.edit()
                .putString(KEY_DELETED_TELEPHONY_IDS, bounded.joinToString(","))
                .commit()
        }
    }

    /**
     * Drops tombstones for ids the provider has handed back to us as live rows again.
     *
     * The SMS provider does not use AUTOINCREMENT, so SQLite reuses the rowid of a deleted message:
     * delete the newest thread and the next SMS you send can be assigned one of the ids you just
     * tombstoned. Without this the tombstone would swallow that brand-new message - it would go out
     * over the air and never appear in the conversation.
     */
    private fun forgetDeletedTelephonyIds(telephonyIds: List<Long>) {
        if (telephonyIds.isEmpty()) return
        synchronized(TOMBSTONE_LOCK) {
            val remaining = readDeletedTelephonyIds() - telephonyIds.toSet()
            deletedTelephonyIdPrefs.edit()
                .putString(KEY_DELETED_TELEPHONY_IDS, remaining.joinToString(","))
                .commit()
        }
    }

    suspend fun loadLocalMessageEntities(threadId: Long): List<SmsMessageEntity> = withContext(Dispatchers.IO) {
        smsDao.messagesForThread(threadId)
    }

    /**
     * Undo of a delete still inside its snackbar window: puts the snapshot back, lifts the
     * tombstones so the provider rows are importable again, and - when sync is on - re-queues the
     * messages so the delete events already sitting in sync_queue do not leave other devices
     * believing the thread is gone.
     */
    suspend fun restoreThreadMessages(messages: List<SmsMessageEntity>) = withContext(Dispatchers.IO) {
        if (messages.isEmpty()) return@withContext
        smsDao.insertSmsBatch(messages.map { it.copy(id = 0) })
        forgetDeletedTelephonyIds(messages.mapNotNull { it.telephonyId })
        enqueueForSync { store ->
            messages.forEach { store.enqueueMessage(it.toModel()) }
        }
    }

    suspend fun loadLocalMessages(threadId: Long): List<SmsMessage> = withContext(Dispatchers.IO) {
        smsDao.messagesForThread(threadId).map { it.toModel() }
    }

    suspend fun searchLocalMessages(query: String): List<SmsMessage> = withContext(Dispatchers.IO) {
        smsDao.searchMessages(query).map { it.toModel() }
    }

    suspend fun loadFailedOutgoingMessages(): List<SmsMessage> = withContext(Dispatchers.IO) {
        smsDao.failedOutgoingMessages().map { it.toModel() }
    }

    suspend fun loadMessages(threadId: Long): List<SmsMessage> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.STATUS,
            SUBSCRIPTION_ID,
        )
        val cursor = try {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                // Guarded like syncTelephonyMessages (B-22) and draft-free like the import (B-29).
                "${Telephony.Sms.THREAD_ID} = ? AND ${TelephonyDirectionMapper.IMPORTABLE_TYPE_SELECTION}",
                arrayOf(threadId.toString()),
                "date ASC",
            )
        } catch (_: SecurityException) {
            // READ_SMS can be withdrawn when the app is no longer the default SMS handler.
            return@withContext emptyList()
        }
        cursor?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val thread = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val address = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val body = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val date = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val type = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            val status = cursor.getColumnIndexOrThrow(Telephony.Sms.STATUS)
            val subId = cursor.getColumnIndex(SUBSCRIPTION_ID)
            buildList {
                while (cursor.moveToNext()) {
                    val smsType = cursor.getInt(type)
                    add(
                        SmsMessage(
                            id = cursor.getLong(id),
                            threadId = cursor.getLong(thread),
                            address = cursor.getString(address).orEmpty(),
                            body = cursor.getString(body).orEmpty(),
                            timestamp = Instant.ofEpochMilli(cursor.getLong(date)),
                            direction = if (smsType == Telephony.Sms.MESSAGE_TYPE_INBOX) MessageDirection.Incoming else MessageDirection.Outgoing,
                            status = messageStatus(smsType, cursor.getInt(status)),
                            simSlot = cursor.intOrNull(subId),
                        ),
                    )
                }
            }
        }.orEmpty()
    }

    suspend fun persistOutgoing(recipient: String, body: String, subscriptionId: Int?): Long = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, recipient)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, System.currentTimeMillis())
            put(Telephony.Sms.READ, 1)
            put(Telephony.Sms.SEEN, 1)
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_OUTBOX)
            put(Telephony.Sms.STATUS, Telephony.Sms.STATUS_PENDING)
            subscriptionId?.let { put(SUBSCRIPTION_ID, it) }
        }
        context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)?.let(ContentUris::parseId)
            ?: error("Unable to persist outgoing SMS")
    }

    suspend fun markSending(telephonyId: Long) = withContext(Dispatchers.IO) {
        updateMessage(telephonyId, Telephony.Sms.MESSAGE_TYPE_OUTBOX, Telephony.Sms.STATUS_PENDING)
        smsDao.updateSendStatusByTelephonyId(telephonyId, MessageStatus.Sending)
        enqueueMessageChangeByTelephonyId(telephonyId)
    }

    fun markSent(telephonyId: Long) {
        updateMessage(telephonyId, Telephony.Sms.MESSAGE_TYPE_SENT, Telephony.Sms.STATUS_NONE)
        smsDao.updateSendStatusByTelephonyId(telephonyId, MessageStatus.Sent)
        enqueueMessageChangeByTelephonyId(telephonyId)
    }

    fun markDelivered(telephonyId: Long) {
        updateMessage(telephonyId, Telephony.Sms.MESSAGE_TYPE_SENT, Telephony.Sms.STATUS_COMPLETE)
        smsDao.updateSendStatusByTelephonyId(telephonyId, MessageStatus.Delivered)
        enqueueMessageChangeByTelephonyId(telephonyId)
    }

    fun markFailed(telephonyId: Long) {
        updateMessage(telephonyId, Telephony.Sms.MESSAGE_TYPE_FAILED, Telephony.Sms.STATUS_FAILED)
        smsDao.updateSendStatusByTelephonyId(telephonyId, MessageStatus.Failed)
        enqueueMessageChangeByTelephonyId(telephonyId)
    }

    fun notifyRefreshForTelephonyId(telephonyId: Long) {
        smsDao.messageByTelephonyId(telephonyId)?.threadId?.let { SmsRefreshEvents.notifyThreadChanged(it) }
    }

    private fun updateMessage(messageId: Long, type: Int, status: Int) {
        val values = ContentValues().apply {
            put(Telephony.Sms.TYPE, type)
            put(Telephony.Sms.STATUS, status)
        }
        context.contentResolver.update(messageUri(messageId), values, null, null)
    }

    /**
     * Queues every locally stored message for upload, the initial backfill a freshly enabled sync
     * needs (B-03). Returns the number of messages queued; 0 when sync is off.
     */
    suspend fun enqueueInitialBackfill(): Int = withContext(Dispatchers.IO) {
        if (!syncEnabled()) return@withContext 0
        var queued = 0
        smsDao.allMessages().forEach { message ->
            runCatching { syncQueueStore.enqueueMessage(message.toModel(), initialBackfill = true) }
                .onSuccess { queued++ }
                .onFailure { Log.w(TAG, "Unable to enqueue backfill message ${message.id}", it) }
        }
        queued
    }

    /**
     * A user who never opted into sync must not accumulate a queue (PRD 5.6), so every enqueue is
     * gated on the toggle, and a queue failure is logged rather than thrown: it must never take down
     * a send or a receive.
     */
    private fun enqueueForSync(block: (RoomSyncQueueStore) -> Unit) {
        if (!syncEnabled()) return
        runCatching { block(syncQueueStore) }
            .onFailure { Log.w(TAG, "Unable to enqueue sync event", it) }
    }

    private fun syncEnabled(): Boolean = runCatching { preferences.syncSettings().enabled }.getOrDefault(false)

    private fun enqueueMessageChange(message: SmsMessageEntity) {
        // insertMessage ignores conflicts and then returns -1; there is nothing to upload in that case.
        if (message.id <= 0) return
        enqueueForSync { store -> store.enqueueMessage(message.toModel()) }
    }

    private fun enqueueMessageChangeById(messageId: Long) {
        if (!syncEnabled()) return
        val message = runCatching { smsDao.messagesByIds(listOf(messageId)).firstOrNull() }.getOrNull() ?: return
        enqueueMessageChange(message)
    }

    private fun enqueueMessageChangeByTelephonyId(telephonyId: Long) {
        if (!syncEnabled()) return
        val message = runCatching { smsDao.messageByTelephonyId(telephonyId) }.getOrNull() ?: return
        enqueueMessageChange(message)
    }

    private fun enqueueDeletes(messages: List<SmsMessageEntity>) {
        if (messages.isEmpty()) return
        enqueueForSync { store ->
            val deletedAt = Instant.now().toString()
            messages.forEach { message ->
                store.enqueueThreadState(
                    message.threadId,
                    "delete",
                    JSONObject()
                        .put("message_id", message.id)
                        .put("deleted_at", deletedAt),
                )
            }
        }
    }

    private fun messageStatus(type: Int, status: Int): MessageStatus = when (type) {
        Telephony.Sms.MESSAGE_TYPE_INBOX -> MessageStatus.Received
        Telephony.Sms.MESSAGE_TYPE_OUTBOX, Telephony.Sms.MESSAGE_TYPE_QUEUED -> MessageStatus.Sending
        Telephony.Sms.MESSAGE_TYPE_FAILED -> MessageStatus.Failed
        Telephony.Sms.MESSAGE_TYPE_SENT -> if (status == Telephony.Sms.STATUS_COMPLETE) MessageStatus.Delivered else MessageStatus.Sent
        else -> MessageStatus.Received
    }

    private fun messageUri(messageId: Long): Uri = ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, messageId)

    private fun android.database.Cursor.intOrNull(column: Int): Int? = if (column >= 0 && !isNull(column)) getInt(column) else null

    companion object {
        private const val SUBSCRIPTION_ID = "sub_id"
        private const val SYNC_INSERT_BATCH_SIZE = 500
        private const val RECENT_OUTGOING_REFRESH_LIMIT = 50
        private const val RECENT_OUTGOING_REFRESH_BATCH_SIZE = 25
        private const val SYNC_ID_GAP_WINDOW = 1000L
        private const val TAG = "SmsRepository"
        private const val DELETED_TELEPHONY_IDS_PREFS = "kheyr_deleted_telephony_ids"
        private const val KEY_DELETED_TELEPHONY_IDS = "telephony_ids"
        // Only ids the gap window (SYNC_ID_GAP_WINDOW) can still reach matter, so a bounded set of the
        // most recent deletions is enough and can never grow without limit.
        private const val MAX_DELETED_TELEPHONY_IDS = 2000

        /** Process-wide guard for the tombstone read-modify-write; SmsRepository is constructed per use. */
        private val TOMBSTONE_LOCK = Any()
    }
    private fun SmsMessageEntity.toModel() = SmsMessage(
        id = id,
        threadId = threadId,
        address = address,
        body = body,
        timestamp = timestamp,
        direction = direction,
        status = status,
        simSlot = simSlot,
        telephonyId = telephonyId,
    )

    private fun ThreadWithLatestMessage.toModel() = SmsThread(
        id = id,
        address = address,
        displayName = displayName,
        lastMessage = lastMessage,
        lastMessageAt = lastMessageAt,
        unreadCount = unreadCount,
        isPinned = isPinned,
        pinnedAt = pinnedAt,
        isMuted = isMuted,
        isSpam = isSpam,
        isArchived = isArchived,
        simSlot = simSlot,
    )
}
