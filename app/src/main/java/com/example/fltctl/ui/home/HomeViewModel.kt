package com.example.fltctl.ui.home

import android.content.Context
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fltctl.AppMonitor
import com.example.fltctl.SettingKeys
import com.example.fltctl.controls.arch.FloatingControl
import com.example.fltctl.settings
import com.example.fltctl.controls.arch.FloatingControlManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUIState(
    val enabled: Boolean = false,
    val isShowing: Boolean = false,
    val eInkModeEnabled: Boolean = false,
    val controlSelectionList: List<ControlSelection> = listOf()
)

data class ControlSelection(
    val displayTitle: String,
    val key: String,
    val selected: Boolean
)

class HomeViewModel : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    val uiState: StateFlow<HomeUIState>
        get() = _uiState

    private val _uiState = MutableStateFlow(HomeUIState())

    private val _isWindowShowing = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            val settingsFlow = AppMonitor.appContext.settings.data.map {
                _uiState.value.copy(
                    enabled = it[SettingKeys.ENABLED] ?: false,
                    eInkModeEnabled = it[SettingKeys.UI_EINK_MODE] ?: false
                )
            }
            combine(
                settingsFlow,
                _isWindowShowing,
                FloatingControlManager.controlStateFlow,
            ) { stateFromSetting, windowShowing, controlList ->
                stateFromSetting.copy(isShowing = windowShowing, controlSelectionList = controlList)
            }.catch {
                Log.e(TAG, "State error: $it")
            }.collect {
                _uiState.value = it
            }
        }
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

    fun onSelectControl(key: String) {
        FloatingControlManager.selectControl(key)
    }

    fun onClickConfigureControl(key: String, context: Context) {
        FloatingControlManager.openConfigureControlPage("", context)
    }


}