package com.example.fltctl.widgets.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.Px
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.fltctl.configs.EInkDeviceConfigs
import kotlin.math.roundToInt

fun Number.takeDp(): Int = takeDpAsFloat().roundToInt()

fun Number.takeDpAsFloat(): Float = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    toFloat(),
    Resources.getSystem().displayMetrics
)

val Context.statusBarHeight: Int
    @SuppressLint("InternalInsetResource", "DiscouragedApi")
    get() {
        val statusBarId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (statusBarId > 0) resources.getDimensionPixelSize(statusBarId) else 0
    }

val Context.navBarHeight: Int
    @SuppressLint("InternalInsetResource", "DiscouragedApi")
    get() {
        val navBarId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (navBarId > 0) resources.getDimensionPixelSize(navBarId) else 0
    }


val Context.screenWidth: Int
    get() = resources.configuration.screenWidthDp.takeDp()

val Context.screenHeight: Int
    get() = resources.configuration.screenHeightDp.takeDp()

fun View.margin(
    @Px start: Int? = null,
    @Px top: Int? = null,
    @Px end: Int? = null,
    @Px bottom: Int? = null,
    reverseInRTL: Boolean = true
) {
    val lp = layoutParams
    if (lp is ViewGroup.MarginLayoutParams) {
        lp.margin(context, start, top, end, bottom, reverseInRTL)
    }
    layoutParams = lp
}

fun View.margin(@Px all: Int) = margin(all, all, all, all)

fun ViewGroup.MarginLayoutParams.margin(
    ctx: Context,
    @Px start: Int? = null,
    @Px top: Int? = null,
    @Px end: Int? = null,
    @Px bottom: Int? = null,
    reverseInRTL: Boolean = true
) {
    val reverseLeftRight = (start != null || end != null) && reverseInRTL
    if (reverseLeftRight && ctx.isRTL()) {
        start?.let { rightMargin = it }
        end?.let { leftMargin = it }
    } else {
        start?.let { leftMargin = it }
        end?.let { rightMargin = it }
    }
    top?.let { topMargin = it }
    bottom?.let { bottomMargin = it }
    if (reverseLeftRight) {
        start?.let { marginStart = it }
        end?.let { marginEnd = it }
    } else {
        // clear start end margins to let left right margins to take into effect
        start?.let { marginStart = Int.MIN_VALUE }
        end?.let { marginEnd = Int.MIN_VALUE }
    }
}

fun Context.isRTL(): Boolean = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

fun Int.withAlpha(@FloatRange(0.0, 1.0) alpha: Float): Int {
    val a = (alpha * 255).roundToInt() shl 24
    val rgb = 0x00ffffff and this
    return a + rgb
}

fun Color.withAlpha(@FloatRange(0.0, 1.0) alpha: Float): Color {
    return Color(this.toArgb().withAlpha(alpha))
}

fun createRoundedCornerColorDrawable(@ColorInt color: Int,cornerRadius: Float): Drawable = GradientDrawable().apply {
    setColor(color)
    setCornerRadius(cornerRadius)
}

fun createRoundedCornerEInkDrawable(cornerRadius: Float): Drawable = GradientDrawable().apply {
    setColor(EInkDeviceConfigs.backgroundColor)
    setCornerRadius(cornerRadius)
    setStroke(2.takeDp(), EInkDeviceConfigs.foregroundColor)
}

fun View.setDebouncedOnClickListener(interval: Long = 500L, onClick: (View) -> Unit) {
    var lastClickTime = 0L

    setOnClickListener {
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastClickTime >= interval) {
            lastClickTime = currentTime
            onClick(it)
        }
    }
}
