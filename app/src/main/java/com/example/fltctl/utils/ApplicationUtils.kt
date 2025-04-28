package com.example.fltctl.utils

import android.content.Intent
import android.content.pm.PackageManager

/**
 * Created by zeyu.zyzhang on 4/25/25
 * @author zeyu.zyzhang@bytedance.com
 */

fun isLauncher(pm: PackageManager, packageName: String) =
    Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
    }.let {
        pm.resolveActivity(it, PackageManager.MATCH_DEFAULT_ONLY)
    }.let {
        packageName == it?.activityInfo?.packageName
    }

