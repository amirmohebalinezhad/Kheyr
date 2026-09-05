package com.kheyr.sms.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.SystemClock
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.kheyr.sms.data.SmsThread
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DeviceContact(
    val id: Long,
    val displayName: String,
    val phoneNumber: String,
    val photoUri: Uri? = null,
)

class ContactRepository(private val context: Context) {

    fun invalidateCache() {
        clearContactCache()
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
        lookupProfileInIndex(getContactData().profileIndex, phoneNumber)
            ?: lookupProfileSync(phoneNumber)
            ?: lookupProfileSync(PhoneNumberNormalizer.normalize(phoneNumber))
    }

    suspend fun enrichThreads(threads: List<SmsThread>): List<SmsThread> = withContext(Dispatchers.IO) {
        if (!hasContactsPermission() || threads.isEmpty()) return@withContext threads
        val data = getContactData()
        threads.map { thread ->
            val profile = lookupProfileInIndex(data.profileIndex, thread.address)
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

    internal fun lookupProfileInIndex(profileIndex: Map<String, ContactProfile>, address: String): ContactProfile? {
        profileIndex[address]?.let { return it }
        // Never fall back on a blank key: an empty normalized/match key would collide with the
        // blank-keyed entries an alphanumeric sender collapses to, mislabeling unrelated threads.
        val normalized = PhoneNumberNormalizer.normalize(address)
        if (normalized.isNotBlank()) profileIndex[normalized]?.let { return it }
        val key = PhoneNumberNormalizer.matchKey(address)
        if (key.isNotBlank()) profileIndex[key]?.let { return it }
        return null
    }

    private data class CachedContactData(
        val nameIndex: Map<String, String>,
        val profileIndex: Map<String, ContactProfile>,
    )

    private suspend fun getContactData(): CachedContactData {
        ensureContactObserver(context)
        cachedContactDataOrNull()?.let { return it }
        val built = buildContactData()
        storeContactData(built)
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
                // Never index a blank key: an empty normalized/match key would otherwise match every
                // alphanumeric sender and mislabel unrelated threads with this contact.
                for (key in setOf(number, PhoneNumberNormalizer.normalize(number), PhoneNumberNormalizer.matchKey(number))) {
                    if (key.isBlank()) continue
                    nameIndex.putIfAbsent(key, name)
                    profileIndex.putIfAbsent(key, profile)
                }
            }
        }
        return CachedContactData(nameIndex, profileIndex)
    }

    private fun loadPhotoUriByContactId(): Map<Long, Uri> {
        val photos = mutableMapOf<Long, Uri>()
        // Avatars render at 40-54dp, so the thumbnail is plenty; PHOTO_URI is the full-resolution
        // image and made every row decode a large bitmap.
        context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.PHOTO_THUMBNAIL_URI),
            "${ContactsContract.Contacts.HAS_PHONE_NUMBER} = 1",
            null,
            null,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val photoCol = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)
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

    internal companion object {
        // Contact data is cached at process scope: ContactRepository is constructed fresh at several
        // call sites (the shell, the notifier, the receive pipeline), so a per-instance cache would
        // almost never be hit. The cache is written from background threads and cleared from the
        // ContentObserver's binder thread, so every access goes through this lock.
        private val cacheLock = Any()
        private var cachedContactData: CachedContactData? = null
        private var cachedAtElapsedMillis: Long = 0L
        private val contactObserverRegistered = AtomicBoolean(false)

        // Belt and braces in case the observer never fires (some OEM providers are stingy with
        // change notifications): the cache goes stale on its own after this long.
        private const val CACHE_TTL_MILLIS: Long = 5 * 60 * 1000L

        private fun clearContactCache() {
            synchronized(cacheLock) {
                cachedContactData = null
                cachedAtElapsedMillis = 0L
            }
        }

        private fun cachedContactDataOrNull(): CachedContactData? = synchronized(cacheLock) {
            val cached = cachedContactData ?: return@synchronized null
            if (SystemClock.elapsedRealtime() - cachedAtElapsedMillis >= CACHE_TTL_MILLIS) {
                cachedContactData = null
                cachedAtElapsedMillis = 0L
                return@synchronized null
            }
            cached
        }

        private fun storeContactData(data: CachedContactData) {
            synchronized(cacheLock) {
                cachedContactData = data
                cachedAtElapsedMillis = SystemClock.elapsedRealtime()
            }
        }

        /** True while the process-wide contact cache holds usable data. Test seam only. */
        internal fun isContactCacheWarm(): Boolean = cachedContactDataOrNull() != null

        private fun ensureContactObserver(context: Context) {
            // Registered at most once per process, and deliberately never unregistered: the cache it
            // protects is process-scoped, so the process is the observer's correct lifetime.
            if (!contactObserverRegistered.compareAndSet(false, true)) return
            val observer = object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) {
                    clearContactCache()
                }
            }
            context.applicationContext.contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI,
                true,
                observer,
            )
        }
    }
}
