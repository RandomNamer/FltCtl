package com.example.fltctl

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/**
 * Settings & Keys
 */
private const val SETTINGS = "settings"
val Context.settings by preferencesDataStore(name = SETTINGS)

object SettingKeys {
    val UI_EINK_MODE = booleanPreferencesKey("eink_ui")
    val WINDOW_ADHERE_TO_EDGE = booleanPreferencesKey("window_adhere_to_edge")
    val CONTROL_SETTINGS_LIST = stringPreferencesKey("control_settings_list")
    val ENABLED = booleanPreferencesKey("enabled")
}