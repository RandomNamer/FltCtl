package com.example.fltctl

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

object SettingCache {

    private var latest: Preferences? = null

    operator fun <T> get(key: Preferences.Key<T>): T? = latest?.get(key)

    init {
        commonSettingScope.launch {
            delay(100)
            AppMonitor.appContext.settings.data.collectLatest {
                //same as debounce():
                delay(100)
                synchronized(this@SettingCache) {
                    latest = it
                }
            }
        }
    }
}