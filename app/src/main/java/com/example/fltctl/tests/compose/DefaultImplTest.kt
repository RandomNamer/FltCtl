package com.example.fltctl.tests.compose

import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.fltctl.tests.UiTest
import kotlinx.parcelize.Parcelize

/**
 * Created by zeyu.zyzhang on 5/7/25
 * @author zeyu.zyzhang@bytedance.com
 */
val defaultImplIssueTest = UiTest.ComposeUiTest(
    title = "Default Impl Issue",
    description = "Test for default impl issue",
    content = {
        DefaultImplTestScreen()
    }
)

interface UiEngine {
    @Composable
    fun A(color: Color, flag: Boolean, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier)

    @Composable
    fun B(text: String, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier)

    @Composable
    fun C(text: String, flag: Boolean)
}

@Parcelize
open class UiEngineDefault: UiEngine, Parcelable {
    @Composable
    override fun A(color: Color, flag: Boolean, modifier: Modifier) {
        Box(modifier = modifier
            .background(color)
            .size(100.dp))
    }

    @Composable
    override fun B(text: String, modifier: Modifier) {
        Text(text, modifier)
    }

    @Composable
    override fun C(text: String, flag: Boolean) {
        Text(text = text, style = MaterialTheme.typography.titleLarge,)
    }

//    @Composable
//    open fun D(text: String, textSize: TextUnit = 18.sp, modifier:Modifier = Modifier) {
//        Text(text = "D: $text", fontSize = textSize, modifier = modifier)
//    }

}

@Parcelize
class InheritedUiEngine: UiEngineDefault() {
    @Composable
    override fun B(text: String, modifier: Modifier) {
        Text(text = "InheritedUiEngine: $text", modifier = modifier)
    }

    @Composable
    override fun C(text: String, flag: Boolean) {
        super.C(text, flag)
    }

//    @Composable
//    override fun D(text: String, textSize: TextUnit, modifier: Modifier) {
//        Text(text = "override D: $text, $textSize & $modifier", modifier = modifier, fontSize = textSize)
//    }
}


@Composable
private fun DefaultImplTestScreen() {

    val e = remember { InheritedUiEngine() }
    val de = remember { UiEngineDefault() }

    Box(Modifier.fillMaxSize()) {
        e.A(Color.Red, true)
        TextShower(e)
        Box(Modifier.align(Alignment.BottomCenter)) {
            TextShower(de)
        }
        e.C("HelloC", true)
//        e.D("HelloD", 12.sp, Modifier.background(Color.Cyan).align(Alignment.CenterEnd))
    }
}

@Composable
private fun BoxScope.TextShower(e: UiEngine) {
    Box(Modifier.size(150.dp).align(Alignment.Center)) {
        e.B(text = "Hello", modifier = Modifier.background(Color.Yellow))
    }
}