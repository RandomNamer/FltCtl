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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUIState(
    val enabled: Boolean = false,
    val isShowing: Boolean = false,
    val eInkModeEnabled: Boolean = false,
    val controlSelectionList: List<ControlSelection> = listOf()
)

data class StateFromSetting(
    val enabled: Boolean = false,
    val eInkModeEnabled: Boolean = false
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

    private var allPermissionsClear = false

    init {
        viewModelScope.launch (Dispatchers.IO) {
            val settingsFlow = AppMonitor.appContext.settings.data.map {
                StateFromSetting(
                    enabled = it[SettingKeys.ENABLED] ?: false,
                    eInkModeEnabled = it[SettingKeys.UI_EINK_MODE] ?: false
                )
            }
            combine(
                settingsFlow,
                _isWindowShowing,
                FloatingControlManager.controlStateFlow,
            ) { stateFromSetting, windowShowing, controlList ->
                _uiState.value.copy(
                    enabled = stateFromSetting.enabled && allPermissionsClear,
                    isShowing = windowShowing,
                    controlSelectionList = controlList,
                    eInkModeEnabled = stateFromSetting.eInkModeEnabled
                )
            }.catch {
                Log.e(TAG, "State error: $it")
            }.distinctUntilChanged().collect {
                Log.d(TAG, "State mutate: $it")
                _uiState.value = it
            }
        }
        viewModelScope.launch(Dispatchers.Main) {
            uiState.map { it.enabled }.distinctUntilChanged().collect {
                onEnableStateChanged(it)
            }
        }

    }

    fun toggleEnable(enable: Boolean) {
        viewModelScope.launch{ AppMonitor.appContext.settings.edit { it[SettingKeys.ENABLED] = enable } }
    }

    private fun onEnableStateChanged(enable: Boolean) {
        if (enable) {
            if (_isWindowShowing.value) FloatingControlManager.tryShowWindowWithCurrentControl()
        } else {
            FloatingControlManager.closeWindow()
            //Which window show state (checkbox) should not change
        }
    }

    fun toggleShowWindow(show: Boolean) {
        if (show) FloatingControlManager.tryShowWindowWithCurrentControl {
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

    fun onResumeCheckersComplete(
        hasOverlaysPermission: Boolean,
        hasAccessibilityPermission: Boolean,
    ) {
        allPermissionsClear = hasAccessibilityPermission && hasOverlaysPermission
        viewModelScope.launch {
            if (allPermissionsClear) AppMonitor.appContext.settings.data.collectLatest {
                _uiState.value = _uiState.value.copy(enabled = it[SettingKeys.ENABLED] ?: false)
            }
        }
    }
}