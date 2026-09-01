package com.sameerasw.medrop.viewmodels

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.sameerasw.medrop.data.repository.MeDropRepository
import com.sameerasw.medrop.domain.model.MeDropContact
import com.sameerasw.medrop.domain.model.MeDropProfile
import com.sameerasw.medrop.domain.model.MeDropProfileType
import com.sameerasw.medrop.domain.model.MeDropSettings
import com.sameerasw.medrop.utils.PermissionUtils

class MeDropViewModel : ViewModel() {
    val meDropSettings = mutableStateOf<MeDropSettings?>(null)
    val isMeDropAllowWhenLocked = mutableStateOf(true)
    val isPitchBlackThemeEnabled = mutableStateOf(false)
    val isBlurEnabled = mutableStateOf(true)
    val isDeveloperModeEnabled = mutableStateOf(false)
    val hasContactsPermission = mutableStateOf(false)
    val isTileAdded = mutableStateOf(false)

    fun check(context: Context) {
        val repo = MeDropRepository(context)
        isPitchBlackThemeEnabled.value = repo.isPitchBlackThemeEnabled()
        isBlurEnabled.value = repo.isBlurEnabled()
        isDeveloperModeEnabled.value = repo.isDeveloperModeEnabled()
        hasContactsPermission.value = PermissionUtils.hasContactsPermission(context)
        updateTileState(context)
        loadMeDropSettings(context)
    }

    fun updateTileState(context: Context) {
        var added = false
        try {
            val tilesString = android.provider.Settings.Secure.getString(context.contentResolver, "sysui_qs_tiles") ?: ""
            if (tilesString.contains("com.sameerasw.medrop/.services.tiles.MeDropTileService") ||
                tilesString.contains("com.sameerasw.medrop")
            ) {
                added = true
                MeDropRepository(context).setTileAdded(true)
            }
        } catch (_: Exception) {}

        if (!added) {
            added = MeDropRepository(context).isTileAdded()
        }
        isTileAdded.value = added
    }

    fun setTileAdded(context: Context, added: Boolean) {
        MeDropRepository(context).setTileAdded(added)
        isTileAdded.value = added
    }

    fun setPitchBlackTheme(context: Context, enabled: Boolean) {
        MeDropRepository(context).setPitchBlackThemeEnabled(enabled)
        isPitchBlackThemeEnabled.value = enabled
    }

    fun setBlurEnabled(context: Context, enabled: Boolean) {
        MeDropRepository(context).setBlurEnabled(enabled)
        isBlurEnabled.value = enabled
    }

    fun setDeveloperModeEnabled(context: Context, enabled: Boolean) {
        MeDropRepository(context).setDeveloperModeEnabled(enabled)
        isDeveloperModeEnabled.value = enabled
    }

    fun loadMeDropSettings(context: Context) {
        val repo = MeDropRepository(context)
        val json = repo.getMeDropSettingsJson()
        meDropSettings.value =
            if (json != null) {
                try {
                    Gson().fromJson(json, MeDropSettings::class.java)
                } catch (_: Exception) {
                    MeDropSettings()
                }
            } else {
                MeDropSettings()
            }
        isMeDropAllowWhenLocked.value = repo.isMeDropAllowWhenLocked()
        com.sameerasw.medrop.services.MeDropWearSyncManager.syncProfiles(context)
    }

    fun saveMeDropSettings(
        context: Context,
        settings: MeDropSettings?,
    ) {
        meDropSettings.value = settings
        val json = if (settings != null) Gson().toJson(settings) else null
        MeDropRepository(context).setMeDropSettingsJson(json)
        com.sameerasw.medrop.services.MeDropWearSyncManager.syncProfiles(context)
    }

    fun setMeDropContact(
        context: Context,
        contact: MeDropContact?,
    ) {
        val current = meDropSettings.value ?: MeDropSettings()
        val updated = current.copy(contact = contact)
        saveMeDropSettings(context, updated)
    }

    fun setMeDropAllowWhenLocked(context: Context, enabled: Boolean) {
        MeDropRepository(context).setMeDropAllowWhenLocked(enabled)
        isMeDropAllowWhenLocked.value = enabled
        val current = meDropSettings.value ?: MeDropSettings()
        saveMeDropSettings(context, current.copy(allowWhenLocked = enabled))
    }

    fun setMeDropProfileEnabled(
        context: Context,
        type: MeDropProfileType,
        enabled: Boolean,
    ) {
        val current = meDropSettings.value ?: MeDropSettings()
        val profile = current.getProfile(type).copy(enabled = enabled)
        val updated = current.updateProfile(profile)
        saveMeDropSettings(context, updated)
    }

    fun setMeDropActiveProfile(
        context: Context,
        type: MeDropProfileType,
    ) {
        val current = meDropSettings.value ?: MeDropSettings()
        val updated = current.copy(activeProfileType = type)
        saveMeDropSettings(context, updated)
    }

    fun setMeDropShowRipple(context: Context, enabled: Boolean) {
        val current = meDropSettings.value ?: MeDropSettings()
        val updated = current.copy(showRipple = enabled)
        saveMeDropSettings(context, updated)
    }

    fun setMeDropUsePhotoForAll(context: Context, enabled: Boolean) {
        val current = meDropSettings.value ?: MeDropSettings()
        val updated = current.copy(usePhotoForAll = enabled)
        saveMeDropSettings(context, updated)
    }

    fun setMeDropEnableReceiving(context: Context, enabled: Boolean) {
        val current = meDropSettings.value ?: MeDropSettings()
        val updated = current.copy(enableReceiving = enabled)
        saveMeDropSettings(context, updated)
    }

    fun toggleMeDropProfileEntry(
        context: Context,
        type: MeDropProfileType,
        entryId: String,
        enabled: Boolean,
    ) {
        val current = meDropSettings.value ?: MeDropSettings()
        val effective = current.getEffectiveEntryIds(type).toMutableSet()
        if (enabled) {
            effective.add(entryId)
        } else {
            effective.remove(entryId)
        }
        val profile = current.getProfile(type).copy(selectedEntryIds = effective)
        val updated = current.updateProfile(profile)
        saveMeDropSettings(context, updated)
    }

    fun updateMeDropProfilePhoto(
        context: Context,
        type: MeDropProfileType,
        photoUri: String?,
    ) {
        MeDropContact.clearPhotoCache()
        val current = meDropSettings.value ?: MeDropSettings()
        val profile = current.getProfile(type).copy(photoUri = photoUri)
        val updated = current.updateProfile(profile)
        saveMeDropSettings(context, updated)
    }

    fun updateMeDropProfileDisplayName(
        context: Context,
        type: MeDropProfileType,
        customName: String?,
    ) {
        val current = meDropSettings.value ?: MeDropSettings()
        val profile = current.getProfile(type).copy(customDisplayName = customName?.takeIf { it.isNotBlank() })
        val updated = current.updateProfile(profile)
        saveMeDropSettings(context, updated)
    }

    fun updateMeDropProfileFieldValue(
        context: Context,
        type: MeDropProfileType,
        fieldId: String,
        value: String?,
    ) {
        val current = meDropSettings.value ?: MeDropSettings()
        val profile = current.getProfile(type)
        val currentOverrides = profile.customFieldOverrides.toMutableMap()
        if (value.isNullOrBlank()) {
            currentOverrides.remove(fieldId)
        } else {
            currentOverrides[fieldId] = value
        }
        val updatedProfile = profile.copy(customFieldOverrides = currentOverrides)
        val updated = current.updateProfile(updatedProfile)
        saveMeDropSettings(context, updated)
    }
}
