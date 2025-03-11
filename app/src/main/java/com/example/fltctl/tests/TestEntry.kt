package com.example.fltctl.tests

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.fltctl.tests.compose.AlbumPreviewUiTest
import com.example.fltctl.tests.controls.FloatingControlIntegrationTest
import com.example.fltctl.tests.controls.LogViewer
import com.example.fltctl.widgets.composable.DualStateListDialog
import com.example.fltctl.widgets.composable.DualStateListItem

/**
 * Created by zeyu.zyzhang on 3/6/25
 * @author zeyu.zyzhang@bytedance.com
 */
object TestEntry {
    private val registry = mutableSetOf<UiTest>(
        FloatingControlIntegrationTest(),
        AlbumPreviewUiTest(),
        LogViewer(),
    )

    @Composable
    fun TestSelectionEntry(dismissHandle: () -> Unit) {
        val ctx = LocalContext.current
        DualStateListDialog<UiTest>(
            items = registry.map { DualStateListItem(true, it.title, it) },
            title = "Select a UI Test",
            onItemSelected = { test -> test?.let { enterTest(ctx, it) }; dismissHandle() },
            onDismiss = dismissHandle,
        )
    }

    private fun enterTest(context: Context, test: UiTest) {
        ViewBasedTestsContainerActivity.launch(context, test)
    }

    fun enterFltCtlIntegrationTest(context: Context) {
        ViewBasedTestsContainerActivity.launch(context, FloatingControlIntegrationTest().info)
    }

}