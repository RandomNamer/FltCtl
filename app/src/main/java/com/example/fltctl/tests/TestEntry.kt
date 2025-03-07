package com.example.fltctl.tests

import android.content.Context
import com.example.fltctl.controls.arch.FloatingControlInfo
import com.example.fltctl.tests.controls.FloatingControlIntegrationTest

/**
 * Created by zeyu.zyzhang on 3/6/25
 * @author zeyu.zyzhang@bytedance.com
 */
object TestEntry {
    private val testableControls = listOf<FloatingControlInfo>(

    )

    fun enterFltCtlIntegrationTest(context: Context) {
        ViewBasedTestsContainerActivity.launch(context, FloatingControlIntegrationTest.info)
    }

}