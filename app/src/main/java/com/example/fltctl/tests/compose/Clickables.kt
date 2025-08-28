package com.example.fltctl.tests.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.fltctl.tests.UiTest
import com.example.fltctl.widgets.composable.EInkCompatCard
import com.example.fltctl.widgets.composable.debouncedClickable

/**
 * Created by zeyu.zyzhang on 7/24/25
 * @author zeyu.zyzhang@bytedance.com
 */
val clickablesTest = UiTest.ComposeUiTest(
    title = "Clickables",
    description = "Test the usage of clickables.",
    content = {
        Clickables()
    },
    fullscreen = true
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Clickables() {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        ClickableTestCard("combined") { callbacks ->
            combinedClickable(
                onClick = callbacks::onClick,
                onLongClick = callbacks::onLongClick,
                onDoubleClick = {}
            )
        }
        Spacer(Modifier.size(10.dp))
        ClickableTestCard("seperate") { callbacks ->
            clickable(
                onClick = callbacks::onClick,
            )
            pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { callbacks.onLongClick() }
                )
            }
        }
        Spacer(Modifier.size(10.dp))

        ClickableTestCard("manual debounce") {
            debouncedClickable(it::onClick, 500)
        }
        Spacer(Modifier.size(10.dp))

        ClickableTestCard("no debounce") {
            clickable { it.onClick() }
        }

    }
}

private interface TestableClickCallbacks {
    fun onClick()
    fun onLongClick()
}

@Composable
private fun ClickableTestCard(
    title: String,
    clickableModifierGroup: Modifier.(TestableClickCallbacks) -> Modifier
) {
    var singleClickCount by remember { mutableIntStateOf(0) }
    var lastSingleClickGap by remember { mutableLongStateOf(0) }

    var longClickCount by remember { mutableIntStateOf(0) }
    var lastLongClickTime by remember { mutableLongStateOf(0) }

    val callbacks = remember {
        object : TestableClickCallbacks {
            private var lastSingleClickTime = 0L
            override fun onClick() {
                singleClickCount++
                lastSingleClickGap = System.currentTimeMillis() - lastSingleClickTime
                lastSingleClickTime = System.currentTimeMillis()
            }

            override fun onLongClick() {
                longClickCount++
                lastLongClickTime = System.currentTimeMillis()
            }
        }
    }

    EInkCompatCard (modifier = Modifier.fillMaxWidth(), conformToRecommendedPadding = true) { pv ->
        Box(
            modifier = Modifier
                .size(width = 200.dp, height = 100.dp)
                .background(MaterialTheme.colorScheme.primary)
                .align(Alignment.CenterHorizontally)
                .clickableModifierGroup(callbacks)
        ) {
            Text(title, color = MaterialTheme.colorScheme.onPrimary)
        }

        Text("Single clicks: $singleClickCount, click dur: $lastSingleClickGap", Modifier.padding(pv))
        Text("Long clicks: $longClickCount, last at: $lastLongClickTime", Modifier.padding(pv))
    }
}
