package com.example.fltctl.tests.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.fltctl.tests.UiTest

/**
 * Created by zeyu.zyzhang on 3/10/25
 * @author zeyu.zyzhang@bytedance.com
 * TODO: create album > create preview page > apply gestures > vid/pic dual type compat
 */
class AlbumPreviewUiTest: UiTest.ComposeUiTest() {

    override val title: String = "Album Preview Gestures"
    override val description: String = ""

    @Composable
    override fun BoxScope.Content() {
        Box(modifier = Modifier.size(100.dp).align(Alignment.Center).background(Color.Red)) {
            Text("${this@AlbumPreviewUiTest::class.qualifiedName}")
        }
    }


}