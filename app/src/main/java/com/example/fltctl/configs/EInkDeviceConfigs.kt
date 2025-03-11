package com.example.fltctl.configs

import android.graphics.Color
import android.os.Build
import com.example.fltctl.widgets.view.withAlpha
import kotlinx.coroutines.delay

/**
 * Created by zeyu.zyzhang on 2/16/25
 * @author zeyu.zyzhang@bytedance.com
 */
object EInkDeviceConfigs {
    @JvmStatic
    private val eInkBrands = listOf("Onyx", "iReader", "Hanvon")

    @JvmStatic
    private fun matchByManufacturer(): Boolean {
        //TODO: pattern matching
        return eInkBrands.contains(Build.BRAND) || eInkBrands.contains(Build.MANUFACTURER)
    }

    val isEInkDevice: Boolean
        get() = matchByManufacturer()

    val eInkBrand: String? = eInkBrands.find { it == Build.BRAND || it == Build.MANUFACTURER }

    val backgroundColor: Int
        get() = Color.WHITE.withAlpha(0.3f)

    val foregroundColor: Int
        get() = Color.BLACK

    init {
        if (matchByManufacturer()) {
            SettingsCache.setAfter(SettingKeys.UI_EINK_MODE, true) {
                delay(500L)
            }
        }
    }

}