package com.example.fltctl.appselection.ui

import android.content.pm.ActivityInfo
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fltctl.AppMonitor
import com.example.fltctl.SettingKeys
import com.example.fltctl.appselection.model.AppInfo
import com.example.fltctl.appselection.model.AppInfoCache
import com.example.fltctl.appselection.model.ConciseActivityInfo
import com.example.fltctl.appselection.model.filter
import com.example.fltctl.settings
import com.example.fltctl.ui.toast
import com.example.fltctl.widgets.composable.DualStateListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.exp

data class PackageFilterCriterion(
    val searchKeyword: String? = null,
    val includeSystem: Boolean = false,
    val includeNoExportedActivity: Boolean = false,
)

data class AppInfoWithSelection(
    val value: AppInfo,
    val selected: Boolean = false
)

data class ActivitySelectDialogUiState(
    val show: Boolean = false,
    val origin: AppInfo = AppInfo.EMPTY,
    val list: List<DualStateListItem<ConciseActivityInfo>> = listOf(),
//    val activityCount: Int = 0,
    //Computed once
    val exportedCount: Int = 0
)

data class AppSelectUiState(
    val appList: List<AppInfoWithSelection> = listOf(),
    val filter: PackageFilterCriterion = PackageFilterCriterion(),
    val counts: Counts = Counts(),
    val activityDialog: ActivitySelectDialogUiState = ActivitySelectDialogUiState()
) {
    data class Counts(
        val selected: Int = 0,
        val filtered: Int = 0,
        val total: Int = 0
    )
}

class AppSelectPageVM: ViewModel() {

    companion object {
        private const val TAG = "AppSelectPageVM"
    }

    val uiState: StateFlow<AppSelectUiState>
        get() = _uiState

    private val _uiState = MutableStateFlow(AppSelectUiState())

    private val _filterState = MutableStateFlow(PackageFilterCriterion())

    private val _selectedAppState = MutableStateFlow(setOf<String>())

    private val _activitySelectDialogState = MutableStateFlow(ActivitySelectDialogUiState())

    //TODO: add swipe to refresh
    private val _uiRefreshState = MutableStateFlow(false)

    private var sortActivityList = false
    val fastSearch: Boolean
        get() = _fastSearch

    private var _fastSearch: Boolean = false

    init {
        viewModelScope.launch {
            AppMonitor.appContext.settings.data.collect { pref ->
                sortActivityList = pref[SettingKeys.ACTIVITY_LIST_SORTING] ?: run {
                    AppMonitor.appContext.settings.edit { it[SettingKeys.ACTIVITY_LIST_SORTING] = true }
                    false
                }
                _fastSearch = pref[SettingKeys.APP_LIST_AGGRESSIVE_SEARCH] ?: run {
                    AppMonitor.appContext.settings.edit { it[SettingKeys.APP_LIST_AGGRESSIVE_SEARCH] = true }
                    false
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            AppInfoCache.refresh(AppInfoCache.RetrieveOptions(getActivityInfo = true))
            combine(
                AppInfoCache.getAppInfo(),
                _filterState,
                _selectedAppState,
                _activitySelectDialogState
            ) { appInfoList, filterCriterion, selectedApps, dlgState ->
                val filtered = appInfoList.filter { filterCriterion.filter(it) }.map {
                    AppInfoWithSelection(it, selectedApps.contains(it.packageName))
                }
                AppSelectUiState(
                    appList = filtered,
                    filter = filterCriterion,
                    counts = AppSelectUiState.Counts(
                        selected = selectedApps.size,
                        filtered = filtered.size,
                        total = appInfoList.size
                    ),
                    activityDialog = dlgState
                )
            }.catch {
                Log.e(TAG, "upstream flow error: $it")
            }.collect {
                _uiState.value = it
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            delay(5000)
            AppInfoCache.refresh(AppInfoCache.RetrieveOptions(getActivityInfo = true))
            AppMonitor.appContext.toast("Supplemental refresh finished")
        }
    }

    fun onSelect(packageName: String) {
        _selectedAppState.value = _selectedAppState.value.toMutableSet().apply{ add(packageName) }
    }

    fun onUnselect(packageName: String) {
        _selectedAppState.value = _selectedAppState.value.toMutableSet().apply{ remove(packageName) }
    }

    fun onSearchFilterChange(keyword: String?) {
        _filterState.value = _filterState.value.copy(searchKeyword = keyword?.replace(Regex("\\s"), ""))
    }

    fun onFilterChange(newFilter: PackageFilterCriterion) {
        _filterState.value = newFilter
    }

    fun onClickItemCard(appInfo: AppInfo) {
        _activitySelectDialogState.value = ActivitySelectDialogUiState(
            show = true,
            origin = appInfo,
            list = appInfo.activities.map { DualStateListItem(it.exported, it.simpleName, it) }.sortedByDescending { if (sortActivityList) it.enabled else false },
            exportedCount = appInfo.activities.count { it.exported }
        )
    }

    fun onDismissActivityListDialog() {
        _activitySelectDialogState.value = _activitySelectDialogState.value.copy(show = false, origin = AppInfo.EMPTY)
    }
}