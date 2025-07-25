package com.example.fltctl.tests.compose.albumtest

import android.os.CancellationSignal
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastSumBy
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.sqrt

/**
 * Created by zeyu.zyzhang on 4/15/25
 * @author zeyu.zyzhang@bytedance.com
 */

/**
 * Enable only drag down to start
 */
suspend fun PointerInputScope.detectZoomAndDrag(
    onZoomGesture: (centroid: Offset, pan: Offset, zoom: Float) -> Unit,
    onZoomStart: () -> Unit = {},
    onZoomEnd: () -> Unit = {},
    detectVerticalOnly: () -> Boolean = { true },
    onDrag: (point: Offset, delta: Offset) -> Unit,
    onDragStart: () -> Unit = {},
    onDragEnd: (Velocity) -> Unit = {},
) {
    awaitEachGesture {
        var zoom = 1f
        var pan = Offset.Zero
        var passedZoomTouchSlop = false
        var passedDragTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        val velocityTracker = VelocityTracker()

        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.fastAny { it.isConsumed }
            val touchCount = event.changes.fastSumBy { if (it.pressed) 1 else 0 }
            if (!canceled) {
                if (touchCount == 1) {
                    if (passedZoomTouchSlop) break
                    val targetInputChange = event.changes[0]
                    val dragChange = event.calculatePan()
                    if (!passedDragTouchSlop && dragChange.getDistance() > touchSlop) {
                        if (!detectVerticalOnly() || (dragChange.y > dragChange.x.absoluteValue)) {
                            passedDragTouchSlop = true
                            onDragStart()
                        }
                    }
                    if (passedDragTouchSlop) {
                        onDrag(targetInputChange.position, dragChange)
                        velocityTracker.addPosition(targetInputChange.uptimeMillis, targetInputChange.position)
                        targetInputChange.consume()
                    }
                } else if (touchCount > 1) {
                    if (passedDragTouchSlop) break
                    val zoomChange = event.calculateZoom()
                    val panChange = event.calculatePan()

                    if (!passedZoomTouchSlop) {
                        zoom *= zoomChange
                        pan += panChange

                        val centroidSize = event.calculateCentroidSize(useCurrent = false)
                        val zoomMotion = abs(1 - zoom) * centroidSize
                        val panMotion = pan.getDistance()

                        if (zoomMotion > touchSlop ||
                            panMotion > touchSlop
                        ) {
                            passedZoomTouchSlop = true
                            onZoomStart()
                        }
                    }

                    if (passedZoomTouchSlop) {
                        val centroid = event.calculateCentroid(useCurrent = false)
                        if (zoomChange != 1f ||
                            panChange != Offset.Zero
                        ) {
                            onZoomGesture(centroid, panChange, zoomChange)
                        }
                        event.changes.fastForEach {
                            if (it.positionChanged()) {
                                it.consume()
                            }
                        }
                    }
                }

            }
        } while (!canceled && touchCount > 0)

        if (passedDragTouchSlop) {
            onDragEnd(velocityTracker.calculateVelocity())
        } else if (passedZoomTouchSlop) {
            onZoomEnd()
        }


    }
}

suspend fun LazyListState.scrollToRevealItem(index: Int) {

    val firstVisibleIndex = layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
    val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

    when {
        index <= firstVisibleIndex -> {

            animateScrollToItem(index)
        }

        index > lastVisibleIndex -> {
            val viewportSize = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset

            val targetItemInfo = layoutInfo.visibleItemsInfo.find { it.index == index }
            val itemSize = targetItemInfo?.size ?: layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0

            val offset = (viewportSize - itemSize).coerceAtLeast(0)
            animateScrollToItem(index, scrollOffset = -offset)
        }
        else -> { /* Do nothing */ }
    }
}

fun calculateDragAmountForZoom(target: Rect, bounds: Rect, drag: Offset): Offset {
    var (dragX, dragY) = drag

    // Constrain horizontal dragging
    if (target.width <= bounds.width) {
//        // If image is narrower than bounds, center it horizontally
//        dragX = bounds.left + (bounds.width - image.width) / 2f - image.left
        dragX = 0f
    } else {
        if (dragX > 0) {
            //drag ltr
            if (target.left + dragX > bounds.left) dragX = bounds.left - target.left
        } else {
            //drag rtl
            if (target.right + dragX < bounds.right) dragX = bounds.right - target.right
        }
    }

    // Constrain vertical dragging
    if (target.height <= bounds.height) {
//        // If image is shorter than bounds, center it vertically
//        dragY = bounds.top + (bounds.height - image.height) / 2f - image.top
        dragY = 0f
    } else {
        if (dragY > 0) {
            //drag down
            if (target.top + dragY > bounds.top) dragY = bounds.top - target.top
        } else {
            //drag up
            if (target.bottom + dragY < bounds.bottom) dragY = bounds.bottom - target.bottom
        }
    }

    return Offset(dragX, dragY)
}

