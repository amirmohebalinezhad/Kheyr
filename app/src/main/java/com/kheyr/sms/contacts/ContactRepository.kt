package com.kheyr.sms.contacts

import android.Manifest
import android.content.ContentUris
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
    val photoUri: Uri? = null,
)

class ContactRepository(private val context: Context) {
    @Volatile
    private var cachedContactData: CachedContactData? = null

    fun invalidateCache() {
        cachedContactData = null
    }

    fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    suspend fun loadContacts(): List<DeviceContact> = withContext(Dispatchers.IO) {
        if (!hasContactsPermission()) return@withContext emptyList()
        val photoByContactId = loadPhotoUriByContactId()
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
                val contactId = cursor.getLong(idCol)
                val dedupeKey = "$contactId:$number"
                if (!seen.add(dedupeKey)) continue
                contacts += DeviceContact(
                    id = contactId,
                    displayName = cursor.getString(nameCol).orEmpty().ifBlank { number },
                    phoneNumber = number,
                    photoUri = photoByContactId[contactId],
                )
            }
        }
        contacts
    }

    suspend fun lookupProfile(phoneNumber: String): ContactProfile? = withContext(Dispatchers.IO) {
        if (!hasContactsPermission() || phoneNumber.isBlank()) return@withContext null
        getContactData().profileIndex[phoneNumber]
            ?: getContactData().profileIndex[PhoneNumberNormalizer.normalize(phoneNumber)]
            ?: lookupProfileSync(phoneNumber)
    }

    suspend fun enrichThreads(threads: List<SmsThread>): List<SmsThread> = withContext(Dispatchers.IO) {
        if (!hasContactsPermission() || threads.isEmpty()) return@withContext threads
        val data = getContactData()
        threads.map { thread ->
            val profile = data.profileIndex[thread.address]
                ?: data.profileIndex[PhoneNumberNormalizer.normalize(thread.address)]
            if (profile == null && thread.displayName != thread.address && thread.displayName.isNotBlank()) {
                thread
            } else {
                thread.copy(
                    displayName = profile?.displayName?.takeIf { it.isNotBlank() }
                        ?: thread.displayName.takeIf { it.isNotBlank() && it != thread.address }
                        ?: thread.address,
                    contactPhotoUri = profile?.photoUri ?: thread.contactPhotoUri,
                )
            }
        }
    }

    fun matchesAddress(first: String, second: String): Boolean = PhoneNumberNormalizer.matches(first, second)

    private data class CachedContactData(
        val nameIndex: Map<String, String>,
        val profileIndex: Map<String, ContactProfile>,
    )

    private suspend fun getContactData(): CachedContactData {
        cachedContactData?.let { return it }
        val built = buildContactData()
        cachedContactData = built
        return built
    }

    private fun buildContactData(): CachedContactData {
        val nameIndex = mutableMapOf<String, String>()
        val profileIndex = mutableMapOf<String, ContactProfile>()
        val photoByContactId = loadPhotoUriByContactId()
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameCol).orEmpty()
                if (name.isBlank()) continue
                val number = cursor.getString(numberCol).orEmpty().trim()
                if (number.isEmpty()) continue
                val contactId = cursor.getLong(idCol)
                val profile = ContactProfile(
                    displayName = name,
                    photoUri = photoByContactId[contactId],
                    contactId = contactId,
                )
                nameIndex.putIfAbsent(number, name)
                nameIndex.putIfAbsent(PhoneNumberNormalizer.normalize(number), name)
                profileIndex.putIfAbsent(number, profile)
                profileIndex.putIfAbsent(PhoneNumberNormalizer.normalize(number), profile)
            }
        }
        return CachedContactData(nameIndex, profileIndex)
    }

    private fun loadPhotoUriByContactId(): Map<Long, Uri> {
        val photos = mutableMapOf<Long, Uri>()
        context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.PHOTO_URI),
            "${ContactsContract.Contacts.HAS_PHONE_NUMBER} = 1",
            null,
            null,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val photoCol = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI)
            while (cursor.moveToNext()) {
                val photo = cursor.getString(photoCol)?.takeIf { it.isNotBlank() } ?: continue
                photos[cursor.getLong(idCol)] = Uri.parse(photo)
            }
        }
        return photos
    }

    private fun lookupProfileSync(phoneNumber: String): ContactProfile? {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        return context.contentResolver.query(
            uri,
            arrayOf(
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup._ID,
                ContactsContract.PhoneLookup.PHOTO_URI,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            val name = cursor.getString(0)?.takeIf { it.isNotBlank() }
            val contactId = cursor.getLong(1)
            val photo = cursor.getString(2)?.takeIf { it.isNotBlank() }?.let(Uri::parse)
            ContactProfile(displayName = name, photoUri = photo, contactId = contactId)
        }
    }
}
