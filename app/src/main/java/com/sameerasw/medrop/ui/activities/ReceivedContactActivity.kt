package com.sameerasw.medrop.ui.activities

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.medrop.ui.sheets.ReceivedContactBottomSheet
import com.sameerasw.medrop.ui.theme.MeDropTheme
import com.sameerasw.medrop.utils.ReceivedContact
import com.sameerasw.medrop.utils.VCardParser
import com.sameerasw.medrop.viewmodels.MeDropViewModel

class ReceivedContactActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        var parsedContact: ReceivedContact? = null
        intent?.let { int ->
            if (int.action == android.nfc.NfcAdapter.ACTION_NDEF_DISCOVERED) {
                val rawMsgs = int.getParcelableArrayExtra(android.nfc.NfcAdapter.EXTRA_NDEF_MESSAGES)
                if (rawMsgs != null) {
                    for (raw in rawMsgs) {
                        val msg = raw as? android.nfc.NdefMessage ?: continue
                        for (rec in msg.records) {
                            val text = String(rec.payload, Charsets.UTF_8)
                            if (text.contains("BEGIN:VCARD", ignoreCase = true)) {
                                val start = text.indexOf("BEGIN:VCARD", ignoreCase = true)
                                val clean = text.substring(start)
                                parsedContact = VCardParser.parse(clean)
                                if (parsedContact != null) break
                            }
                        }
                    }
                }
            }
        }

        if (parsedContact == null) {
            finish()
            return
        }

        setContent {
            val mainViewModel: MeDropViewModel = viewModel()
            val isPitchBlackThemeEnabled by mainViewModel.isPitchBlackThemeEnabled
            val context = LocalContext.current

            LaunchedEffect(Unit) {
                mainViewModel.check(context)
            }

            var contactToDisplay by remember { mutableStateOf<ReceivedContact?>(parsedContact) }

            MeDropTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                contactToDisplay?.let { contact ->
                    ReceivedContactBottomSheet(
                        contact = contact,
                        onDismissRequest = {
                            contactToDisplay = null
                            finish()
                        }
                    )
                }
            }
        }
    }
}
