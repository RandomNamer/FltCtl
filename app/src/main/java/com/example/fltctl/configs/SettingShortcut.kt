package com.example.fltctl.configs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException


object SettingsCache {

    private lateinit var dataStore: DataStore<Preferences>

    private val _preferencesFlow = MutableStateFlow<Preferences>(emptyPreferences())
    val preferencesFlow: StateFlow<Preferences> = _preferencesFlow.asStateFlow()

    fun init(context: Context) {
        dataStore = context.settings

        // Collect data store changes and update the cache
        commonSettingScope.launch {
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }
                .collect { preferences ->
                    _preferencesFlow.value = preferences
                }
        }
    }

    operator fun <T> get(key: Preferences.Key<T>): T? {
        return preferencesFlow.value[key]
    }

    // Asynchronous set method
    operator fun <T> set(key: Preferences.Key<T>, value: T) {
        setAfter(key, value) {}
    }

    internal fun <T> setAfter(key: Preferences.Key<T>, value: T, block: suspend () -> Unit) {
        commonSettingScope.launch {
            block.invoke()
            dataStore.edit { settings ->
                settings[key] = value
            }
        }
    }

}