package com.buildwclaude.dialer.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import com.buildwclaude.dialer.domain.Contact
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private fun canRead() =
        context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    /** One entry per contact (first phone number), sorted by display name. */
    suspend fun contacts(): List<Contact> = withContext(Dispatchers.IO) {
        if (!canRead()) return@withContext emptyList()
        val byId = LinkedHashMap<Long, Contact>()
        runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                    ContactsContract.CommonDataKinds.Phone.STARRED,
                    ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
                ),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} IS NOT NULL",
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC",
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    if (byId.containsKey(id)) continue
                    val name = c.getString(1) ?: continue
                    byId[id] = Contact(
                        id = id,
                        name = name,
                        number = c.getString(2) ?: "",
                        photoUri = c.getString(3),
                        starred = c.getInt(4) == 1,
                        lookupKey = c.getString(5),
                    )
                }
            }
        }
        byId.values.sortedBy { it.name.lowercase() }
    }

    suspend fun favorites(): List<Contact> = contacts().filter { it.starred }

    /** Deletes whole contacts by lookup key. Requires WRITE_CONTACTS. */
    suspend fun delete(contacts: List<Contact>): Int = withContext(Dispatchers.IO) {
        var deleted = 0
        for (c in contacts) {
            val key = c.lookupKey ?: continue
            runCatching {
                val uri = android.net.Uri.withAppendedPath(
                    ContactsContract.Contacts.CONTENT_LOOKUP_URI, key,
                )
                deleted += context.contentResolver.delete(uri, null, null)
            }
        }
        deleted
    }

    fun contactUri(contact: Contact): android.net.Uri? {
        val key = contact.lookupKey ?: return null
        return android.net.Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, key)
    }
}
