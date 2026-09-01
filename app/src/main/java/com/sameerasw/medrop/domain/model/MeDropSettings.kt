package com.sameerasw.medrop.domain.model

data class MeDropSettings(
    val contact: MeDropContact? = null,
    val contactProfile: MeDropProfile = MeDropProfile(MeDropProfileType.CONTACT, enabled = true),
    val professionalProfile: MeDropProfile = MeDropProfile(MeDropProfileType.PROFESSIONAL, enabled = false),
    val customProfile: MeDropProfile = MeDropProfile(MeDropProfileType.CUSTOM, enabled = false),
    val usePhotoForAll: Boolean = true,
    val allowWhenLocked: Boolean = true,
    val showRipple: Boolean = true,
    val enableReceiving: Boolean = false,
    val activeProfileType: MeDropProfileType = MeDropProfileType.CONTACT
) {
    fun getProfile(type: MeDropProfileType): MeDropProfile {
        return when (type) {
            MeDropProfileType.CONTACT -> contactProfile
            MeDropProfileType.PROFESSIONAL -> professionalProfile
            MeDropProfileType.CUSTOM -> customProfile
        }
    }

    fun updateProfile(profile: MeDropProfile): MeDropSettings {
        return when (profile.type) {
            MeDropProfileType.CONTACT -> copy(contactProfile = profile)
            MeDropProfileType.PROFESSIONAL -> copy(professionalProfile = profile)
            MeDropProfileType.CUSTOM -> copy(customProfile = profile)
        }
    }

    fun getEffectiveDisplayName(type: MeDropProfileType): String {
        val safeContactName = contact?.displayName ?: ""
        if (type == MeDropProfileType.CONTACT) return safeContactName
        val profile = getProfile(type)
        return profile.customDisplayName?.takeIf { it.isNotBlank() } ?: safeContactName
    }

    fun getEffectiveFieldValue(type: MeDropProfileType, fieldId: String, defaultValue: String?): String? {
        val profile = getProfile(type)
        if (profile.customFieldOverrides != null && profile.customFieldOverrides.containsKey(fieldId)) {
            return profile.customFieldOverrides[fieldId]
        }
        return defaultValue
    }

    fun getEffectivePhotoUri(type: MeDropProfileType): String? {
        return if (usePhotoForAll) {
            contactProfile.photoUri ?: contact?.photoUri
        } else {
            getProfile(type).photoUri ?: (if (type == MeDropProfileType.CONTACT) contact?.photoUri else null)
        }
    }

    fun getEffectiveEntryIds(type: MeDropProfileType): Set<String> {
        val profile = getProfile(type)
        if (profile.selectedEntryIds != null) {
            return profile.selectedEntryIds
        }
        val safe = contact ?: return emptySet()
        return when (type) {
            MeDropProfileType.CONTACT -> safe.getDefaultContactEntryIds()
            MeDropProfileType.PROFESSIONAL -> safe.getDefaultProfessionalEntryIds()
            MeDropProfileType.CUSTOM -> safe.getDefaultContactEntryIds()
        }
    }

    fun isEntrySelected(type: MeDropProfileType, entryId: String): Boolean {
        return getEffectiveEntryIds(type).contains(entryId)
    }
}
