package com.example.fltctl

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Settings & Keys
 */
private const val SETTINGS = "settings"
val Context.settings by preferencesDataStore(name = SETTINGS)

/**
 * For scenarios that don't have a reused scope
 */
val commonSettingScope = CoroutineScope(Dispatchers.IO)


object SettingKeys {
    val UI_EINK_MODE = booleanPreferencesKey("eink_ui")
    val WINDOW_ADHERE_TO_EDGE = booleanPreferencesKey("window_adhere_to_edge")
    val CURRENT_CONTROL = stringPreferencesKey("current_control")
    val ENABLED = booleanPreferencesKey("enabled")
    val LAST_WINDOW_POSITION_X = floatPreferencesKey("wnd_pos_x")
    val LAST_WINDOW_POSITION_Y = floatPreferencesKey("wnd_pos_y")
}