package com.example.fltctl.tests.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled._3dRotation
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.fltctl.tests.UiTest
import com.example.fltctl.ui.theme.FltCtlTheme
import com.example.fltctl.utils.androidLogs
import com.example.fltctl.widgets.composable.EInkCompatCard

/**
 * Created by zeyu.zyzhang on 8/26/25
 * @author zeyu.zyzhang@bytedance.com
 */

val compositionLessAnimations = UiTest.ComposeUiTest(
    title = "Composition Less Animations",
    description = "Test the usage of composition less animations.",
    content = {
        FltCtlTheme {
            Screen()
        }
    },
)

private val log by androidLogs("CompositionLessAnimations")

@Composable
private fun Screen() {
    Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 20.dp)) {
        InfiniteRotateAnim()
    }
}

@Composable
private fun InfiniteRotateAnim() {

   var enableAnimState by remember { mutableStateOf(false) }

    EInkCompatCard(conformToRecommendedPadding = true) {
        Icon(
            imageVector = Icons.Filled._3dRotation, contentDescription = null, modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .infiniteRotate(enabled = enableAnimState)
        )
        Spacer(Modifier.fillMaxWidth().height(20.dp))
        Button(onClick = { enableAnimState = !enableAnimState }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(if (enableAnimState) "Disable" else "Enable")
        }
    }
}

fun Modifier.infiniteRotate(
    enabled: Boolean = true,
    direction: Path.Direction = Path.Direction.Clockwise,
    animationSpec: InfiniteRepeatableSpec<Float> = infiniteRepeatable(
        animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Restart
    )
): Modifier = composed {
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(enabled, direction) {
        if (enabled) {
            val targetValue = if (direction == Path.Direction.Clockwise) 360f else -360f
            rotation.snapTo(0f)
            rotation.animateTo(
                targetValue = targetValue,
                animationSpec = animationSpec
            )
        } else {
            rotation.stop()
        }
    }

    this.graphicsLayer {
        rotationZ = if (enabled) rotation.value else 0f
    }
}