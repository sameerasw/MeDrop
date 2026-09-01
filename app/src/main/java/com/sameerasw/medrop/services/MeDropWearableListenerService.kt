package com.sameerasw.medrop.services

import android.content.Intent
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.sameerasw.medrop.MainActivity
import com.sameerasw.medrop.domain.model.MeDropProfileType
import com.sameerasw.medrop.viewmodels.MeDropViewModel

class MeDropWearableListenerService : WearableListenerService() {
    companion object {
        const val PATH_REQUEST_SYNC = "/request_medrop_sync"
        const val PATH_OPEN_APP = "/open_medrop_app"
        const val PATH_SET_ACTIVE_PROFILE = "/set_active_profile"
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        when (messageEvent.path) {
            PATH_REQUEST_SYNC -> {
                MeDropWearSyncManager.syncProfiles(this)
            }
            PATH_OPEN_APP -> {
                val intent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(intent)
            }
            PATH_SET_ACTIVE_PROFILE -> {
                try {
                    val typeStr = String(messageEvent.data)
                    val profileType = MeDropProfileType.valueOf(typeStr)
                    val vm = MeDropViewModel()
                    vm.loadMeDropSettings(this)
                    vm.setMeDropActiveProfile(this, profileType)
                } catch (_: Exception) {}
            }
        }
    }
}
