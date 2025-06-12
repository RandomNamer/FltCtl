package com.example.fltctl.tests.compose

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import com.example.fltctl.tests.UiTest
import kotlin.math.abs

/**
 * Created by zeyu.zyzhang on 5/13/25
 * @author zeyu.zyzhang@bytedance.com
 */

val oobTouchEventTestCompose = UiTest.ComposeUiTest(
    "OobTouchEventTestCompose",
    "Test OobTouchEvent",
    content = {
        Screen()
    },
    fullscreen = false
).also {
//    AppMonitor.addStartupTestPage(it)
}

@Composable
private fun Screen() {
    Box(Modifier.fillMaxSize().padding(100.dp)
        .pointerInput(Unit) {
            detectZoomWhenShoot { change, panChange ->
                Log.d("CameraGestureDet", "parent view $change $panChange")
            }
        }) {
        Box(
            modifier = Modifier.size(100.dp).align(Alignment.BottomCenter)
                .background(color = Color.Red)
                .pointerInput(Unit) {
                    interceptOutOfBoundsChildEvents = true
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = true).consume()
                        Log.d("CameraGestureDet", "child view down")
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            Log.d("CameraGestureDet", "child view ${event.changes.fastMap { it.id.value }}")
                            event.changes.fastForEach { it.consume() }
                        } while (event.changes.fastAny { it.pressed })
                        Log.d("CameraGestureDet", "child view released")
                    }
                }
        )
    }
}


internal suspend fun PointerInputScope.detectZoomWhenShoot(
    onZoom: (zoomChange: Float, panChange: Offset) -> Unit,
) {
    awaitEachGesture {
        var zoom = 1f
        var passedZoomTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop


        do {
            val event = awaitPointerEvent(PointerEventPass.Main)

            val canceled = !event.changes.fastAny { !it.isConsumed }
            val otherFingers = event.changes.fastFilter { !it.isConsumed && it.pressed }
            Log.d("CameraGestureDet", "all fingers: ${event.changes.fastMap { it.id.value }}, fingerCount: ${otherFingers.count()}. $canceled")
            if (!canceled && otherFingers.count() >= 2) {
                val zoomChange = otherFingers.calculateZoom()
                val panChange = otherFingers.calculatePan()
                if (!passedZoomTouchSlop) {
                    zoom *= zoomChange

                    val centroidSize = otherFingers.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1-zoom) * centroidSize

                    if (zoomMotion > touchSlop){
                        passedZoomTouchSlop = true
                    }

                }

                if (passedZoomTouchSlop) {
                    if (zoomChange != 1f) {
                        onZoom.invoke(zoomChange, panChange)
                    }
                }
                otherFingers.fastForEach {
                    if (it.positionChanged()) it.consume()
                }
            } else {
                passedZoomTouchSlop = false
                zoom = 1f
            }

        } while (true)
    }
}

/**
 * Slightly modified from androidx/compose/foundation/gestures/TransformGestureDetector.kt
 */
private fun List<PointerInputChange>.calculateZoom(): Float {
    val currentCentroidSize = calculateCentroidSize(useCurrent = true)
    val previousCentroidSize = calculateCentroidSize(useCurrent = false)
    if (currentCentroidSize == 0f || previousCentroidSize == 0f) {
        return 1f
    }
    return currentCentroidSize / previousCentroidSize
}

private fun List<PointerInputChange>.calculatePan(): Offset {
    val currentCentroid = calculateCentroid(useCurrent = true)
    if (currentCentroid == Offset.Unspecified) {
        return Offset.Zero
    }
    val previousCentroid = calculateCentroid(useCurrent = false)
    return currentCentroid - previousCentroid
}

private fun List<PointerInputChange>.calculateCentroidSize(useCurrent: Boolean = true): Float {
    val centroid = calculateCentroid(useCurrent)
    if (centroid == Offset.Unspecified) {
        return 0f
    }

    var distanceToCentroid = 0f
    var distanceWeight = 0
    this.fastForEach { change ->
        if (change.pressed && change.previousPressed) {
            val position = if (useCurrent) change.position else change.previousPosition
            distanceToCentroid += (position - centroid).getDistance()
            distanceWeight++
        }
    }
    return distanceToCentroid / distanceWeight.toFloat()
}


private fun List<PointerInputChange>.calculateCentroid(
    useCurrent: Boolean = true
): Offset {
    var centroid = Offset.Zero
    var centroidWeight = 0

    fastForEach { change ->
        if (change.pressed && change.previousPressed) {
            val position = if (useCurrent) change.position else change.previousPosition
            centroid += position
            centroidWeight++
        }
    }
    return if (centroidWeight == 0) {
        Offset.Unspecified
    } else {
        centroid / centroidWeight.toFloat()
    }
}
