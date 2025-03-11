package com.example.fltctl.utils

import android.accessibilityservice.AccessibilityService
import android.app.AppOpsManager
import android.content.Context
import android.os.Binder
import android.os.Build
import android.provider.Settings
import com.example.fltctl.AppBackgroundListener
import com.example.fltctl.AppMonitor
import com.example.fltctl.controls.service.FloatingControlAccessibilityService

fun hasOverlaysPermission(context: Context): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context)
        else hasOverlaysPermissionUnderM(context)
    } catch (e: Exception) {
        false
    }
}

private const val OP_SYSTEM_ALERT_WINDOW = 24
private fun hasOverlaysPermissionUnderM(context: Context): Boolean {
    val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    try {
        val checkOpById = appOpsManager::class.java.getDeclaredMethod(
            "checkOp",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            String::class.java,
        )
        if (!checkOpById.isAccessible) checkOpById.isAccessible = true
        return AppOpsManager.MODE_ALLOWED == checkOpById.invoke(appOpsManager, OP_SYSTEM_ALERT_WINDOW, Binder.getCallingUid(), context.packageName)
    } catch (e: Exception) {
        return false
    }
}

fun hasEnabledAccessibilityService(context: Context, serviceClass: Class<out AccessibilityService> = FloatingControlAccessibilityService::class.java): Boolean {
    try {
        if (Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED) == 1) {
            val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            enabledServices.split(",").forEach {
                if (it.contains(serviceClass.simpleName)) return true
            }
        }
        return false
    }catch (e: Exception) {
        MmapLogProxy.getInstance().log(MmapLogProxy.ERROR, "PermissionsKt", "Check accessibility fail for ${e.message}")
        return false
    }
}

fun requestAccessibilityPermission(context: Context, serviceClass: Class<AccessibilityService>, onResult: (Boolean) -> Unit) {

    AppMonitor.addListener(object: AppBackgroundListener {
        override fun onAppBackground() {}

        override fun onAppForeground() {

        }
    })
}