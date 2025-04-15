package com.example.fltctl.tests

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import com.example.fltctl.AppMonitor
import com.example.fltctl.R
import com.example.fltctl.controls.arch.FloatingControl
import com.example.fltctl.controls.arch.FloatingControlInfo

/**
 * Created by zeyu.zyzhang on 3/7/25
 * @author zeyu.zyzhang@bytedance.com
 */
interface ControlInfoEncapsulated {
    val info: FloatingControlInfo
}

data class SimpleMenuItem(
    @DrawableRes val iconRes: Int,
    val title: String,
    val callback: (context: Activity) -> Unit
)

sealed interface UiTest {
    val title: String
    val description: String
    fun onEntryShow() {}
    fun onActivityCreate(activity: AppCompatActivity) {}
    fun produceMenuItems(context: Activity): List<SimpleMenuItem> = emptyList()

    abstract class FltCtlUiTest: FloatingControl(), ControlInfoEncapsulated, UiTest {
        override val title: String
            get() = info.displayName
        override val description: String
            get() = info.desc
    }

    data class ComposeUiTest(
        override val title: String, override val description: String = "",
        val content: @Composable BoxScope.() -> Unit,
        val fullscreen: Boolean = false
    ): UiTest {
        override fun onActivityCreate(activity: AppCompatActivity) {
            activity.setTheme(R.style.Theme_FltCtl)
        }
    }


    data class ViewProviderUiTest(
        override val title: String, override val description: String = "",
        val viewProvider: (Context) -> View,
    ): UiTest

    data class XmlUiTest(
        val layoutId: Int,
        override val title: String = AppMonitor.appContext.resources.getResourceEntryName(layoutId),
        override val description: String = ""
    ) : UiTest

}