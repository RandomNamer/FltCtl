package com.example.fltctl.configs

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.fltctl.AppMonitor
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Settings & Keys
 */
private const val SETTINGS = "settings"
val Context.settings by preferencesDataStore(name = SETTINGS)

/**
 * For scenarios that don't have a reused scope, eats exceptions by default
 */
val commonSettingScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, t ->
    Log.e("SettingsReusedScope", t.toString())
})


object SettingKeys {
    val UI_EINK_MODE = booleanPreferencesKey("eink_ui")
    val WINDOW_ADHERE_TO_EDGE = booleanPreferencesKey("window_adhere_to_edge")
    val CURRENT_CONTROL = stringPreferencesKey("current_control")
    val ENABLED = booleanPreferencesKey("enabled")
    val ALWAYS_SHOW_WINDOW = booleanPreferencesKey("always_show_window")
    val LAST_WINDOW_POSITION_X = floatPreferencesKey("wnd_pos_x")
    val LAST_WINDOW_POSITION_Y = floatPreferencesKey("wnd_pos_y")
    val ACTIVITY_LIST_SORTING = booleanPreferencesKey("act_list_sort")
    val APP_LIST_AGGRESSIVE_SEARCH = booleanPreferencesKey("app_select_search_perf")
}
