package com.example.fltctl.widgets.composable

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.borderBottom(width: Dp, color: Color, padding: Dp = 0.dp): Modifier = drawWithContent {
    val widthPx = width.toPx()
    drawContent()
    drawLine(
        color = color,
        start = Offset(padding.toPx(), size.height - widthPx),
        end = Offset(size.width - padding.toPx(), size.height - widthPx),
        strokeWidth = widthPx,
        cap = StrokeCap.Square
    )
}

fun Modifier.borderTop(width: Dp, color: Color, padding: Dp = 0.dp): Modifier = drawWithContent {
    val widthPx = width.toPx()
    drawContent()
    drawLine(
        color = color,
        start = Offset(padding.toPx(), 0f),
        end = Offset(size.width - padding.toPx(), 0f),
        strokeWidth = widthPx,
        cap = StrokeCap.Square
    )
}