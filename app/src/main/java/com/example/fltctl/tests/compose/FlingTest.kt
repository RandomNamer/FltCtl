package com.example.fltctl.tests.compose

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.fltctl.tests.UiTest
import com.example.fltctl.ui.theme.FltCtlTheme
import com.example.fltctl.utils.globalRandom
import com.example.fltctl.widgets.composable.EInkCompatCard
import kotlin.math.abs

/**
 * Created by zeyu.zyzhang on 4/21/25
 * @author zeyu.zyzhang@bytedance.com
 */
val flingTest = UiTest.ComposeUiTest(
    title = "Fling Test",
    description = "Test different fling behaviors in a LazyColumn.",
    content = { FltCtlTheme {
        Surface {
            FlingScreen()
        }
    } },
)

enum class DecayType {
    SPLINE, EXPONENTIAL
}

@Composable
fun FlingScreen() {
    var selectedDecayType by remember { mutableStateOf(DecayType.SPLINE) }
    var sliderPosition by remember { mutableFloatStateOf(0.2125f) } // Initial position corresponding to default friction 1f in the first stage
    var frictionMultiplier by remember { mutableFloatStateOf(1f) }
    val density = LocalDensity.current
    var sliderWidth by remember { mutableStateOf(IntSize.Zero) }
    val list = List(1000) {
        LoremIpsum(globalRandom.nextInt(3, 20)).values.first()
    }

    // Calculate frictionMultiplier based on sliderPosition
    LaunchedEffect(sliderPosition) {
        frictionMultiplier = if (sliderPosition <= 0.6f) {
            // Stage 1: 0.15 to 1.0 (maps 0.0-0.6 slider range)
            0.15f + (sliderPosition / 0.6f) * (1.0f - 0.15f)
        } else {
            // Stage 2: 1.0 to 4.0 (maps 0.6-1.0 slider range)
            1.0f + ((sliderPosition - 0.6f) / 0.4f) * (4.0f - 1.0f)
        }
    }

    val flingBehavior = remember(selectedDecayType, frictionMultiplier) {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                val decayAnimationSpec: DecayAnimationSpec<Float> = when (selectedDecayType) {
                    DecayType.SPLINE -> splineBasedDecay<Float>(density = density)
                    DecayType.EXPONENTIAL -> exponentialDecay(frictionMultiplier = frictionMultiplier)
                }
                return if (abs(initialVelocity) > 1f) {
                    var velocityLeft = initialVelocity
                    var lastValue = 0f
                    AnimationState(initialValue = 0f, initialVelocity = initialVelocity).animateDecay(decayAnimationSpec) {
                        val delta = value - lastValue
                        val consumed = scrollBy(delta)
                        lastValue = value
                        velocityLeft = this.velocity
                        // avoid rounding errors and stop if anything is unconsumed
                        if (abs(delta - consumed) > 0.5f) this.cancelAnimation()
                        if (abs(velocityLeft) < 1f) this.cancelAnimation()
                    }
                    velocityLeft
                } else {
                    initialVelocity
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        EInkCompatCard(isInEInkMode = false, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Fling Behavior Controls", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                Text("Decay Type:")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DecayType.values().forEach { decayType ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 16.dp)) {
                            RadioButton(
                                selected = selectedDecayType == decayType,
                                onClick = { selectedDecayType = decayType }
                            )
                            Text(decayType.name)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (selectedDecayType == DecayType.EXPONENTIAL) {
                    Text("Friction Multiplier: ${String.format("%.2f", frictionMultiplier)}")
                    Box(modifier = Modifier.fillMaxWidth()) { // Container for Slider and Separator
                        Slider(
                            value = sliderPosition,
                            onValueChange = { sliderPosition = it },
                            valueRange = 0f..1f, // Slider visually represents 0% to 100%
                            modifier = Modifier
                                .fillMaxWidth()
                                .onSizeChanged { sliderWidth = it } // Get slider width
                        )
                        // Visual separator at 60% mark
                        val trackHeight = 4.dp // Approximate default Slider track height
                        val thumbRadius = 10.dp // Approximate default Slider thumb radius
                        Box(
                            modifier = Modifier
                                .offset(x = (with(density) { sliderWidth.width.toDp() } * 0.6f) - (1.dp / 2), y = (thumbRadius - trackHeight / 2))
                                .width(1.dp)
                                .height(trackHeight)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                .alpha(if (sliderWidth.width > 0) 1f else 0f) // Hide until width is known
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            flingBehavior = flingBehavior
        ) {
            items(list.count(), key = { it }) { index -> // Effectively infinite for practical purposes
                Text(
                    text = "Item $index: ${list[index]}",
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}