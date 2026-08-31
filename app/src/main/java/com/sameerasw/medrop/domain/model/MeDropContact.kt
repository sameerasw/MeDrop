package com.sameerasw.medrop.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MeDropContact(
    val lookupKey: String,
    val displayName: String,
    val photoUri: String? = null,
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
) {
    @Suppress("SENSELESS_COMPARISON")
    fun getSafePhones(): List<String> = if (phones != null) phones else emptyList()
    @Suppress("SENSELESS_COMPARISON")
    fun getSafeEmails(): List<String> = if (emails != null) emails else emptyList()
    @Suppress("SENSELESS_COMPARISON")
    fun getSafeAddresses(): List<String> = if (addresses != null) addresses else emptyList()
    @Suppress("SENSELESS_COMPARISON")
    fun getSafeAddressTypes(): List<Int> = if (addressTypes != null) addressTypes else emptyList()
    @Suppress("SENSELESS_COMPARISON")
    fun getSafeUrls(): List<String> = if (urls != null) urls else emptyList()

    fun getDefaultContactEntryIds(): Set<String> {
        val set = mutableSetOf<String>()
        set.add("photo")
        if (!nickname.isNullOrBlank()) set.add("nickname")
        if (getSafePhones().isNotEmpty()) set.add("phone_0")
        if (getSafeEmails().isNotEmpty()) set.add("email_0")
        if (!organization.isNullOrBlank()) set.add("organization")
        if (getSafeUrls().isNotEmpty()) set.add("url_0")
        return set
    }

    fun getDefaultProfessionalEntryIds(): Set<String> {
        val set = mutableSetOf<String>()
        set.add("photo")
        if (getSafePhones().isNotEmpty()) set.add("phone_0")
        if (getSafeEmails().isNotEmpty()) set.add("email_0")
        if (!organization.isNullOrBlank()) set.add("organization")
        if (!department.isNullOrBlank()) set.add("department")
        if (!jobTitle.isNullOrBlank()) set.add("jobTitle")
        if (!role.isNullOrBlank()) set.add("role")

        val safeAddresses = getSafeAddresses()
        val safeTypes = getSafeAddressTypes()
        val workIndex = safeTypes.indexOfFirst { it == 2 }
        if (workIndex != -1 && workIndex < safeAddresses.size) {
            set.add("address_$workIndex")
        }

        if (getSafeUrls().isNotEmpty()) set.add("url_0")
        return set
    }

    fun toVCard(
        context: android.content.Context? = null,
        activeEntryIds: Set<String>,
        customPhotoUri: String? = null,
        customDisplayName: String? = null,
        fieldOverrides: Map<String, String> = emptyMap(),
        includeRev: Boolean = true
    ): String {
        val effectiveDisplayName = customDisplayName?.takeIf { it.isNotBlank() } ?: displayName
        val effectivePhotoUri = customPhotoUri ?: photoUri
        val sb = StringBuilder()
        sb.appendLine("BEGIN:VCARD")
        sb.appendLine("VERSION:3.0")
        sb.appendLine("FN:$effectiveDisplayName")
        sb.appendLine("N:${buildNField(effectiveDisplayName)}")

        val effNickname = fieldOverrides["nickname"] ?: nickname
        if (activeEntryIds.contains("nickname") && !effNickname.isNullOrBlank()) {
            sb.appendLine("NICKNAME:$effNickname")
        }

        if (activeEntryIds.contains("photo") && !effectivePhotoUri.isNullOrBlank() && context != null) {
            val cached = photoBase64Cache[effectivePhotoUri]
            if (cached != null) {
                sb.appendLine("PHOTO;TYPE=JPEG;ENCODING=b:$cached")
            } else {
                try {
                    val uri = android.net.Uri.parse(effectivePhotoUri)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val originalBitmap = android.graphics.BitmapFactory.decodeStream(stream)
                        if (originalBitmap != null) {
                            val maxDim = 96
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
                            val baos = java.io.ByteArrayOutputStream()
                            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 60, baos)
                            val bytes = baos.toByteArray()
                            if (bytes.isNotEmpty()) {
                                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                                photoBase64Cache[effectivePhotoUri] = base64
                                sb.appendLine("PHOTO;TYPE=JPEG;ENCODING=b:$base64")
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        val effBirthday = fieldOverrides["birthday"] ?: birthday
        if (activeEntryIds.contains("birthday") && !effBirthday.isNullOrBlank()) {
            sb.appendLine("BDAY:$effBirthday")
        }

        val effPronouns = fieldOverrides["pronouns"] ?: pronouns
        if (activeEntryIds.contains("pronouns") && !effPronouns.isNullOrBlank()) {
            sb.appendLine("PRONOUNS:$effPronouns")
            sb.appendLine("X-PRONOUNS:$effPronouns")
        }

        val effOrg = fieldOverrides["organization"] ?: organization
        val effDept = fieldOverrides["department"] ?: department
        val hasOrg = activeEntryIds.contains("organization") && !effOrg.isNullOrBlank()
        val hasDept = activeEntryIds.contains("department") && !effDept.isNullOrBlank()
        if (hasOrg || hasDept) {
            val orgPart = if (hasOrg) effOrg else ""
            val deptPart = if (hasDept) ";$effDept" else ""
            sb.appendLine("ORG:$orgPart$deptPart")
        }

        val effJobTitle = fieldOverrides["jobTitle"] ?: jobTitle
        if (activeEntryIds.contains("jobTitle") && !effJobTitle.isNullOrBlank()) {
            sb.appendLine("TITLE:$effJobTitle")
        }

        val effRole = fieldOverrides["role"] ?: role
        if (activeEntryIds.contains("role") && !effRole.isNullOrBlank()) {
            sb.appendLine("ROLE:$effRole")
        }

        getSafePhones().forEachIndexed { i, phone ->
            val effPhone = fieldOverrides["phone_$i"] ?: phone
            if (activeEntryIds.contains("phone_$i") && effPhone.isNotBlank()) {
                sb.appendLine("TEL;TYPE=CELL:$effPhone")
            }
        }
        getSafeEmails().forEachIndexed { i, email ->
            val effEmail = fieldOverrides["email_$i"] ?: email
            if (activeEntryIds.contains("email_$i") && effEmail.isNotBlank()) {
                sb.appendLine("EMAIL;TYPE=INTERNET:$effEmail")
            }
        }
        getSafeAddresses().forEachIndexed { i, addr ->
            val effAddr = fieldOverrides["address_$i"] ?: addr
            if (activeEntryIds.contains("address_$i") && effAddr.isNotBlank()) {
                val addrType = getSafeAddressTypes().getOrNull(i)
                val typeTag = if (addrType == 2) "WORK" else "HOME"
                sb.appendLine("ADR;TYPE=$typeTag:;;${effAddr.replace("\n", ";")};;;")
            }
        }
        getSafeUrls().forEachIndexed { i, url ->
            val effUrl = fieldOverrides["url_$i"] ?: url
            if (activeEntryIds.contains("url_$i") && effUrl.isNotBlank()) {
                sb.appendLine("URL:$effUrl")
            }
        }
        val effNote = fieldOverrides["note"] ?: note
        if (activeEntryIds.contains("note") && !effNote.isNullOrBlank()) {
            sb.appendLine("NOTE:${effNote.replace("\n", " ")}")
        }

        if (includeRev) {
            val rev = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).format(Date())
            sb.appendLine("REV:$rev")
        }
        sb.append("END:VCARD")
        return sb.toString()
    }

    private fun buildNField(displayName: String): String {
        val parts = displayName.trim().split(" ")
        val last = if (parts.size > 1) parts.last() else ""
        val first = if (parts.size > 1) parts.dropLast(1).joinToString(" ") else displayName
        return "$last;$first;;;"
    }

    companion object {
        private val photoBase64Cache = java.util.concurrent.ConcurrentHashMap<String, String>()

        fun clearPhotoCache() {
            photoBase64Cache.clear()
        }
    }
}
