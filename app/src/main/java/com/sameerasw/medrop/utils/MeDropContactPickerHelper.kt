package com.sameerasw.medrop.utils

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import com.sameerasw.medrop.domain.model.MeDropContact
import com.sameerasw.medrop.domain.model.MeDropProfileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MeDropContactPickerHelper {

    fun buildPickIntent(): Intent =
        Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)

    suspend fun saveCompressedCustomPhoto(uri: Uri, context: Context, profileType: MeDropProfileType): String? =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val originalBitmap = android.graphics.BitmapFactory.decodeStream(stream) ?: return@withContext null
                    val maxDim = 1024
                    val width = originalBitmap.width
                    val height = originalBitmap.height
                    val ratio = if (width > height) {
                        maxDim.toFloat() / width
                    } else {
                        maxDim.toFloat() / height
                    }
                    val scaledBitmap = if (ratio < 1.0f) {
                        android.graphics.Bitmap.createScaledBitmap(
                            originalBitmap,
                            (width * ratio).toInt().coerceAtLeast(1),
                            (height * ratio).toInt().coerceAtLeast(1),
                            true
                        )
                    } else {
                        originalBitmap
                    }

                    val photosDir = java.io.File(context.filesDir, "medrop")
                    if (!photosDir.exists()) photosDir.mkdirs()
                    val prefix = "custom_photo_${profileType.name.lowercase()}"
                    photosDir.listFiles()?.forEach { file ->
                        if (file.name.startsWith(prefix) && file.name.endsWith(".jpg")) {
                            file.delete()
                        }
                    }
                    val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
                    val photoFile = java.io.File(photosDir, fileName)
                    java.io.FileOutputStream(photoFile).use { out ->
                        scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    Uri.fromFile(photoFile).toString()
                }
            } catch (_: Exception) {
                null
            }
        }

    suspend fun processResult(uri: Uri, context: Context): MeDropContact? =
        withContext(Dispatchers.IO) {
            val projection = arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.LOOKUP_KEY,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ContactsContract.Contacts.PHOTO_URI
            )
            val contactId: Long
            val lookupKey: String
            val displayName: String
            var contactPhotoUri: String? = null

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@withContext null
                contactId = cursor.getLong(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                lookupKey = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)) ?: ""
                displayName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)) ?: ""
                val photoCol = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
                if (photoCol != -1) {
                    contactPhotoUri = cursor.getString(photoCol)
                }
            } ?: return@withContext null

            val details = try {
                fetchContactDetails(context, contactId.toString())
            } catch (_: SecurityException) {
                ExtractedDetails()
            } catch (_: Exception) {
                ExtractedDetails()
            }

            val savedContactPhoto = if (contactPhotoUri != null) {
                try {
                    val pUri = Uri.parse(contactPhotoUri)
                    saveCompressedCustomPhoto(pUri, context, MeDropProfileType.CONTACT)
                } catch (_: Exception) {
                    null
                }
            } else {
                try {
                    val contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
                    val photoStream = ContactsContract.Contacts.openContactPhotoInputStream(context.contentResolver, contactUri, true)
                    photoStream?.use { stream ->
                        val originalBitmap = android.graphics.BitmapFactory.decodeStream(stream)
                        if (originalBitmap != null) {
                            val photosDir = java.io.File(context.filesDir, "medrop")
                            if (!photosDir.exists()) photosDir.mkdirs()
                            val photoFile = java.io.File(photosDir, "custom_photo_contact.jpg")
                            java.io.FileOutputStream(photoFile).use { out ->
                                originalBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                            }
                            Uri.fromFile(photoFile).toString()
                        } else null
                    }
                } catch (_: Exception) {
                    null
                }
            }

            MeDropContact(
                lookupKey = lookupKey,
                displayName = displayName,
                photoUri = savedContactPhoto,
                nickname = details.nickname,
                birthday = details.birthday,
                pronouns = details.pronouns,
                phones = details.phones.distinct(),
                emails = details.emails.distinct(),
                organization = details.organization,
                department = details.department,
                jobTitle = details.jobTitle,
                role = details.role,
                addresses = details.addresses.distinct(),
                addressTypes = details.addressTypes,
                urls = details.urls.distinct(),
                note = details.note
            )
        }

    private data class ExtractedDetails(
        val nickname: String? = null,
        val birthday: String? = null,
        val pronouns: String? = null,
        val phones: List<String> = emptyList(),
        val emails: List<String> = emptyList(),
        val organization: String? = null,
        val department: String? = null,
        val jobTitle: String? = null,
        val role: String? = null,
        val addresses: List<String> = emptyList(),
        val addressTypes: List<Int> = emptyList(),
        val urls: List<String> = emptyList(),
        val note: String? = null
    )

    private fun fetchContactDetails(context: Context, contactId: String): ExtractedDetails {
        var nickname: String? = null
        var birthday: String? = null
        var pronouns: String? = null
        val phones = mutableListOf<String>()
        val emails = mutableListOf<String>()
        var organization: String? = null
        var department: String? = null
        var jobTitle: String? = null
        var role: String? = null
        val addresses = mutableListOf<String>()
        val addressTypes = mutableListOf<Int>()
        val urls = mutableListOf<String>()
        var note: String? = null

        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(
                ContactsContract.Data.MIMETYPE,
                ContactsContract.Data.DATA1,
                ContactsContract.Data.DATA2,
                ContactsContract.Data.DATA3,
                ContactsContract.Data.DATA4,
                ContactsContract.Data.DATA5
            ),
            "${ContactsContract.Data.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )?.use { cursor ->
            val mimeIdx = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE)
            val data1Idx = cursor.getColumnIndex(ContactsContract.Data.DATA1)
            val data2Idx = cursor.getColumnIndex(ContactsContract.Data.DATA2)
            val data3Idx = cursor.getColumnIndex(ContactsContract.Data.DATA3)
            val data4Idx = cursor.getColumnIndex(ContactsContract.Data.DATA4)
            val data5Idx = cursor.getColumnIndex(ContactsContract.Data.DATA5)

            while (cursor.moveToNext()) {
                val mime = if (mimeIdx != -1) cursor.getString(mimeIdx) ?: continue else continue
                val data1 = if (data1Idx != -1) cursor.getString(data1Idx) ?: "" else ""
                val data2 = if (data2Idx != -1) cursor.getString(data2Idx) ?: "" else ""
                val data3 = if (data3Idx != -1) cursor.getString(data3Idx) ?: "" else ""
                val data4 = if (data4Idx != -1) cursor.getString(data4Idx) ?: "" else ""
                val data5 = if (data5Idx != -1) cursor.getString(data5Idx) ?: "" else ""

                when (mime) {
                    ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE -> {
                        if (data1.isNotBlank()) nickname = data1
                    }
                    ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE -> {
                        val eventType = if (data2.isNotBlank()) data2.toIntOrNull() else null
                        if (eventType == ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY && data1.isNotBlank()) {
                            birthday = data1
                        }
                    }
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> if (data1.isNotBlank()) phones.add(data1)
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> if (data1.isNotBlank()) emails.add(data1)
                    ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE -> {
                        if (data1.isNotBlank()) organization = data1
                        if (data5.isNotBlank()) department = data5
                        if (data4.isNotBlank()) jobTitle = data4
                        if (data3.isNotBlank()) role = data3
                    }
                    ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> {
                        if (data1.isNotBlank()) {
                            addresses.add(data1)
                            val type = if (data2.isNotBlank()) data2.toIntOrNull() ?: ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER else ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER
                            addressTypes.add(type)
                        }
                    }
                    ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE -> if (data1.isNotBlank()) urls.add(data1)
                    "vnd.android.cursor.item/pronouns" -> if (data1.isNotBlank()) pronouns = data1
                    ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE -> if (data1.isNotBlank()) note = data1
                }
            }
        }

        return ExtractedDetails(
            nickname = nickname,
            birthday = birthday,
            pronouns = pronouns,
            phones = phones,
            emails = emails,
            organization = organization,
            department = department,
            jobTitle = jobTitle,
            role = role,
            addresses = addresses,
            addressTypes = addressTypes,
            urls = urls,
            note = note
        )
    }
}
