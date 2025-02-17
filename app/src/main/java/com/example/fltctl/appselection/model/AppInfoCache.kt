package com.example.fltctl.appselection.model

import android.content.pm.PackageManager
import com.example.fltctl.AppMonitor
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

//TODO: get activity lazy
object AppInfoCache {

    data class RetrieveOptions(
        val getActivityInfo: Boolean = false,
        val getMetadata: Boolean = false,
        val includeApex: Boolean = false
    )

    private val uiModelList: List<AppInfo>
            get() = uiModelCache.values.toList()

    private val uiModelCache = mutableMapOf<String, AppInfo>()

    private val packageNameSet = mutableSetOf<String>()

    private val pm by lazy {
        AppMonitor.appContext.packageManager
    }

    private val globalRefreshChannel = Channel<RetrieveOptions>(
        capacity = 2,
        onBufferOverflow = BufferOverflow.SUSPEND
    )

    fun getAppInfo(): Flow<List<AppInfo>> {
        val flow = flow {
            emit(uiModelList)
            globalRefreshChannel.receiveAsFlow().collect {
                refreshInternal(option = it)
                emit(uiModelList)
            }
        }
        return flow
    }

    suspend fun refresh(option: RetrieveOptions = RetrieveOptions()) {
        globalRefreshChannel.send(option)
    }

    private fun refreshInternal(option: RetrieveOptions) {
        val refreshed = pm.getInstalledPackages(resolveFlags(option)).mapNotNull {
            it.toUiModel(pm)
        }
        updateCache(refreshed)
    }

    private fun updateCache(result: List<AppInfo>) {
        result.forEach {
            uiModelCache.merge(it.packageName, it, AppInfo::updateWith)
        }
    }


    private fun resolveFlags(option: RetrieveOptions): Int {
        return (if (option.getActivityInfo) PackageManager.GET_ACTIVITIES else 0) or
                (if (option.getMetadata) PackageManager.GET_META_DATA else 0) or
                (if (option.includeApex) PackageManager.MATCH_APEX else 0)
    }

}