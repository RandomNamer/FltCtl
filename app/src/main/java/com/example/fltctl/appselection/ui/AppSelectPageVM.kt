package com.example.fltctl.appselection.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fltctl.appselection.model.AppInfo
import com.example.fltctl.appselection.model.AppInfoCache
import com.example.fltctl.appselection.model.filter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PackageFilterCriterion(
    val searchKeyword: String? = null,
    val includeSystem: Boolean = false,
    val includeNoExportedActivity: Boolean = false,
)

data class AppInfoWithSelection(
    val value: AppInfo,
    val selected: Boolean = false
)


data class AppSelectUiState(
    val appList: List<AppInfoWithSelection> = listOf(),
    val filter: PackageFilterCriterion = PackageFilterCriterion(),
    val counts: Counts = Counts()
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

    //TODO: add swipe to refresh
    private val _uiRefreshState = MutableStateFlow(false)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            AppInfoCache.refresh(AppInfoCache.RetrieveOptions(getActivityInfo = true))
            combine(
                AppInfoCache.getAppInfo(),
                _filterState,
                _selectedAppState
            ) { appInfoList, filterCriterion, selectedApps ->
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
                    )
                )
            }.catch {
                Log.e(TAG, "upstream flow error: $it")
            }.collect {
                _uiState.value = it
            }
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
}