fun Offset.pivotFractionIn(bounds: Rect, allowOob: Boolean = false): Offset {
    val coercedPivot = Offset(
        x = x.coerceIn(bounds.left, bounds.right),
        y = y.coerceIn(bounds.top, bounds.bottom)
    )

    val finalPivot = if (allowOob) this else coercedPivot

    val pivotX = if (bounds.width == 0f) 0f else (finalPivot.x - bounds.left) / bounds.width
    val pivotY = if (bounds.height == 0f) 0f else (finalPivot.y - bounds.top) / bounds.height

    var pivotFrac = Offset(pivotX, pivotY)

    log.d("pivot frac calc: $this in $bounds > $pivotFrac")

    return pivotFrac
}

fun Offset.pivotIn(bounds: Rect, allowOob: Boolean = false): Offset {
    val coercedOffset = Offset(
        x = x.coerceIn(bounds.left, bounds.right),
        y = y.coerceIn(bounds.top, bounds.bottom)
    )

    return if (allowOob) this else coercedOffset
}

fun Offset.toAbsolutePivot(bounds: Rect): Offset {
    return Offset(
        x = bounds.left + bounds.width * x,
        y = bounds.top + bounds.height * y
    )
}

fun calculateTranslationFix(dstBound: Rect, containerBound: Rect): Offset {
    val horizontalTranslation: Float = if(dstBound.width > containerBound.width) {
        //maybe gaps
        if (dstBound.left > containerBound.left) {
            containerBound.left - dstBound.left
        } else if (dstBound.right < containerBound.right) {
            containerBound.right - dstBound.right
        } else 0f
    } else{
        val centerX = containerBound.center.x
        val dstCenterX = dstBound.center.y
        centerX - dstCenterX
    }


    val verticalTranslation = if(dstBound.height > containerBound.height) {
        //maybe gaps
        if (dstBound.top > containerBound.top) {
            containerBound.top - dstBound.top
        } else if (dstBound.bottom < containerBound.bottom) {
            containerBound.bottom - dstBound.bottom
        } else 0f
    } else {
        val centerY = containerBound.center.y
        val dstCenterY = dstBound.center.y
        centerY - dstCenterY
    }

    return Offset(horizontalTranslation, verticalTranslation)
}



val Velocity.total: Float
    get() = sqrt(x*x + y*y)

fun Rect.fit(imageSize: Size): Rect {
    val scaleFactor = ContentScale.Fit.computeScaleFactor(imageSize, size)

    // Calculate the scaled dimensions of the image
    val scaledWidth = imageSize.width * scaleFactor.scaleX
    val scaledHeight = imageSize.height * scaleFactor.scaleY

    // Calculate the position to center the image within the rect
    val targetLeft = left + (size.width - scaledWidth) / 2f
    val targetTop = top + (size.height - scaledHeight) / 2f

    // Create a new Rect with the scaled dimensions and centered position
    return Rect(Offset(targetLeft, targetTop), Size(scaledWidth, scaledHeight))
}

fun imagePivotIn(imageSize: Size, containerSize: Size): TransformOrigin {
    val containerAspectRatio = containerSize.width / containerSize.height
    val imageAspectRatio = imageSize.width / imageSize.height
    return if (imageAspectRatio > containerAspectRatio) {
        //fit width
        TransformOrigin(
            pivotFractionX = 0f,
            pivotFractionY = (1 - containerAspectRatio / imageAspectRatio) / 2f
        )
    } else {
        TransformOrigin(
            pivotFractionX = (1 - imageAspectRatio / containerAspectRatio) / 2f,
            pivotFractionY = 0f
        )
    }
}

suspend fun <T : Any> loadCancellable(block: (CancellationSignal) -> T) = suspendCancellableCoroutine<T> { continuation ->
    val signal = CancellationSignal()
    continuation.invokeOnCancellation {
        signal.cancel()
    }
    try {
        val result = block(signal)
        continuation.resume(result)
    } catch (e: Exception) {
        continuation.resumeWithException(e)
    }
}
