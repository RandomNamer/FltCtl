package com.example.fltctl.ui.home

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fltctl.AppMonitor
import com.example.fltctl.SettingKeys
import com.example.fltctl.settings
import com.example.fltctl.window.FloatingControlManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUIState(
    val enabled: Boolean = false,
    val isShowing: Boolean = false,
    val eInkModeEnabled: Boolean = false,
)

class HomeViewModel : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val _uiState = MutableStateFlow(HomeUIState())

    private val _isWindowShowing = MutableStateFlow(false)

    fun getUiState(context: Context): StateFlow<HomeUIState> {
        viewModelScope.launch {
            val settingsFlow = context.settings.data.map {
                _uiState.value.copy(
                    enabled = it[SettingKeys.ENABLED] ?: false,
                    eInkModeEnabled = it[SettingKeys.UI_EINK_MODE] ?: false
                )
            }
            combine(
                settingsFlow,
                _isWindowShowing
            ) { stateFromSetting, windowShowing ->
                stateFromSetting.copy(isShowing = windowShowing)
            }.catch {
                Log.e(TAG, "State error: $it")
            }.collect {
                _uiState.value = it
            }
        }
        return _uiState as StateFlow<HomeUIState>
    }

    fun toggleEnable(enable: Boolean) {
        viewModelScope.launch{ AppMonitor.appContext.settings.edit { it[SettingKeys.ENABLED] = enable } }
    }

    fun toggleShowWindow(show: Boolean) {
        if (show) FloatingControlManager.tryShowWindow {
            _isWindowShowing.value = it
        } else {
            FloatingControlManager.closeWindow()
            _isWindowShowing.value = false
        }
    }


}