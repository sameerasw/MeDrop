package com.sameerasw.medrop.utils

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import com.sameerasw.medrop.domain.model.MeDropSettings
import com.sameerasw.medrop.services.MeDropHceService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MeDropNfcManager {

    fun isNfcAvailable(context: Context): Boolean =
        NfcAdapter.getDefaultAdapter(context) != null

    fun isNfcEnabled(context: Context): Boolean =
        NfcAdapter.getDefaultAdapter(context)?.isEnabled == true

    suspend fun startBroadcast(activity: Activity, settings: MeDropSettings) {
        val contact = settings.contact ?: return
        val activeType = settings.activeProfileType
        val activeEntries = settings.getEffectiveEntryIds(activeType)
        val photoUri = settings.getEffectivePhotoUri(activeType)
        val context = activity.applicationContext

        val profile = settings.getProfile(activeType)
        val vCard = withContext(Dispatchers.IO) {
            contact.toVCard(
                context = context,
                activeEntryIds = activeEntries,
                customPhotoUri = photoUri,
                customDisplayName = if (activeType != com.sameerasw.medrop.domain.model.MeDropProfileType.CONTACT) profile.customDisplayName else null,
                fieldOverrides = profile.customFieldOverrides
            )
        }

        withContext(Dispatchers.IO) {
            MeDropHceService.prepareVCard(vCard, settings.enableIPhoneSupport, settings.shareAsVCard)
        }

        withContext(Dispatchers.Main) {
            val component = ComponentName(context, MeDropHceService::class.java)
            val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
            if (nfcAdapter != null && !activity.isFinishing && !activity.isDestroyed) {
                try {
                    val cardEmulation = CardEmulation.getInstance(nfcAdapter)
                    cardEmulation.setPreferredService(activity, component)
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun stopBroadcast(activity: Activity) {
        val context = activity.applicationContext

        withContext(Dispatchers.Main) {
            val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
            if (nfcAdapter != null) {
                try {
                    val cardEmulation = CardEmulation.getInstance(nfcAdapter)
                    cardEmulation.unsetPreferredService(activity)
                } catch (_: Exception) {}
            }
        }

        withContext(Dispatchers.IO) {
            MeDropHceService.clearVCard()
        }
    }

    suspend fun startBroadcast(context: Context, settings: MeDropSettings) {
        if (context is Activity) {
            startBroadcast(context, settings)
        } else {
            val contact = settings.contact ?: return
            val activeType = settings.activeProfileType
            val activeEntries = settings.getEffectiveEntryIds(activeType)
            val photoUri = settings.getEffectivePhotoUri(activeType)

            val profile = settings.getProfile(activeType)
            val vCard = withContext(Dispatchers.IO) {
                contact.toVCard(
                    context = context,
                    activeEntryIds = activeEntries,
                    customPhotoUri = photoUri,
                    customDisplayName = if (activeType != com.sameerasw.medrop.domain.model.MeDropProfileType.CONTACT) profile.customDisplayName else null,
                    fieldOverrides = profile.customFieldOverrides
                )
            }

            withContext(Dispatchers.IO) {
                MeDropHceService.prepareVCard(vCard, settings.enableIPhoneSupport, settings.shareAsVCard)
            }
        }
    }

    suspend fun stopBroadcast(context: Context) {
        if (context is Activity) {
            stopBroadcast(context)
        } else {
            withContext(Dispatchers.IO) {
                MeDropHceService.clearVCard()
            }
        }
    }

    fun enableReaderMode(activity: Activity, onVCardReceived: (String) -> Unit) {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS

        try {
            nfcAdapter.enableReaderMode(activity, { tag ->
                try {
                    // Try reading standard NDEF first
                    val ndef = android.nfc.tech.Ndef.get(tag)
                    if (ndef != null) {
                        ndef.connect()
                        val ndefMessage = ndef.ndefMessage
                        ndef.close()
                        if (ndefMessage != null) {
                            for (record in ndefMessage.records) {
                                val payload = record.payload
                                val text = String(payload, Charsets.UTF_8)
                                if (text.contains("BEGIN:VCARD", ignoreCase = true)) {
                                    val vcardStartIndex = text.indexOf("BEGIN:VCARD", ignoreCase = true)
                                    val cleanVCard = text.substring(vcardStartIndex)
                                    activity.runOnUiThread {
                                        onVCardReceived(cleanVCard)
                                    }
                                    return@enableReaderMode
                                }
                            }
                        }
                    }

                    // Fallback to ISO-DEP APDU commands for MeDrop HCE transmitters
                    val isoDep = android.nfc.tech.IsoDep.get(tag)
                    if (isoDep != null) {
                        isoDep.connect()
                        // Select NDEF Application AID: D2 76 00 00 85 01 01
                        val selectAid = byteArrayOf(
                            0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), 0x07.toByte(),
                            0xD2.toByte(), 0x76.toByte(), 0x00.toByte(), 0x00.toByte(), 0x85.toByte(), 0x01.toByte(), 0x01.toByte()
                        )
                        val respAid = isoDep.transceive(selectAid)
                        if (respAid.size >= 2 && respAid[respAid.size - 2] == 0x90.toByte()) {
                            // Select NDEF file (0xE1, 0x04)
                            val selectFile = byteArrayOf(
                                0x00.toByte(), 0xA4.toByte(), 0x00.toByte(), 0x0C.toByte(), 0x02.toByte(),
                                0xE1.toByte(), 0x04.toByte()
                            )
                            val respFile = isoDep.transceive(selectFile)
                            if (respFile.size >= 2 && respFile[respFile.size - 2] == 0x90.toByte()) {
                                // Read NLEN (first 2 bytes)
                                val readNlen = byteArrayOf(0x00.toByte(), 0xB0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x02.toByte())
                                val nlenResp = isoDep.transceive(readNlen)
                                if (nlenResp.size >= 4 && nlenResp[nlenResp.size - 2] == 0x90.toByte()) {
                                    val nlen = ((nlenResp[0].toInt() and 0xFF) shl 8) or (nlenResp[1].toInt() and 0xFF)
                                    if (nlen in 1..65535) {
                                        var offset = 2
                                        var remaining = nlen
                                        val fullData = java.io.ByteArrayOutputStream()
                                        while (remaining > 0) {
                                            val chunkSize = minOf(remaining, 240)
                                            val readChunk = byteArrayOf(
                                                0x00.toByte(), 0xB0.toByte(),
                                                ((offset shr 8) and 0xFF).toByte(),
                                                (offset and 0xFF).toByte(),
                                                (chunkSize and 0xFF).toByte()
                                            )
                                            val chunkResp = isoDep.transceive(readChunk)
                                            if (chunkResp.size >= 2 && chunkResp[chunkResp.size - 2] == 0x90.toByte()) {
                                                fullData.write(chunkResp, 0, chunkResp.size - 2)
                                                offset += chunkSize
                                                remaining -= chunkSize
                                            } else {
                                                break
                                            }
                                        }
                                        val fullNdefBytes = fullData.toByteArray()
                                        if (fullNdefBytes.isNotEmpty()) {
                                            try {
                                                val msg = android.nfc.NdefMessage(fullNdefBytes)
                                                for (rec in msg.records) {
                                                    val raw = String(rec.payload, Charsets.UTF_8)
                                                    if (raw.contains("BEGIN:VCARD", ignoreCase = true)) {
                                                        val start = raw.indexOf("BEGIN:VCARD", ignoreCase = true)
                                                        val clean = raw.substring(start)
                                                        activity.runOnUiThread {
                                                            onVCardReceived(clean)
                                                        }
                                                        isoDep.close()
                                                        return@enableReaderMode
                                                    }
                                                }
                                            } catch (_: Exception) {
                                                val raw = String(fullNdefBytes, Charsets.UTF_8)
                                                if (raw.contains("BEGIN:VCARD", ignoreCase = true)) {
                                                    val start = raw.indexOf("BEGIN:VCARD", ignoreCase = true)
                                                    val clean = raw.substring(start)
                                                    activity.runOnUiThread {
                                                        onVCardReceived(clean)
                                                    }
                                                    isoDep.close()
                                                    return@enableReaderMode
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        isoDep.close()
                    }
                } catch (_: Exception) {}
            }, flags, null)
        } catch (_: Exception) {}
    }

    fun disableReaderMode(activity: Activity) {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        try {
            nfcAdapter.disableReaderMode(activity)
        } catch (_: Exception) {}
    }
}
