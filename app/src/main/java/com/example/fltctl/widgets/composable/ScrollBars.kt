package com.example.fltctl.widgets.composable

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.random.Random


@Composable
private fun ColorScheme.verticalScrollbarColors(): VerticalScrollbarColors = VerticalScrollbarColors(
    foreground = primary, background = primaryContainer
)


data class VerticalScrollbarColors(
    val foreground: Color,
    val background: Color
)

object VerticalScrollbarDefaults {
    val color: VerticalScrollbarColors
        @Composable get() = MaterialTheme.colorScheme.verticalScrollbarColors()
    val minHeight = 16.dp
    val thickness = 6.dp
    val cornerRadius = 4.dp
}


/**
 * Yet another widget missing from current Material 3 components, even at 2025.
 * Only has indicator function, not draggable.
 * This component should be visible when the content is actually scrollable.
 * Auto-hide in 2s (configurable), fully customizable and only recomposes when there's scrolling.
 */
@SuppressLint("FrequentlyChangedStateReadInComposition")
@Composable
fun VerticalScrollbar(
    scrollState: Any, // Accept ScrollState or LazyListState
    timeout: Long = 2000,
    modifier: Modifier = Modifier,
    colors: VerticalScrollbarColors = VerticalScrollbarDefaults.color,
    thickness: Dp = VerticalScrollbarDefaults.thickness,
    minHeight: Dp = VerticalScrollbarDefaults.minHeight,
    cornerRadius: Dp = VerticalScrollbarDefaults.cornerRadius
) {
    var isVisible by remember { mutableStateOf(false) }
    var previousScrollOffset by remember { mutableStateOf(0f) }

    if (scrollState !is ScrollState && scrollState !is LazyListState) return



    // Monitor scroll changes
    val currentScrollOffset: Float = when (scrollState) {
        is ScrollState -> scrollState.value.toFloat() / scrollState.maxValue
        is LazyListState -> {
            val totalItems = scrollState.layoutInfo.totalItemsCount
            if (totalItems > 0) {
                scrollState.firstVisibleItemIndex.toFloat() / (totalItems - scrollState.layoutInfo.visibleItemsInfo.size)
            } else 0f
        }
        else -> throw IllegalArgumentException("Unsupported scroll state type")
    }


    // Trigger visibility and timeout logic
    LaunchedEffect(currentScrollOffset) {
        if (currentScrollOffset != previousScrollOffset) {
            isVisible = true // Make scrollbar visible when scrolling
            previousScrollOffset = currentScrollOffset

            delay(timeout)
            if (currentScrollOffset == previousScrollOffset) {
                isVisible = false
            }
        }
    }

    if (isVisible) {
        Canvas(modifier = modifier
            .fillMaxHeight()
            .width(thickness)) {

            var proportion: Float = when(scrollState) {
                is ScrollState -> size.height / (scrollState.maxValue.toFloat() + size.height)
                is LazyListState -> scrollState.layoutInfo.visibleItemsInfo.size / scrollState.layoutInfo.totalItemsCount.toFloat()
                else -> 0f
            }

            if (proportion > 0.9f) {
                isVisible = false
                return@Canvas
            }

            // Calculate scrollbar dimensions
            val scrollbarHeight = max(minHeight.toPx(), size.height * proportion)
            val scrollbarOffsetY = (size.height - scrollbarHeight) * currentScrollOffset

            // Draw the scrollbar track (optional)
            drawRoundRect(
                color = colors.background,
                size = size.copy(width = (thickness - 2.dp).toPx()),
                cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
            )

            // Draw the scrollbar handle
            drawRoundRect(
                color = colors.foreground,
                topLeft = androidx.compose.ui.geometry.Offset(
                    x = -1.dp.toPx(),
                    y = scrollbarOffsetY
                ),
                size = Size(
                    width = thickness.toPx(),
                    height = scrollbarHeight
                ),
                cornerRadius = CornerRadius(
                    x = cornerRadius.toPx(),
                    y = cornerRadius.toPx()
                )
            )
        }
    }
}

@Preview
@Composable
fun VScrollBarPreview() {
    val items = listOf(
        "Item 1",
        "Item 2",
        "Item 3",
        "Item 4",
        "Item 5",
        "Item 6",
        "Item 7",
        "Item 8",
        "Item 9",
        "Item 10",
//        "Item 1",
//        "Item 2",
//        "Item 3",
//        "Item 4",
//        "Item 5",
//        "Item 6",
//        "Item 7",
//        "Item 8",
//        "Item 9",
//        "Item 10"
    )
    Box(Modifier.size(width = 300.dp, height = 500.dp)) {
        val scrollState = rememberScrollState(0)
        Column(
            Modifier
                .fillMaxHeight()
                .verticalScroll(scrollState)) {
            items.forEach {
                Text(text = it, modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(color = Color(Random.nextInt())))
            }
        }
        VerticalScrollbar(scrollState, modifier = Modifier
            .align(Alignment.CenterEnd)
            .padding( top = 100.dp, bottom = 100.dp,end = 5.dp))
    }
}