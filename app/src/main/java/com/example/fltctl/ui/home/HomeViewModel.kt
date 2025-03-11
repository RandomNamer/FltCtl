package com.example.fltctl.ui.home

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fltctl.AppMonitor
import com.example.fltctl.configs.SettingKeys
import com.example.fltctl.configs.settings
import com.example.fltctl.controls.arch.FloatingControlManager
import com.example.fltctl.utils.logs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class HomeUIState(
    val enabled: Boolean = false,
    val isShowing: Boolean = false,
    val eInkModeEnabled: Boolean = false,
    val controlSelectionList: List<ControlSelection> = listOf()
)

data class StateFromSetting(
    val enabled: Boolean = false,
    val eInkModeEnabled: Boolean = false,
    val alwaysShowWindow: Boolean = false
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

    private var _isWindowShowing = false

    private var allPermissionsClear = false

    private val log by logs(TAG)

    init {
        viewModelScope.launch (Dispatchers.IO) {
            val settingsFlow = AppMonitor.appContext.settings.data.map {
                StateFromSetting(
                    enabled = it[SettingKeys.ENABLED] ?: false,
                    eInkModeEnabled = it[SettingKeys.UI_EINK_MODE] ?: false,
                    alwaysShowWindow = it[SettingKeys.ALWAYS_SHOW_WINDOW]?: false
                )
            }
            combine(
                settingsFlow,
                FloatingControlManager.controlStateFlow,
            ) { stateFromSetting, controlList ->
                _uiState.value.copy(
                    enabled = stateFromSetting.enabled && allPermissionsClear,
                    isShowing = stateFromSetting.alwaysShowWindow,
                    controlSelectionList = controlList,
                    eInkModeEnabled = stateFromSetting.eInkModeEnabled
                )
            }.catch {
                log.e("State error: $it")
            }.distinctUntilChanged().collect {
                log.d("State mutate: $it")
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
            if (_isWindowShowing) FloatingControlManager.tryShowWindowWithCurrentControl()
        } else {
            FloatingControlManager.closeWindow()
            //Which window show state (checkbox) should not change
        }
    }

    fun toggleShowWindow(show: Boolean) {
        viewModelScope.launch { AppMonitor.appContext.settings.edit { it[SettingKeys.ALWAYS_SHOW_WINDOW] = show } }
        if (show) {
            FloatingControlManager.tryShowWindowWithCurrentControl()
        } else {
            FloatingControlManager.closeWindow()
        }
    }

    fun onSelectControl(key: String) {
        viewModelScope.launch {
            AppMonitor.appContext.settings.edit { it[SettingKeys.CURRENT_CONTROL] = key }
        }
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