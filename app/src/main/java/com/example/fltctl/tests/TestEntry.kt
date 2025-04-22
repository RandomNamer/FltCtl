package com.example.fltctl.tests

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.fltctl.tests.compose.albumtest.albumPreviewUiTest
import com.example.fltctl.tests.compose.flingTest
import com.example.fltctl.tests.controls.CrashTest
import com.example.fltctl.tests.controls.FloatingControlIntegrationTest
import com.example.fltctl.tests.controls.LogViewer
import com.example.fltctl.tests.views.textViewEllipsizeTest
import com.example.fltctl.ui.theme.FltCtlTheme
import com.example.fltctl.ui.theme.LocalEInkMode
import com.example.fltctl.widgets.composable.DualStateListDialog
import com.example.fltctl.widgets.composable.DualStateListItem
import com.example.fltctl.widgets.composable.EInkCompatCard
import logTrimmer

/**
 * Created by zeyu.zyzhang on 3/6/25
 * @author zeyu.zyzhang@bytedance.com
 */
object TestEntry {
    private val registry = mutableSetOf<UiTest>(
        FloatingControlIntegrationTest(),
        albumPreviewUiTest,
        LogViewer(),
        textViewEllipsizeTest,
        CrashTest(),
        flingTest
    )

    private val debugOnly = mutableSetOf<UiTest>(
        logTrimmer
    )

    @Composable
    fun TestSelectionEntry(dismissHandle: () -> Unit) {
        val ctx = LocalContext.current
        DualStateListDialog<UiTest>(
            items = registry.map { it.onEntryShow(); DualStateListItem(true, it.title, it) },
            title = "Select a UI Test",
            onItemSelected = { test -> test?.let { enterTest(ctx, it) }; dismissHandle() },
            onDismissRequest = dismissHandle,
        )
    }

    class TestEntryListActivity : ComponentActivity() {
        @OptIn(ExperimentalMaterial3Api::class)
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val tests = registry.toMutableList().apply { addAll(debugOnly) }
//            enableEdgeToEdge()
            setContent {
                FltCtlTheme {
                    Scaffold( modifier = Modifier.fillMaxSize(),
                        topBar = { TopAppBar(title = { Text("Test Entry") }) }
                    ) { innerPadding ->
                        val listScrollState = rememberScrollState()
                        Column(
                            Modifier
                                .padding(innerPadding)
                                .padding(horizontal = 10.dp)
                                .padding(bottom = 10.dp)
                                .fillMaxWidth()
                                .verticalScroll(listScrollState)
                        ) {
                            tests.forEach { uiTest ->
                                Spacer(Modifier.height(10.dp))
                                EInkCompatCard(LocalEInkMode.current, Modifier.clickable {
                                    enterTest(this@TestEntryListActivity, uiTest)
                                }) { pv ->
                                    Column(Modifier
                                        .fillMaxWidth()
                                        .padding(pv)) {
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(uiTest.title, Modifier.weight(2f), style = MaterialTheme.typography.titleLarge)
                                            AssistChip(onClick = {}, label = {
                                                Text(
                                                    when (uiTest) {
                                                        is UiTest.ComposeUiTest, is UiTest.ComposeUiTestWrap -> "Compose"
                                                        is UiTest.XmlUiTest -> "XML"
                                                        is UiTest.ViewProviderUiTest -> "ViewProvider"
                                                        is UiTest.FltCtlUiTest -> "FltCtl"
                                                    },
                                                )
                                            })
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(uiTest.description.takeIf { it.isNotBlank() } ?: "No Description", style = MaterialTheme.typography.bodyMedium)
                                        Spacer(Modifier.height(10.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun enterTest(context: Context, test: UiTest) {
        ViewBasedTestsContainerActivity.launch(context, test)
    }

    fun enterFltCtlIntegrationTest(context: Context) {
        ViewBasedTestsContainerActivity.launch(context, FloatingControlIntegrationTest().info)
    }

}