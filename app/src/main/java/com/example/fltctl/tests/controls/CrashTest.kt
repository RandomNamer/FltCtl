package com.example.fltctl.tests.controls

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.fltctl.controls.arch.FloatingControlInfo
import com.example.fltctl.tests.UiTest
import com.example.fltctl.utils.chance

/**
 * Created by zeyu.zyzhang on 4/10/25
 * @author zeyu.zyzhang@bytedance.com
 */
class CrashTest: UiTest.FltCtlUiTest() {

    companion object {
        private val staticInfo = FloatingControlInfo(
            displayName = "Crash Test",
            klass = CrashTest::class
        )
    }

    override fun onActivityCreate(activity: AppCompatActivity) {
        super.onActivityCreate(activity)
    }

    override fun onEntryShow() {
        chance(-1) {
            throw RuntimeException("Crash onEntryShow")
        }
    }

    override fun onCreateView(context: Context): View {
        chance(30) {
            (context as Activity)!!::class.java.getDeclaredMethod("getSharedPrefsFile").invoke(context, "/data/114514.xml")
        }
        return FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            addView(Button(context).apply {
                text = "crash it"
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER
                }
                setOnClickListener {
                    throw RuntimeException("User init crash")
                }
            })
        }
    }

    override val info: FloatingControlInfo
        get() = staticInfo
}