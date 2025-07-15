package com.example.fltctl.appselection.model

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import com.example.fltctl.AppMonitor
import com.example.fltctl.appselection.ui.PackageFilterCriterion
import com.example.fltctl.utils.logs

const val TAG_APPINF = "AppInfInfra"
private val log by logs(TAG_APPINF)

fun PackageFilterCriterion.filter(appInfo: AppInfo): Boolean {
    return appInfo.search(searchKeyword ?: "") &&
            (includeSystem || !appInfo.isSystem) &&
            (includeNoExportedActivity || appInfo.activities.any { it.exported })

}

fun AppInfo.search(with: String): Boolean = displayName.contains(with, ignoreCase = true) || packageName.contains(with, ignoreCase = true)

fun PackageInfo.toUiModel(pm: PackageManager): AppInfo? {
    return applicationInfo?.let {
        AppInfo(
            displayName = it.loadLabel(pm).toString(),
            packageName = packageName,
            versionName = versionName ?: "null",
            icon = it.loadIcon(pm),
            firstInstallTime = firstInstallTime,
            lastUsedTime = lastUpdateTime,
            apkDir = it.sourceDir,
            dataDir = it.dataDir,
            isSystem = it.flags and ApplicationInfo.FLAG_SYSTEM != 0,
            activities = activities?.map { a ->
                ConciseActivityInfo(
                    simpleName = a.name,
                    className = a.name,
                    exported = a.exported
                ) } ?: listOf(),
        )
    }
}

fun AppInfo.updateWith(new: AppInfo): AppInfo {
    if (new.packageName != packageName) return this@updateWith
    //Fields needs compute:
    val newVersionName = if (new.versionName == "null") versionName else new.versionName
    val mergedActivities = activities.associateBy { it.className }.toMutableMap()
    new.activities.forEach { mergedActivities.putIfAbsent(it.className, it) }
    return new.copy(versionName = newVersionName, activities = mergedActivities.values.toList())
}

fun Context.openAppSettingsPage(packageName: String) {
    log.w("Incoming pkgName: $packageName")
    startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        setData(Uri.parse("package:$packageName"))
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

fun Context.startActivityByName(pkgName: String, clsName: String) {
    log.i("starting Activity by: $pkgName, $clsName")
    try {
        startActivity(Intent().apply {
            component = ComponentName(pkgName, clsName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    } catch (e: java.lang.SecurityException) {
        Toast.makeText(AppMonitor.appContext, "Start activity failed, possibly intent filter mismatch", Toast.LENGTH_SHORT).show()
        log.w(e.toString())
        log.w(e.stackTraceToString())
    } catch (e: Exception) {
        Toast.makeText(AppMonitor.appContext, "Other Exception: $e", Toast.LENGTH_LONG).show()
        log.w("Other exception: $e")
    }
}
