package com.kheyr.sms.sync

data class SyncDownloadResult(val changes: Int, val nextCursor: String?, val hasMore: Boolean)

class SyncDownloader {
    fun parse(cursor: String?, payloadCount: Int, nextCursor: String?, hasMore: Boolean): SyncDownloadResult =
        SyncDownloadResult(changes = payloadCount, nextCursor = nextCursor ?: cursor, hasMore = hasMore)
}
