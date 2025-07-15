package com.example.fltctl.tests.compose.tutorials.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import com.example.fltctl.tests.UiTest
import com.example.fltctl.utils.logs

/**
 * Created by zeyu.zyzhang on 6/27/25
 * @author zeyu.zyzhang@bytedance.com
 */
val layoutModifierChaining = UiTest.ComposeUiTest(
    title = "Layout Modifier Chaining",
    description = "Test the usage of layout modifier chaining.",
    content = {
        val log by logs("layoutModifierChaining")
        Box(modifier = Modifier.fillMaxSize().wrapContentSize(align = Alignment.Center).size(100.dp).background(Color.Blue))
        Box(
            modifier = Modifier
//                .fillMaxSize()
//                .wrapContentSize()
                .layout { measurable, constraints ->
                    val verticalAvailable = constraints.maxHeight
                    val centerX = constraints.maxWidth / 2
                    val centerY = verticalAvailable * 0.5f
                    val placeable = measurable.measure(constraints)
                    log.d("${placeable.width}, ${placeable.height}, $centerX, $centerY")
                    layout(placeable.width, placeable.height) {
                        placeable.place(x = centerX - placeable.width / 2, y = (centerY - placeable.height / 2f).toInt(), zIndex = 0f)
                    }
                }
                .size(100.dp)
                .padding(25.dp)
                .background(Color.Yellow)
        )
    },
).also {
//    AppMonitor.addStartupTestPage(it)
}