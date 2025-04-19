package com.example.fltctl.widgets.composable

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import kotlin.math.roundToInt

fun Modifier.borderBottom(width: Dp, color: Color, padding: Dp = 0.dp, paddingStart: Dp = padding, paddingEnd: Dp = padding): Modifier = borderLine(false, width, color, padding, paddingStart, paddingEnd)

fun Modifier.borderTop(width: Dp, color: Color, padding: Dp = 0.dp, paddingStart: Dp = 0.dp, paddingEnd: Dp = 0.dp): Modifier = borderLine(true, width, color, padding, paddingStart,paddingEnd)

fun Modifier.borderLine(top: Boolean, width: Dp, color: Color, padding: Dp = 0.dp, paddingStart: Dp = 0.dp, paddingEnd: Dp = 0.dp): Modifier = drawWithContent {
    val widthPx = width.toPx()
    val psPx = max(padding, paddingStart).toPx()
    val pePx = max(padding, paddingEnd).toPx()
    val yPx = if (top) 0f else size.height - widthPx
    drawContent()
    drawLine(
        color = color,
        start = Offset(psPx, yPx),
        end = Offset(size.width - pePx, yPx),
        strokeWidth = widthPx,
        cap = StrokeCap.Square
    )
}

val activity: Activity?
    @Composable get() = LocalContext.current.findActivity()

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun Dp.toPxFloat() = with(LocalDensity.current) { this@toPxFloat.toPx() }

@Composable
fun Dp.toPx() = this@toPx.toPxFloat().roundToInt()

@Composable
fun CornerSize.toPx() = toPx(Size.Zero, LocalDensity.current)

@Composable
fun CornerSize.toDp(): Dp = this@toDp.toPx().toDp()

@Composable
fun Float.toDp() : Dp = (this / 1.dp.toPx()).dp

@Composable
fun Modifier.simplyScrollable(): Modifier = composed {
    val vss = rememberScrollState()
    val hss = rememberScrollState()
    return@composed this.verticalScroll(vss).horizontalScroll(hss)
}