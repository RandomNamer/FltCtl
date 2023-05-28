package com.example.fltctl.appselection.model

import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ApplicationInfoFlags
import com.example.fltctl.appselection.ui.PackageFilterCriterion

fun PackageFilterCriterion.filter(appInfo: AppInfo): Boolean {
    return appInfo.search(searchKeyword ?: "") &&
            (includeSystem || !appInfo.isSystem) &&
            (includeNoExportedActivity || appInfo.activities.any { it.exported })

}

fun AppInfo.search(with: String): Boolean = displayName.contains(with, ignoreCase = true) || packageName.contains(with, ignoreCase = true)

fun PackageInfo.toUiModel(pm: PackageManager): AppInfo {
    return AppInfo(
        displayName = applicationInfo.loadLabel(pm).toString(),
        packageName = packageName,
        versionName = versionName ?: "null",
        icon = applicationInfo.loadIcon(pm),
        firstInstallTime = firstInstallTime,
        lastUsedTime = lastUpdateTime,
        apkDir = applicationInfo.sourceDir,
        dataDir = applicationInfo.dataDir,
        isSystem = applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
        activities = activities?.map { a ->
        ConciseActivityInfo(
            simpleName = a.name,
            className = a.name,
            exported = a.exported
        ) } ?: listOf(),
    )
}