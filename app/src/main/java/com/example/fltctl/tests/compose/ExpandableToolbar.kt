package com.example.fltctl.tests.compose

import android.annotation.SuppressLint
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.example.fltctl.AppMonitor
import com.example.fltctl.tests.UiTest
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Created by zeyu.zyzhang on 7/29/25
 * @author zeyu.zyzhang@bytedance.com
 */

val expandableToolbar = UiTest.ComposeUiTest(
    title = "Expandable Toolbar",
    description = "Test the usage of expandable toolbar.",
    content = {
        val random = Random(System.currentTimeMillis())
        val colors = List(100) { Color(random.nextInt()) }
        val itemsAll = remember {
            List(10) {
                @Composable {
                    Box(Modifier
                        .size(56.dp)
                        .background(
                            colors[it]
                        ))
                }
            }
        }
        Box(Modifier
            .fillMaxWidth().height(600.dp), Alignment.TopEnd) {
            Box(modifier = Modifier.padding(vertical = 50.dp).wrapContentWidth().fillMaxHeight()) {
                ExpandableToolbarEntry(
                    items = itemsAll,
                    5
                )
            }

        }
    },
).also {
    AppMonitor.addStartupTestPage(it)
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun ExpandableToolbarEntry(
    items: List<@Composable () -> Unit>,
    expandIndexStart: Int,
) {
    BoxWithConstraints() {
        val itemAlwaysCount = ((maxHeight / 56.dp).toInt()).coerceIn(1, expandIndexStart)
        val expandable = itemAlwaysCount < items.size

        if (!expandable) {
            Column {
                items.forEach { it() }
            }
        } else {
            ExpandableScrollableToolbarLayout(
                modifier = Modifier.align(Alignment.TopEnd),
                itemsTopFixed = items.subList(0, 2),
                itemsAlways = items.subList(2, itemAlwaysCount-1),
                itemsExpandable = items.subList(itemAlwaysCount-1, items.size),
            )
        }
    }


}

@Composable
private fun EndAlignedSimpleColumn(
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = { content.invoke() },
        measurePolicy = object: MeasurePolicy {
            override fun MeasureScope.measure(
                measurables: List<Measurable>,
                constraints: Constraints
            ): MeasureResult {
                val allChildResults = measurables.map { it.measure(constraints) }
                val width = allChildResults.maxOf { it.width }.coerceIn(constraints.minWidth, constraints.maxWidth)
                val height = allChildResults.sumOf { it.height } //Expose measured height to outside, no need to coerce here
                return layout(width, height) {
                    var curY = 0
                    for (placable in allChildResults) {
                        placable.placeRelative(width - placable.width, curY)
                        curY += placable.height
                    }
                }
            }
        }
    )
}

private class ExpandableLayoutMeasureData(
    var collapsedHeight: Int = 0,
    var expandedHeight: Int = 0,
    var initialized: Boolean = false
)


