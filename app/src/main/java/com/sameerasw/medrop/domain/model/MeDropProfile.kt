package com.sameerasw.medrop.domain.model

enum class MeDropProfileType {
    CONTACT,
    PROFESSIONAL,
    CUSTOM
}

data class MeDropProfile(
    val type: MeDropProfileType,
    val enabled: Boolean = true,
    val photoUri: String? = null,
    val selectedEntryIds: Set<String>? = null
)
