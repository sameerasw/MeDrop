package com.sameerasw.medrop.services

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import com.sameerasw.medrop.data.repository.MeDropRepository
import com.sameerasw.medrop.domain.model.MeDropProfileType
import com.sameerasw.medrop.domain.model.MeDropSettings

object MeDropWearSyncManager {
    private const val TAG = "MeDropWearSyncManager"
    const val SYNC_PATH = "/medrop_profiles"
    const val KEY_PROFILES_JSON = "profiles_json"
    const val KEY_TIMESTAMP = "timestamp"

    data class WearProfileItem(
        val type: String,
        val title: String,
        val displayName: String,
        val iconResName: String,
        val isEnabled: Boolean,
        val isActive: Boolean,
    )

    fun syncProfiles(context: Context) {
        try {
            val repo = MeDropRepository(context)
            val json = repo.getMeDropSettingsJson() ?: return
            val settings = try {
                Gson().fromJson(json, MeDropSettings::class.java)
            } catch (_: Exception) {
                null
            } ?: return

            val contact = settings.contact ?: return

            val items = mutableListOf<WearProfileItem>()

            // Contact Profile
            if (settings.contactProfile.enabled) {
                items.add(
                    WearProfileItem(
                        type = MeDropProfileType.CONTACT.name,
                        title = "Contact",
                        displayName = settings.getEffectiveDisplayName(MeDropProfileType.CONTACT),
                        iconResName = "medrop_logo",
                        isEnabled = true,
                        isActive = settings.activeProfileType == MeDropProfileType.CONTACT,
                    )
                )
            }

            // Professional Profile
            if (settings.professionalProfile.enabled) {
                items.add(
                    WearProfileItem(
                        type = MeDropProfileType.PROFESSIONAL.name,
                        title = "Professional",
                        displayName = settings.getEffectiveDisplayName(MeDropProfileType.PROFESSIONAL),
                        iconResName = "rounded_work_24",
                        isEnabled = true,
                        isActive = settings.activeProfileType == MeDropProfileType.PROFESSIONAL,
                    )
                )
            }

            // Custom Profile
            if (settings.customProfile.enabled) {
                items.add(
                    WearProfileItem(
                        type = MeDropProfileType.CUSTOM.name,
                        title = "Custom",
                        displayName = settings.getEffectiveDisplayName(MeDropProfileType.CUSTOM),
                        iconResName = "rounded_id_card_24",
                        isEnabled = true,
                        isActive = settings.activeProfileType == MeDropProfileType.CUSTOM,
                    )
                )
            }

            val payloadJson = Gson().toJson(items)
            val putDataMapReq = PutDataMapRequest.create(SYNC_PATH).apply {
                dataMap.putString(KEY_PROFILES_JSON, payloadJson)
                dataMap.putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            }
            val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
            Wearable.getDataClient(context.applicationContext).putDataItem(putDataReq)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing profiles to WearOS", e)
        }
    }
}
