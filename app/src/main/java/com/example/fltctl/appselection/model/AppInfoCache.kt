package com.example.fltctl.appselection.model

import android.content.pm.PackageManager
import com.example.fltctl.AppMonitor
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

object AppInfoCache {

    data class RetrieveOptions(
        val getActivityInfo: Boolean = false,
        val getMetadata: Boolean = false,
        val includeApex: Boolean = false
    )

    private val uiModelCache = mutableListOf<AppInfo>()

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
            emit(uiModelCache.toList())
            globalRefreshChannel.receiveAsFlow().collect {
                refreshInternal(option = it)
                emit(uiModelCache.toList())
            }
        }
        return flow
    }

    suspend fun refresh(option: RetrieveOptions = RetrieveOptions()) {
        globalRefreshChannel.send(option)
    }

    private fun refreshInternal(option: RetrieveOptions) {
        val refreshed = pm.getInstalledPackages(resolveFlags(option)).map {
            it.toUiModel(pm)
        }
        refreshed.forEach {
            if (packageNameSet.add(it.packageName)) uiModelCache.add(it)
        }
    }

    private fun resolveFlags(option: RetrieveOptions): Int {
        return (if (option.getActivityInfo) PackageManager.GET_ACTIVITIES else 0) or
                (if (option.getMetadata) PackageManager.GET_META_DATA else 0) or
                (if (option.includeApex) PackageManager.MATCH_APEX else 0)
    }

}