package com.example.fltctl.tests.compose.albumtest

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import com.example.fltctl.tests.UiTest

/**
 * Created by zeyu.zyzhang on 3/10/25
 * @author zeyu.zyzhang@bytedance.com
 * TODO: create album > create preview page > apply gestures > vid/pic dual type compat
 */
val albumPreviewUiTest = UiTest.ComposeUiTest(
    title = "Album Preview Gestures",
    content = { Album() }
)
@Composable
fun BoxScope.Album() {

}