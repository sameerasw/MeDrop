package com.sameerasw.medrop.services

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import com.google.gson.Gson
import com.sameerasw.medrop.data.repository.MeDropRepository
import com.sameerasw.medrop.domain.model.MeDropSettings
import com.sameerasw.medrop.utils.HapticUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MeDropHceService : HostApduService() {

    companion object {
        private val SELECT_AID_PREFIX = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00)
        private val NDEF_AID = byteArrayOf(
            0xD2.toByte(), 0x76, 0x00, 0x00, 0x85.toByte(), 0x01, 0x01
        )

        private val SW_OK = byteArrayOf(0x90.toByte(), 0x00)
        private val SW_UNKNOWN_CMD = byteArrayOf(0x00, 0x00)
        private val SW_FILE_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x82.toByte())

        private val CC_FILE_ID = byteArrayOf(0xE1.toByte(), 0x03)
        private val NDEF_FILE_ID = byteArrayOf(0xE1.toByte(), 0x04)

        private val CC_CONTENT = byteArrayOf(
            0x00, 0x0F,
            0x20,
            0x00, 0xFF.toByte(),
            0x00, 0xFF.toByte(),
            0x04, 0x06,
            0xE1.toByte(), 0x04,
            0x7F, 0xFF.toByte(),
            0x00,
            0xFF.toByte()
        )

        private val NDEF_AID_FCI = byteArrayOf(
            0x6F.toByte(), 0x10,
            0x84.toByte(), 0x07,
            0xD2.toByte(), 0x76, 0x00, 0x00, 0x85.toByte(), 0x01, 0x01,
            0xA5.toByte(), 0x05,
            0x50.toByte(), 0x03,
            0x4E, 0x46, 0x43
        )

        private val READ_BINARY_CMD_PREFIX = byteArrayOf(0x00, 0xB0.toByte())

        var pendingVCardBytes: ByteArray? = null

        val isScanActive = kotlinx.coroutines.flow.MutableStateFlow(false)

        // Static-page fallback for iPhones
        private const val WEB_FALLBACK_BASE_URL = "https://sameerasw.com/medrop-card/"

        private fun buildContactUrl(vcardString: String): String {
            val encoded = android.util.Base64.encodeToString(
                vcardString.toByteArray(Charsets.UTF_8),
                android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
            )
            return "$WEB_FALLBACK_BASE_URL#v=$encoded"
        }

        private fun ndefWrap(vcardString: String): ByteArray {
            val uriRecord = NdefRecord.createUri(buildContactUrl(vcardString))
            val vcardRecord = NdefRecord.createMime("text/vcard", vcardString.toByteArray(Charsets.UTF_8))
            val message = NdefMessage(arrayOf(uriRecord, vcardRecord))
            val ndefData = message.toByteArray()
            val nlen = byteArrayOf((ndefData.size shr 8).toByte(), (ndefData.size and 0xFF).toByte())
            return nlen + ndefData
        }

        fun prepareVCard(vcardString: String) {
            pendingVCardBytes = ndefWrap(vcardString)
        }

        fun clearVCard() {
            pendingVCardBytes = null
            isScanActive.value = false
        }
    }

    private var selectedFile: ByteArray? = null

    override fun onCreate() {
        super.onCreate()
        val json = MeDropRepository(this).getMeDropSettingsJson()
        if (json != null) {
            try {
                val settings = Gson().fromJson(json, MeDropSettings::class.java)
                val contact = settings.contact
                if (contact != null) {
                    val activeType = settings.activeProfileType
                    val activeEntries = settings.getEffectiveEntryIds(activeType)
                    val photoUri = settings.getEffectivePhotoUri(activeType)
                    val vcard = contact.toVCard(this, activeEntries, photoUri)
                    prepareVCard(vcard)
                }
            } catch (_: Exception) {}
        }
    }

    private var scanResetJob: Job? = null

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        if (commandApdu.size < 4) return SW_UNKNOWN_CMD

        return when {
            isSelectAidCommand(commandApdu) -> {
                selectedFile = null
                NDEF_AID_FCI + SW_OK
            }
            isSelectFileCommand(commandApdu) -> {
                if (commandApdu.size < 7) return SW_FILE_NOT_FOUND
                val fileId = commandApdu.copyOfRange(5, 7)
                when {
                    fileId.contentEquals(CC_FILE_ID) -> {
                        selectedFile = CC_CONTENT
                        SW_OK
                    }
                    fileId.contentEquals(NDDEF_FILE_ID()) -> {
                        selectedFile = pendingVCardBytes
                        SW_OK
                    }
                    else -> SW_FILE_NOT_FOUND
                }
            }
            isReadBinaryCommand(commandApdu) -> {
                val data = selectedFile ?: return SW_FILE_NOT_FOUND
                val offset = ((commandApdu[2].toInt() and 0xFF) shl 8) or (commandApdu[3].toInt() and 0xFF)
                val length = if (commandApdu.size == 5) {
                    val rawLe = commandApdu[4].toInt() and 0xFF
                    if (rawLe == 0) 256 else rawLe
                } else if (commandApdu.size >= 7 && commandApdu[4] == 0x00.toByte()) {
                    val extLe = ((commandApdu[5].toInt() and 0xFF) shl 8) or (commandApdu[6].toInt() and 0xFF)
                    if (extLe == 0) 65536 else extLe
                } else if (commandApdu.size >= 5) {
                    val rawLe = commandApdu[4].toInt() and 0xFF
                    if (rawLe == 0) 256 else rawLe
                } else {
                    return SW_UNKNOWN_CMD
                }

                if (selectedFile === pendingVCardBytes) {
                    if (!isScanActive.value) {
                        isScanActive.value = true
                    }
                    scanResetJob?.cancel()
                    scanResetJob = CoroutineScope(Dispatchers.Main).launch {
                        delay(1200L)
                        isScanActive.value = false
                    }
                }
                
                if (offset >= data.size) return SW_FILE_NOT_FOUND
                val end = minOf(offset + length, data.size)
                data.copyOfRange(offset, end) + SW_OK
            }
            else -> SW_UNKNOWN_CMD
        }
    }

    private fun NDDEF_FILE_ID(): ByteArray = NDEF_FILE_ID

    override fun onDeactivated(reason: Int) {
        selectedFile = null
        scanResetJob?.cancel()
        scanResetJob = CoroutineScope(Dispatchers.Main).launch {
            delay(400L)
            isScanActive.value = false
        }
    }

    private fun isSelectAidCommand(apdu: ByteArray): Boolean {
        if (apdu.size < SELECT_AID_PREFIX.size + NDEF_AID.size + 1) return false
        for (i in SELECT_AID_PREFIX.indices) {
            if (apdu[i] != SELECT_AID_PREFIX[i]) return false
        }
        val aidLen = apdu[4].toInt() and 0xFF
        if (aidLen != NDEF_AID.size) return false
        for (i in NDEF_AID.indices) {
            if (apdu[5 + i] != NDEF_AID[i]) return false
        }
        return true
    }

    private fun isSelectFileCommand(apdu: ByteArray): Boolean {
        return apdu.size >= 7 && apdu[0] == 0x00.toByte() && apdu[1] == 0xA4.toByte() && 
               apdu[2] == 0x00.toByte() && (apdu[3] == 0x0C.toByte() || apdu[3] == 0x00.toByte()) && 
               apdu[4] == 0x02.toByte()
    }

    private fun isReadBinaryCommand(apdu: ByteArray): Boolean =
        apdu.size >= 5 && apdu[0] == READ_BINARY_CMD_PREFIX[0] && apdu[1] == READ_BINARY_CMD_PREFIX[1]
}
