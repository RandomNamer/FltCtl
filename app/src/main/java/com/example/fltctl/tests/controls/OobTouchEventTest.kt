package com.example.fltctl.tests.controls

import android.content.Context
import android.graphics.Color
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.example.fltctl.controls.arch.FloatingControlInfo
import com.example.fltctl.tests.UiTest
import com.example.fltctl.utils.androidLogs
import com.example.fltctl.widgets.view.margin
import com.example.fltctl.widgets.view.takeDp

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

    private lateinit var gestureV: View

    private lateinit var fullScreenGestureV2: View

    private val currentTouchPoints = mutableListOf<PointF>()

    private val log by androidLogs(this::class.java.simpleName)

    override fun onCreateView(context: Context): View {
        log.enable(false)
        return FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            addView(
                object: View(context) {
                    override fun onTouchEvent(event: MotionEvent?): Boolean {
                        currentTouchPoints.clear()
                        event ?: run {
                            syncInfo(null)
                            return false
                        }
                        val fingers = event.pointerCount
                        when(event.actionMasked) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                (0 until fingers).map {
                                    currentTouchPoints.add(PointF(event.getX(it), event.getY(it)))
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
                LinearLayout(context).apply {
                    layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                        gravity = Gravity.CENTER or Gravity.BOTTOM
                    }
                    margin(bottom = 100.takeDp())

                    orientation = LinearLayout.VERTICAL

                    addView(
                        TextView(context).apply {
                            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                                setMargins(0,0,0,10.takeDp())
                            }
                            text = "Current finger count:"
                            textSize = 24f
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
                            textSize = 14f
                        }.also {
                            tv2 = it
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
        tv.text = "Current finger count: ${currentTouchPoints.size}, oob count: ${oobPoints.size}"

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

    override val info: FloatingControlInfo
        get() = FloatingControlInfo(
            displayName = "Oob Touch Events",
            klass = OobTouchEventTest::class,
            desc = "Test for Out-of-bounds touch events"
        )
}