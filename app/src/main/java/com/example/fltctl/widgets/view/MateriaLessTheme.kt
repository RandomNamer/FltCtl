package com.example.fltctl.widgets.view

/**
 * Created by zeyu.zyzhang on 2/18/25
 * @author zeyu.zyzhang@bytedance.com
 */
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.example.fltctl.R


enum class SystemPalette {
    ACCENT1, ACCENT2, ACCENT3, NEUTRAL1, NEUTRAL2
}

enum class ColorStrength(val value: Int) {
    _10(10), _50(50), _100(100), _200(200), _300(300), _400(400),
    _500(500), _600(600), _700(700), _800(800), _900(900), _1000(1000);

    companion object {
        fun fromInt(value: Int): ColorStrength? = values().find { it.value == value }
    }
}

fun Context.getSystemAccent(family: SystemPalette, strength: ColorStrength): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemAccentApi31Impl(family, strength)
    } else {
        getFallbackSystemAccent(family) // Fallback for pre-Android 12
    }
}

@RequiresApi(Build.VERSION_CODES.S) // Android 12+
private fun Context.getSystemAccentApi31Impl(family: SystemPalette, strength: ColorStrength): Int {
    val resources = this.resources
    val colorId = when (family) {
        SystemPalette.ACCENT1 -> {
            when (strength) {
                ColorStrength._10 -> android.R.color.system_accent1_10
                ColorStrength._50 -> android.R.color.system_accent1_50
                ColorStrength._100 -> android.R.color.system_accent1_100
                ColorStrength._200 -> android.R.color.system_accent1_200
                ColorStrength._300 -> android.R.color.system_accent1_300
                ColorStrength._400 -> android.R.color.system_accent1_400
                ColorStrength._500 -> android.R.color.system_accent1_500
                ColorStrength._600 -> android.R.color.system_accent1_600
                ColorStrength._700 -> android.R.color.system_accent1_700
                ColorStrength._800 -> android.R.color.system_accent1_800
                ColorStrength._900 -> android.R.color.system_accent1_900
                ColorStrength._1000 -> android.R.color.system_accent1_1000
            }
        }
        SystemPalette.ACCENT2 -> { /* Similar structure for ACCENT2 */
            when (strength) {
                ColorStrength._10 -> android.R.color.system_accent2_10
                ColorStrength._50 -> android.R.color.system_accent2_50
                ColorStrength._100 -> android.R.color.system_accent2_100
                ColorStrength._200 -> android.R.color.system_accent2_200
                ColorStrength._300 -> android.R.color.system_accent2_300
                ColorStrength._400 -> android.R.color.system_accent2_400
                ColorStrength._500 -> android.R.color.system_accent2_500
                ColorStrength._600 -> android.R.color.system_accent2_600
                ColorStrength._700 -> android.R.color.system_accent2_700
                ColorStrength._800 -> android.R.color.system_accent2_800
                ColorStrength._900 -> android.R.color.system_accent2_900
                ColorStrength._1000 -> android.R.color.system_accent2_1000
            }
        }
        SystemPalette.ACCENT3 -> { /* Similar structure for ACCENT3 */
            when (strength) {
                ColorStrength._10 -> android.R.color.system_accent3_10
                ColorStrength._50 -> android.R.color.system_accent3_50
                ColorStrength._100 -> android.R.color.system_accent3_100
                ColorStrength._200 -> android.R.color.system_accent3_200
                ColorStrength._300 -> android.R.color.system_accent3_300
                ColorStrength._400 -> android.R.color.system_accent3_400
                ColorStrength._500 -> android.R.color.system_accent3_500
                ColorStrength._600 -> android.R.color.system_accent3_600
                ColorStrength._700 -> android.R.color.system_accent3_700
                ColorStrength._800 -> android.R.color.system_accent3_800
                ColorStrength._900 -> android.R.color.system_accent3_900
                ColorStrength._1000 -> android.R.color.system_accent3_1000
            }
        }
        SystemPalette.NEUTRAL1 -> { /* Similar structure for NEUTRAL1 */
            when (strength) {
                ColorStrength._10 -> android.R.color.system_neutral1_10
                ColorStrength._50 -> android.R.color.system_neutral1_50
                ColorStrength._100 -> android.R.color.system_neutral1_100
                ColorStrength._200 -> android.R.color.system_neutral1_200
                ColorStrength._300 -> android.R.color.system_neutral1_300
                ColorStrength._400 -> android.R.color.system_neutral1_400
                ColorStrength._500 -> android.R.color.system_neutral1_500
                ColorStrength._600 -> android.R.color.system_neutral1_600
                ColorStrength._700 -> android.R.color.system_neutral1_700
                ColorStrength._800 -> android.R.color.system_neutral1_800
                ColorStrength._900 -> android.R.color.system_neutral1_900
                ColorStrength._1000 -> android.R.color.system_neutral1_1000
            }
        }
        SystemPalette.NEUTRAL2 -> { /* Similar structure for NEUTRAL2 */
            when (strength) {
                ColorStrength._10 -> android.R.color.system_neutral2_10
                ColorStrength._50 -> android.R.color.system_neutral2_50
                ColorStrength._100 -> android.R.color.system_neutral2_100
                ColorStrength._200 -> android.R.color.system_neutral2_200
                ColorStrength._300 -> android.R.color.system_neutral2_300
                ColorStrength._400 -> android.R.color.system_neutral2_400
                ColorStrength._500 -> android.R.color.system_neutral2_500
                ColorStrength._600 -> android.R.color.system_neutral2_600
                ColorStrength._700 -> android.R.color.system_neutral2_700
                ColorStrength._800 -> android.R.color.system_neutral2_800
                ColorStrength._900 -> android.R.color.system_neutral2_900
                ColorStrength._1000 -> android.R.color.system_neutral2_1000
            }
        }
    }
    return resources.getColor(colorId, this.theme) // Use getColor for system resources.
}

private fun Context.getFallbackSystemAccent(family: SystemPalette): Int {
    return when (family) {
        SystemPalette.ACCENT1 -> ContextCompat.getColor(this, android.R.color.holo_blue_light)
        SystemPalette.ACCENT2 -> ContextCompat.getColor(this, android.R.color.holo_green_light)
        SystemPalette.ACCENT3 -> ContextCompat.getColor(this, android.R.color.holo_orange_light)
        SystemPalette.NEUTRAL1 -> ContextCompat.getColor(this, android.R.color.darker_gray)
        SystemPalette.NEUTRAL2 -> ContextCompat.getColor(this, R.color.light_gray)
    }
}

