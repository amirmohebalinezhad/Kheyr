package com.kheyr.sms.telephony

import android.content.Context
import android.provider.Telephony

/**
 * Resolves a telephony thread id for a recipient address, tolerating providers that reject
 * alphanumeric sender IDs (e.g. "VERIFY", "Chase", bank short-names).
 *
 * Some OEM telephony providers throw IllegalArgumentException ("Unable to find or allocate a thread
 * ID") from [Telephony.Threads.getOrCreateThreadId] for non-numeric addresses. Rather than letting
 * that exception bubble up and drop the incoming message entirely, we fall back to a deterministic,
 * stable, positive synthetic id so the sender still gets its own conversation.
 */
object ThreadIdResolver {
    // Synthetic ids live far above the provider's small sequential thread ids to avoid collisions,
    // and stay positive so notification deep-links (which reject negative thread ids) keep working.
    const val SYNTHETIC_THREAD_ID_BASE = 1_000_000_000_000L

    fun getOrCreateThreadId(context: Context, address: String): Long =
        runCatching { Telephony.Threads.getOrCreateThreadId(context, setOf(address)) }
            .getOrElse { syntheticThreadId(address) }

    /** Stable positive id derived from the address; the same sender always maps to the same thread. */
    fun syntheticThreadId(address: String): Long =
        SYNTHETIC_THREAD_ID_BASE + (address.trim().uppercase().hashCode().toLong() and 0xFFFFFFFFL)
}