@Composable
private fun ExpandableScrollableToolbarLayout(
    modifier: Modifier,
    itemsTopFixed: List<@Composable () -> Unit>,
    itemsAlways: List<@Composable () -> Unit>,
    itemsExpandable: List<@Composable () -> Unit>,
) {
    var expanded = remember { MutableTransitionState(false) }
    val transition = rememberTransition(expanded, label = "")
    val scrollState = rememberScrollState()

    val cachedMeasurements = remember(itemsAlways, itemsExpandable) {
        ExpandableLayoutMeasureData()
    }

    val expandProgress by transition.animateFloat (
        { tween(durationMillis = 200) }
    ) {
        if (it) 1f else 0f
    }

    LaunchedEffect(expanded.currentState) {
        if (expanded.currentState == expanded.targetState) {
            scrollState.scrollTo(0)
        }
    }


    Layout(
        modifier = modifier,
        content = {
            EndAlignedSimpleColumn(Modifier.layoutId("topFixed")) {
                itemsTopFixed.forEach { it.invoke() }
                Spacer(Modifier
                    .height(8.dp)
                    .width(56.dp)
                    .background(Color.Black))
            }
            if (!cachedMeasurements.initialized) EndAlignedSimpleColumn(Modifier.layoutId("alwaysItemMeasureOnly").alpha(0f)) {
                itemsAlways.forEach { it.invoke() }
            }
            EndAlignedSimpleColumn(Modifier.layoutId("scrollable").verticalScroll(scrollState)) {
                itemsAlways.forEach { it.invoke() }
                itemsExpandable.forEach { it.invoke() }
            }
            Icon(
                Icons.Outlined.ExpandMore,
                null,
                Modifier
                    .requiredSize(40.dp)
                    .layoutId("icon")
                    .graphicsLayer {
                        rotationZ = lerp(0f, 180f, expandProgress)
                    }
                    .clickable {
                        expanded.targetState = !expanded.currentState
                    }
            )
        },

        measurePolicy = remember {
            object : MeasurePolicy {
                override fun MeasureScope.measure(
                    measurables: List<Measurable>,
                    constraints: Constraints
                ): MeasureResult {
                    val topFixed = measurables.first { it.layoutId == "topFixed" }
                    val alwaysItemMeasureOnly = measurables.firstOrNull { it.layoutId == "alwaysItemMeasureOnly" }
                    val scrollable = measurables.first { it.layoutId == "scrollable" }
                    val icon = measurables.first { it.layoutId == "icon" }

                    // pre-measure
                    val measuredCollapsedHeight = alwaysItemMeasureOnly?.measure(constraints)?.height?.takeIf { it > 0 }
                    val topFixedPlaceable = topFixed.measure(constraints)
                    val iconPlaceable = icon.measure(constraints)
                    val availableScrollableHeight = constraints.maxHeight - iconPlaceable.measuredHeight - topFixedPlaceable.measuredHeight
                    val scrollablePlaceable = scrollable.measure(constraints.copy(maxHeight = availableScrollableHeight))
//                    val expandedTotalHeight = (scrollablePlaceable.measuredHeight + topFixedPlaceable.height + iconPlaceable.height).coerceIn(0, constraints.maxHeight)
                    if (!cachedMeasurements.initialized) {
                        cachedMeasurements.run {
                            measuredCollapsedHeight?.let {
                                //Can be null if topFixed does not change but
                                collapsedHeight = it
                            }
                            expandedHeight = scrollablePlaceable.height
                            initialized = true
                        }
                    }


                    val width = maxOf(topFixedPlaceable.width, scrollablePlaceable.width, iconPlaceable.width)

                    val finalScrollHeight = lerp(cachedMeasurements.collapsedHeight, scrollablePlaceable.height, expandProgress).roundToInt()

                    return layout(width, finalScrollHeight + topFixedPlaceable.height + iconPlaceable.height) {
                        topFixedPlaceable.placeRelative(width - topFixedPlaceable.width, 0)
                        scrollablePlaceable.placeRelativeWithLayer(width - scrollablePlaceable.width, topFixedPlaceable.height) {
                            clip = true
                            shape = GenericShape { size, layoutDirection ->
                                addRect(Rect(0f, 0f, size.width, finalScrollHeight.toFloat()))
                            }
                        }
                        iconPlaceable.place(width - iconPlaceable.width, topFixedPlaceable.height + finalScrollHeight)
                    }
                }

            }
        }
    )

}

