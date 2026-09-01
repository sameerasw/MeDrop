package com.sameerasw.medrop.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.ContactsContract
import android.util.Base64
import androidx.annotation.Keep

@Keep
data class ReceivedContact(
    val displayName: String = "",
    val nickname: String? = null,
    val pronouns: String? = null,
    val birthday: String? = null,
    val organization: String? = null,
    val department: String? = null,
    val jobTitle: String? = null,
    val role: String? = null,
    val phones: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
    val addresses: List<String> = emptyList(),
    val urls: List<String> = emptyList(),
    val note: String? = null,
    val photoBase64: String? = null,
) {
    fun getPhotoBitmap(): Bitmap? {
        if (photoBase64.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(photoBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    fun saveToSystemContacts(context: Context) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            type = ContactsContract.RawContacts.CONTENT_TYPE
            putExtra(ContactsContract.Intents.Insert.NAME, displayName)

            if (!jobTitle.isNullOrBlank()) {
                putExtra(ContactsContract.Intents.Insert.JOB_TITLE, jobTitle)
            }
            if (!organization.isNullOrBlank()) {
                putExtra(ContactsContract.Intents.Insert.COMPANY, organization)
            }

            phones.forEachIndexed { i, phone ->
                when (i) {
                    0 -> putExtra(ContactsContract.Intents.Insert.PHONE, phone)
                    1 -> putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE, phone)
                    2 -> putExtra(ContactsContract.Intents.Insert.TERTIARY_PHONE, phone)
                }
            }

            emails.forEachIndexed { i, email ->
                when (i) {
                    0 -> putExtra(ContactsContract.Intents.Insert.EMAIL, email)
                    1 -> putExtra(ContactsContract.Intents.Insert.SECONDARY_EMAIL, email)
                    2 -> putExtra(ContactsContract.Intents.Insert.TERTIARY_EMAIL, email)
                }
            }

            if (addresses.isNotEmpty()) {
                putExtra(ContactsContract.Intents.Insert.POSTAL, addresses.first())
            }

            if (!note.isNullOrBlank()) {
                putExtra(ContactsContract.Intents.Insert.NOTES, note)
            }

            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
    }
}

object VCardParser {

    fun parse(vcardText: String): ReceivedContact? {
        if (!vcardText.contains("BEGIN:VCARD", ignoreCase = true)) return null

        var displayName = ""
        var nickname: String? = null
        var pronouns: String? = null
        var birthday: String? = null
        var organization: String? = null
        var department: String? = null
        var jobTitle: String? = null
        var role: String? = null
        val phones = mutableListOf<String>()
        val emails = mutableListOf<String>()
        val addresses = mutableListOf<String>()
        val urls = mutableListOf<String>()
        var note: String? = null
        val photoSb = StringBuilder()
        var isReadingPhoto = false

        val lines = vcardText.lines()
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            if (isReadingPhoto) {
                if (line.startsWith(" ") || line.startsWith("\t") || !line.contains(":")) {
                    photoSb.append(line.trim())
                    continue
                } else {
                    isReadingPhoto = false
                }
            }

            val colonIdx = line.indexOf(':')
            if (colonIdx == -1) continue

            val keyPart = line.substring(0, colonIdx).uppercase()
            val valuePart = line.substring(colonIdx + 1).trim()

            when {
                keyPart == "FN" || keyPart.startsWith("FN;") -> {
                    if (displayName.isBlank()) {
                        displayName = valuePart
                    }
                }
                keyPart == "N" || keyPart.startsWith("N;") -> {
                    if (displayName.isBlank()) {
                        val parts = valuePart.split(";")
                        val last = parts.getOrNull(0)?.trim() ?: ""
                        val first = parts.getOrNull(1)?.trim() ?: ""
                        displayName = listOf(first, last).filter { it.isNotBlank() }.joinToString(" ")
                    }
                }
                keyPart == "NICKNAME" || keyPart.startsWith("NICKNAME;") -> {
                    nickname = valuePart
                }
                keyPart == "X-PRONOUNS" || keyPart == "PRONOUNS" || keyPart.startsWith("X-PRONOUNS;") -> {
                    pronouns = valuePart
                }
                keyPart == "BDAY" || keyPart.startsWith("BDAY;") -> {
                    birthday = valuePart
                }
                keyPart == "ORG" || keyPart.startsWith("ORG;") -> {
                    val orgParts = valuePart.split(";")
                    organization = orgParts.getOrNull(0)?.trim()
                    if (orgParts.size > 1) {
                        department = orgParts.getOrNull(1)?.trim()
                    }
                }
                keyPart == "TITLE" || keyPart.startsWith("TITLE;") -> {
                    jobTitle = valuePart
                }
                keyPart == "ROLE" || keyPart.startsWith("ROLE;") -> {
                    role = valuePart
                }
                keyPart == "TEL" || keyPart.startsWith("TEL;") -> {
                    if (valuePart.isNotBlank() && !phones.contains(valuePart)) {
                        phones.add(valuePart)
                    }
                }
                keyPart == "EMAIL" || keyPart.startsWith("EMAIL;") -> {
                    if (valuePart.isNotBlank() && !emails.contains(valuePart)) {
                        emails.add(valuePart)
                    }
                }
                keyPart == "ADR" || keyPart.startsWith("ADR;") -> {
                    val adrFormatted = valuePart.split(";")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .joinToString(", ")
                    if (adrFormatted.isNotBlank() && !addresses.contains(adrFormatted)) {
                        addresses.add(adrFormatted)
                    }
                }
                keyPart == "URL" || keyPart.startsWith("URL;") -> {
                    if (valuePart.isNotBlank() && !urls.contains(valuePart)) {
                        urls.add(valuePart)
                    }
                }
                keyPart == "NOTE" || keyPart.startsWith("NOTE;") -> {
                    note = valuePart.replace("\\n", "\n")
                }
                keyPart.startsWith("PHOTO") -> {
                    isReadingPhoto = true
                    photoSb.append(valuePart)
                }
            }
        }

        val photoBase64 = photoSb.toString().takeIf { it.isNotBlank() }

        if (displayName.isBlank() && phones.isEmpty() && emails.isEmpty()) {
            return null
        }

        return ReceivedContact(
            displayName = displayName.ifBlank { "Contact" },
            nickname = nickname,
            pronouns = pronouns,
            birthday = birthday,
            organization = organization,
            department = department,
            jobTitle = jobTitle,
            role = role,
            phones = phones,
            emails = emails,
            addresses = addresses,
            urls = urls,
            note = note,
            photoBase64 = photoBase64
        )
    }
}
