package com.example.fltctl.widgets.composable

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp

fun Modifier.borderBottom(width: Dp, color: Color): Modifier = drawWithContent {
    val widthPx = width.toPx()
    drawContent()
    drawLine(
        color = color,
        start = Offset(0f, size.height - widthPx),
        end = Offset(size.width, size.height - widthPx),
        strokeWidth = widthPx,
        cap = StrokeCap.Square
    )
}