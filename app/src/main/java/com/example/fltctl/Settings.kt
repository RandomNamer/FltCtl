package com.example.fltctl

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
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
    val ACTIVITY_LIST_SORTING = booleanPreferencesKey("act_list_sort")
    val APP_LIST_AGGRESSIVE_SEARCH = booleanPreferencesKey("app_select_search_perf")
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

object EInkDeviceConfigs {
    @JvmStatic
    private val eInkBrands = listOf("Onyx", "iReader", "Hanvon")

    @JvmStatic
    private fun matchByManufacturer(): Boolean {
        //TODO: pattern matching
        return eInkBrands.contains(Build.BRAND) || eInkBrands.contains(Build.MANUFACTURER)
    }

    init {
        if (matchByManufacturer()) {
            commonSettingScope.launch {
                delay(500)
                AppMonitor.appContext.settings.edit { it[SettingKeys.UI_EINK_MODE] = true }
            }
        }
    }

}