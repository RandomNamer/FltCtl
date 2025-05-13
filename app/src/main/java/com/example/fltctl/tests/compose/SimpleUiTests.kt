package com.example.fltctl.tests.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.fltctl.tests.UiTest

/**
 * Created by zeyu.zyzhang on 5/6/25
 * @author zeyu.zyzhang@bytedance.com
 */

val paddingSeqTest = UiTest.ComposeUiTest(
    title = "Modifier Sequence",
    description = "Test padding sequence",
    fullscreen = true,
    content = {
        Box(Modifier.fillMaxSize().background(color = Color.Black), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .background(Color.Red)
                    .padding(
                        top = 6.dp,
                        bottom = 6.dp,
                        end = 18.dp,
                        start = 14.dp
                    )
                    .size(width = 20.dp, height = 1.dp)
                    .background(Color(0x60FFFFFF))
            )
        }
    }
)