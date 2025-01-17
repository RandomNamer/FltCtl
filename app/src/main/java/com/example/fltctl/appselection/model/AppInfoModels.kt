package com.example.fltctl.appselection.model

import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toBitmapOrNull
import com.example.fltctl.widgets.view.takeDp

/**
 * Combined info for UI from [PackageInfo], [ActivityInfo] and [ApplicationInfo]
 */
data class AppInfo(
    val displayName: String,
    val packageName: String,
    val versionName: String,
    val icon: Drawable,
    val firstInstallTime: Long,
    //TODO: add UseStats
    val lastUsedTime: Long,
    val apkDir: String,
    val dataDir: String,
    val isSystem: Boolean,
    val activities: List<ConciseActivityInfo>
) {
    companion object {
        val EMPTY = testAppInfo.copy(activities = listOf())
    }

    fun iconBitmap(size: Int): Bitmap =
        icon.toBitmapOrNull(size, size, Bitmap.Config.ARGB_8888) ?:
        Resources.getSystem().getDrawable(android.R.drawable.sym_def_app_icon).toBitmap(size, size)

    fun iconBitmapCompose(size: Int): ImageBitmap = iconBitmap(size).asImageBitmap()
}

data class ConciseActivityInfo(
    val simpleName: String,
    val className: String,
    val exported: Boolean = false
)

val testAppInfo = AppInfo(
    displayName = "homo",
    packageName = "com.inm.homo",
    versionName = "1.14.514",
    icon = ColorDrawable(Color.YELLOW),
    firstInstallTime = 1145141919810,
    lastUsedTime = 1145141919810,
    apkDir = "~/Downloads",
    dataDir = "~/Downloads",
    isSystem = false,
    activities = listOf(
        ConciseActivityInfo(
            simpleName = "YejuSenpaiActivity",
            className = "com.inm.homo.ui.YejuSenpaiActivity",
        )
    )
)