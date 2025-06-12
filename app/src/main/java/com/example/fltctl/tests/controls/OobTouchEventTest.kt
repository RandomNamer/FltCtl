package com.example.fltctl.tests.controls

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.text.TextPaint
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import com.example.fltctl.controls.arch.FloatingControlInfo
import com.example.fltctl.tests.UiTest
import com.example.fltctl.utils.androidLogs
import com.example.fltctl.widgets.view.margin
import com.example.fltctl.widgets.view.takeDp
import com.example.fltctl.widgets.view.takeDpAsFloat

/**
 * Created by zeyu.zyzhang on 5/12/25
 * @author zeyu.zyzhang@bytedance.com
 */
class OobTouchEventTest: UiTest.FltCtlUiTest() {

    init {
//        AppMonitor.addStartupTestPage(this)
    }

    private lateinit var tv: TextView

    private lateinit var tv2: TextView

    private lateinit var tv3: TextView


    private lateinit var gestureV: View

    private lateinit var fullScreenGestureV2: View

    private val currentTouchPoints = mutableListOf<PointF>()

    private val currentTouchIds = mutableListOf<Int>()

    private val log by androidLogs(this::class.java.simpleName)

    override fun onCreateView(context: Context): View {
        log.enable(false)
        return FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            addView(
                object: View(context) {
                    private val textPaint = TextPaint().apply {
                        color = Color.BLACK
                        textSize = 14.takeDpAsFloat()
                    }
                    override fun onDraw(canvas: Canvas) {
                        canvas.drawText("Android View", 0f, -textPaint.ascent(), textPaint)
                    }

                    override fun onTouchEvent(event: MotionEvent?): Boolean {
                        currentTouchPoints.clear()
                        currentTouchIds.clear()
                        event ?: run {
                            syncInfo(null)
                            return false
                        }
                        val fingers = event.pointerCount
                        when(event.actionMasked) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                (0 until fingers).forEach {
                                    currentTouchPoints.add(PointF(event.getX(it), event.getY(it)))
                                    currentTouchIds.add(event.getPointerId(it))
                                }
                            }
                            else -> {}
                        }
                        syncInfo(event)
                        return true
                    }
                }.apply {
                    layoutParams = FrameLayout.LayoutParams(100.takeDp(), 100.takeDp()).apply {
                        gravity = Gravity.CENTER
                    }
                    background = ColorDrawable(Color.RED)
                }.also {
                    gestureV = it
                }
            )
            addView(
                ComposeView(context).apply {
                    setContent {
                        Box(
                            Modifier
                                .size(100.dp)
                                .background(androidx.compose.ui.graphics.Color.Magenta)
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        val firstEvent = awaitFirstDown()
                                        do {
                                            val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Main)
                                            syncInfoCompose(event)
                                            event.changes.fastForEach { it.consume() }
                                        } while (event.changes.fastAny { it.pressed })
                                        syncInfoCompose(null)
                                    }
                                }
                        ) {
                            Text("Compose")
                        }
                    }
                }.apply {
                    layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    }
                }
            )

            addView(
                LinearLayout(context).apply {
                    layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                        gravity = Gravity.CENTER or Gravity.BOTTOM
                    }
                    margin(start = 10.takeDp(), bottom = 20.takeDp())

                    orientation = LinearLayout.VERTICAL

                    addView(
                        TextView(context).apply {
                            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                                setMargins(0,0,0,10.takeDp())
                            }
                            text = "Android"
                            textSize = 24f
                        }
                    )

                    addView(
                        TextView(context).apply {
                            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                                setMargins(0,0,0,10.takeDp())
                            }
                            text = "Current finger count:"
                            textSize = 16f
                        }.also {
                            tv = it
                        }
                    )

                    addView(
                        TextView(context).apply {
                            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                                setMargins(0,0,0,10.takeDp())
                            }
                            text = "Current Touch Event type: "
                            textSize = 16f
                        }.also {
                            tv2 = it
                        }
                    )

                    addView(
                        TextView(context).apply {
                            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                                setMargins(0, 0, 0, 10.takeDp())
                            }
                            text = "Compose"
                            textSize = 24f
                        }
                    )

                    addView(
                        TextView(context).apply {
                            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                                setMargins(0, 0, 0, 10.takeDp())
                            }
                            text = "Current touches on Compose: "
                            textSize = 16f
                        }.also {
                            tv3 = it
                        }
                    )
                }
            )
        }
    }

    private fun syncInfo(ev: MotionEvent? = null) {
        log.d("$currentTouchPoints, $ev")

        val selfBounds = Rect(0, 0, gestureV.width, gestureV.height)
        val oobPoints = currentTouchPoints.filter {
            !selfBounds.contains(it.x.toInt(), it.y.toInt())
        }
        tv.text = "${currentTouchPoints.size} fingers: $currentTouchIds, oob count: ${oobPoints.size}"

        val evStr = when(ev?.actionMasked) {
            MotionEvent.ACTION_DOWN -> "ACTION_DOWN"
            MotionEvent.ACTION_MOVE -> "ACTION_MOVE"
            MotionEvent.ACTION_UP -> "ACTION_UP"
            MotionEvent.ACTION_CANCEL -> "ACTION_CANCEL"
            null -> "null"
            else -> "Unknown"
        }


        tv2.text = "Current Touch Event type: $evStr"

    }

    private fun syncInfoCompose(ev: PointerEvent? = null) {
        tv3.text = "${ev?.changes?.size.toString()} touches on Compose: ${ev?.changes?.fastMap { it.id.value } ?: ""}"
    }

    override val info: FloatingControlInfo
        get() = FloatingControlInfo(
            displayName = "Oob Touch Events",
            klass = OobTouchEventTest::class,
            desc = "Test for Out-of-bounds touch events"
        )
}