@Composable
private fun ExpandableScrollableToolbarLayout2(
    modifier: Modifier,
    itemsTopFixed: List<@Composable () -> Unit>,
    itemsAlways: List<@Composable () -> Unit>,
    itemsExpandable: List<@Composable () -> Unit>,
) {
    var expanded by remember { mutableStateOf(false) }

    // Pre-measure collapsed and expanded heights only once when content changes
    val measurementState = remember(itemsAlways, itemsExpandable) {
        MeasurementState()
    }

    // Animation values that don't trigger recomposition of content
    val expandProgress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "expandProgress"
    )

    val expandIconRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "iconRotation"
    )

    Layout(
        modifier = modifier,
        content = {
            // Top fixed content
            EndAlignedSimpleColumn(Modifier.layoutId("topFixed")) {
                itemsTopFixed.forEach { it.invoke() }
                Spacer(Modifier
                    .height(8.dp)
                    .width(56.dp)
                    .background(Color.Black))
            }

            // Hidden measurement-only composable for collapsed height
            if (!measurementState.isInitialized) {
                EndAlignedSimpleColumn(
                    Modifier
                        .layoutId("measureCollapsed")
                        .alpha(0f) // Invisible but measured
                ) {
                    itemsAlways.forEach { it.invoke() }
                }
            }

            // Full scrollable content (always contains everything)
            EndAlignedSimpleColumn(
                Modifier
                    .layoutId("scrollable")
                    .verticalScroll(rememberScrollState())
            ) {
                itemsAlways.forEach { it.invoke() }
                itemsExpandable.forEach { it.invoke() }
            }

            // Expand/collapse icon
            Icon(
                Icons.Outlined.ExpandMore,
                contentDescription = null,
                modifier = Modifier
                    .requiredSize(40.dp)
                    .layoutId("icon")
                    .graphicsLayer {
                        rotationZ = expandIconRotation // Use graphicsLayer instead of Modifier.rotate
                    }
                    .clickable { expanded = !expanded }
            )
        },

        measurePolicy = remember {
            MeasurePolicy { measurables, constraints ->
                val topFixed = measurables.first { it.layoutId == "topFixed" }
                val measureCollapsed = measurables.firstOrNull { it.layoutId == "measureCollapsed" }
                val scrollable = measurables.first { it.layoutId == "scrollable" }
                val icon = measurables.first { it.layoutId == "icon" }

                // Measure all components
                val topFixedPlaceable = topFixed.measure(constraints)
                val iconPlaceable = icon.measure(constraints)

                // Initialize measurements if needed (only happens once)
                measureCollapsed?.let { measurable ->
                    val collapsedPlaceable = measurable.measure(constraints)
                    measurementState.collapsedHeight = collapsedPlaceable.height
                }

                // Measure scrollable content with full available height
                val availableHeightForScrollable = constraints.maxHeight - topFixedPlaceable.height - iconPlaceable.height
                val scrollablePlaceable = scrollable.measure(
                    constraints.copy(maxHeight = availableHeightForScrollable)
                )

                // Store expanded height if not initialized
                if (!measurementState.isInitialized) {
                    measurementState.expandedHeight = minOf(scrollablePlaceable.height, availableHeightForScrollable)
                    measurementState.isInitialized = true
                }

                val width = maxOf(topFixedPlaceable.width, scrollablePlaceable.width, iconPlaceable.width)

                // Calculate current scroll height based on animation progress
                val currentScrollHeight =  lerp(
                    measurementState.collapsedHeight,
                    measurementState.expandedHeight,
                    expandProgress
                ).roundToInt()

                val totalHeight = topFixedPlaceable.height + currentScrollHeight + iconPlaceable.height

                layout(width, totalHeight) {
                    // Place top fixed content
                    topFixedPlaceable.placeRelative(width - topFixedPlaceable.width, 0)

                    // Place scrollable content with clipping
                    scrollablePlaceable.placeRelativeWithLayer(
                        x = width - scrollablePlaceable.width,
                        y = topFixedPlaceable.height
                    ) {
                        clip = true
                        shape = GenericShape { size, _ ->
                            addRect(Rect(0f, 0f, size.width, currentScrollHeight.toFloat()))
                        }
                    }

                    // Place icon at bottom
                    iconPlaceable.place(
                        x = width - iconPlaceable.width,
                        y = topFixedPlaceable.height + currentScrollHeight
                    )
                }
            }
        }
    )
}

// Helper class to store measurement state
private class MeasurementState {
    var collapsedHeight: Int = 0
    var expandedHeight: Int = 0
    var isInitialized: Boolean = false
}

// Helper function for linear interpolation
private fun lerp(start: Int, stop: Int, fraction: Float): Float {
    return start + fraction * (stop - start)
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + fraction * (stop - start)
}