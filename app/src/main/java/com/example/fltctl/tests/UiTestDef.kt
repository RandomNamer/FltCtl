package com.example.fltctl.tests

import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import com.example.fltctl.controls.arch.FloatingControl
import com.example.fltctl.controls.arch.FloatingControlInfo

/**
 * Created by zeyu.zyzhang on 3/7/25
 * @author zeyu.zyzhang@bytedance.com
 */
interface ControlInfoEncapsulated {
    val info: FloatingControlInfo
}

sealed interface UiTest {
    val title: String
    val description: String

    abstract class FltCtlUiTest: FloatingControl(), ControlInfoEncapsulated, UiTest {
        override val title: String
            get() = info.displayName
        override val description: String
            get() = info.desc
    }

    abstract class ComposeUiTest: UiTest {
        @Composable
        abstract fun BoxScope.Content()
    }

    abstract class ViewProviderUiTest: UiTest {
        abstract fun onCreateView(context: Context): View
    }

}