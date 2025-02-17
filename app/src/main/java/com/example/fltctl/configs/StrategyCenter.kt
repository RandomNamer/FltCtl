package com.example.fltctl.configs

import android.health.connect.datatypes.units.Power
import android.os.PowerManager

/**
 * Created by zeyu.zyzhang on 2/16/25
 * @author zeyu.zyzhang@bytedance.com
 */
data class WakeLockStrategy(
    val useView: Boolean = false,
    val wakeLockType: Int = PowerManager.PARTIAL_WAKE_LOCK,
    val writeSettings: Boolean = false
)

val localWakeLockStrategy: WakeLockStrategy
    get() {
        val noWakelockSupport = EInkDeviceConfigs.eInkBrand == "Onyx"
        return WakeLockStrategy(
            useView = !noWakelockSupport,
            wakeLockType = if(EInkDeviceConfigs.isEInkDevice) PowerManager.FULL_WAKE_LOCK else PowerManager.SCREEN_DIM_WAKE_LOCK,
            writeSettings = noWakelockSupport
        )
    }
