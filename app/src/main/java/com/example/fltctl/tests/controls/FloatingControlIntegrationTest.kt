package com.example.fltctl.tests.controls

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import com.example.fltctl.controls.arch.FloatingControlInfo
import com.example.fltctl.tests.UiTest
import com.example.fltctl.widgets.view.takeDp

/**
 * Created by zeyu.zyzhang on 3/6/25
 * @author zeyu.zyzhang@bytedance.com
 */
class FloatingControlIntegrationTest: UiTest.FltCtlUiTest() {

    override val info: FloatingControlInfo
        get() = staticInfo

    companion object {
        private val staticInfo: FloatingControlInfo = FloatingControlInfo(
            displayName = "FltCtl integration",
            klass = FloatingControlIntegrationTest::class
        )
    }
    private lateinit var textView: TextView

    override fun onCreateView(context: Context): View = with(context) {
        ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(200.takeDp(), 200.takeDp())
            background = ColorDrawable(Color.YELLOW)
            addView(
                TextView(this@with).apply {
                    layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                    text = "onCreateView"
                }.also { textView = it }
            )
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onEInkConfigChanged(isEInk: Boolean) {
        super.onEInkConfigChanged(isEInk)
        textView.append("\n${System.currentTimeMillis()}: EinkConfigChanged: $isEInk")
    }

    override fun onStop() {
        textView.append("\n${System.currentTimeMillis()}: onStop")
        super.onStop()
    }

    override fun onStart() {
        textView.append("\n${System.currentTimeMillis()}: onStart")
        super.onStart()
    }

}