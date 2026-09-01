package com.sameerasw.medrop.data.repository

import android.content.Context
import android.content.SharedPreferences

class MeDropRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getMeDropSettingsJson(): String? = prefs.getString(KEY_MEDROP_SETTINGS_JSON, null)

    fun setMeDropSettingsJson(json: String?) {
        prefs.edit().putString(KEY_MEDROP_SETTINGS_JSON, json).apply()
    }

    fun isMeDropAllowWhenLocked(): Boolean = prefs.getBoolean(KEY_MEDROP_ALLOW_WHEN_LOCKED, true)

    fun setMeDropAllowWhenLocked(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MEDROP_ALLOW_WHEN_LOCKED, enabled).apply()
    }

    fun isPitchBlackThemeEnabled(): Boolean = prefs.getBoolean(KEY_PITCH_BLACK_THEME, false)

    fun setPitchBlackThemeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PITCH_BLACK_THEME, enabled).apply()
    }

    fun isBlurEnabled(): Boolean = prefs.getBoolean(KEY_BLUR_ENABLED, true)

    fun setBlurEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLUR_ENABLED, enabled).apply()
    }

    fun isDeveloperModeEnabled(): Boolean = prefs.getBoolean(KEY_DEVELOPER_MODE_ENABLED, false)

    fun setDeveloperModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEVELOPER_MODE_ENABLED, enabled).apply()
    }

    fun isTileAdded(): Boolean = prefs.getBoolean(KEY_TILE_ADDED, false)

    fun setTileAdded(added: Boolean) {
        prefs.edit().putBoolean(KEY_TILE_ADDED, added).apply()
    }

    companion object {
        private const val PREFS_NAME = "medrop_preferences"
        const val KEY_MEDROP_SETTINGS_JSON = "medrop_settings_json"
        const val KEY_MEDROP_ALLOW_WHEN_LOCKED = "medrop_allow_when_locked"
        const val KEY_PITCH_BLACK_THEME = "pitch_black_theme"
        const val KEY_BLUR_ENABLED = "blur_enabled"
        const val KEY_DEVELOPER_MODE_ENABLED = "developer_mode_enabled"
        const val KEY_TILE_ADDED = "tile_added"
    }
}
