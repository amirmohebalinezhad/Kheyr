package com.kheyr.sms.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.kheyr.sms.data.SmsThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DeviceContact(
    val id: Long,
    val displayName: String,
    val phoneNumber: String,
)

class ContactRepository(private val context: Context) {
    fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    suspend fun loadContacts(): List<DeviceContact> = withContext(Dispatchers.IO) {
        if (!hasContactsPermission()) return@withContext emptyList()
        val contacts = mutableListOf<DeviceContact>()
        val seen = mutableSetOf<String>()
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            ),
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val number = cursor.getString(numberCol).orEmpty().trim()
                if (number.isEmpty()) continue
                val dedupeKey = "${cursor.getLong(idCol)}:$number"
                if (!seen.add(dedupeKey)) continue
                contacts += DeviceContact(
                    id = cursor.getLong(idCol),
                    displayName = cursor.getString(nameCol).orEmpty().ifBlank { number },
                    phoneNumber = number,
                )
            }
        }
        contacts
    }

    suspend fun lookupDisplayName(phoneNumber: String): String? = withContext(Dispatchers.IO) {
        if (!hasContactsPermission() || phoneNumber.isBlank()) return@withContext null
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0)?.takeIf { it.isNotBlank() } else null
        }
    }

    suspend fun enrichThreads(threads: List<SmsThread>): List<SmsThread> = withContext(Dispatchers.IO) {
        if (!hasContactsPermission() || threads.isEmpty()) return@withContext threads
        val nameIndex = buildNameIndex()
        val cache = mutableMapOf<String, String?>()
        threads.map { thread ->
            if (thread.displayName != thread.address && thread.displayName.isNotBlank()) return@map thread
            val resolved = resolveDisplayName(thread.address, nameIndex, cache)
            resolved?.let { thread.copy(displayName = it) } ?: thread
        }
    }

    private fun buildNameIndex(): Map<String, String> {
        val index = mutableMapOf<String, String>()
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            val nameCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameCol).orEmpty()
                if (name.isBlank()) continue
                val number = cursor.getString(numberCol).orEmpty().trim()
                if (number.isEmpty()) continue
                index.putIfAbsent(number, name)
                index.putIfAbsent(normalizePhone(number), name)
            }
        }
        return index
    }

    private fun resolveDisplayName(
        address: String,
        nameIndex: Map<String, String>,
        cache: MutableMap<String, String?>,
    ): String? = cache.getOrPut(address) {
        nameIndex[address]
            ?: nameIndex[normalizePhone(address)]
            ?: lookupDisplayNameSync(address)
    }

    private fun lookupDisplayNameSync(phoneNumber: String): String? {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        return context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0)?.takeIf { it.isNotBlank() } else null
        }
    }

    private fun normalizePhone(phone: String): String = phone.filter { it.isDigit() || it == '+' }
}
