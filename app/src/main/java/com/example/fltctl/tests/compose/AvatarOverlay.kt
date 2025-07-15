package com.example.fltctl.tests.compose

import android.graphics.Path
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.util.fastFirst
import androidx.core.graphics.toRect
import com.example.fltctl.tests.UiTest
import com.example.fltctl.ui.theme.FltCtlTheme
import com.example.fltctl.utils.logs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Created by zeyu.zyzhang on 6/24/25
 * @author zeyu.zyzhang@bytedance.com
 */
val pathedAvatarOverlay = UiTest.ComposeUiTest(
    title = "Pathed Avatar Overlay",
    description = "Overlay a pathed avatar on top of the screen",
    content = {
        CombinedAvatar()
    }
).also {
//    AppMonitor.addStartupTestPage(it)
}

private data class DotConfig(
    val centerOffsetDp: Offset,
    val innerRadius: Dp,
    val outerRadius: Dp,
    val backgroundColor: Color
) {
    fun cutoutShape(srcShape: Shape) = object: Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ): Outline {
                val srcOutline = srcShape.createOutline(size, layoutDirection, density)
                val srcPath = when(srcOutline) {
                    is Outline.Generic -> srcOutline.path
                    is Outline.Rectangle -> androidx.compose.ui.graphics.Path().apply { addRect(srcOutline.rect) }
                    is Outline.Rounded -> androidx.compose.ui.graphics.Path().apply { addRoundRect(srcOutline.roundRect) }
                }
                val outerRadiusPx = outerRadius.value * density.density
                val center = size.dotCenterRelative(density, layoutDirection)

                val dotPath = Path().apply {
                    addCircle(center.x, center.y, outerRadiusPx, Path.Direction.CW)
                }.asComposePath()
                val cutoutPath = androidx.compose.ui.graphics.Path().apply {
                    op(srcPath, dotPath, PathOperation.Difference)
                }
                return Outline.Generic(cutoutPath)
            }

        }

    fun Size.dotCenterRelative(density: Density, layoutDirection: LayoutDirection): Offset {
        var (dx, dy) = centerOffsetDp * density.density
        if (layoutDirection == LayoutDirection.Rtl) dx = -dx
        return Offset(
            x = if (dx >= 0) dx else width + dx,
            y = if (dy >= 0) dy else height + dy
        )
    }

    companion object {
        val Design = DotConfig(
            innerRadius = 20.dp,
            centerOffsetDp = Offset(x = -16f, y = -20f),
            outerRadius = 24.dp,
            backgroundColor = Color.Black
        )
    }
}

@Composable
private fun DotOverlayLayout(
    modifier: Modifier = Modifier,
    dotConfig: DotConfig,
    dotContent: @Composable (Modifier) -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    androidx.compose.ui.layout.Layout(
        content = {
            dotContent(Modifier.layoutId("dot"))
            content(Modifier.layoutId("content"))
        },
        modifier = modifier,
        measurePolicy = { measurables, constraints ->
            val dot = measurables.fastFirst { it.layoutId == "dot" }
            val content = measurables.fastFirst { it.layoutId == "content" }
            val r = dotConfig.innerRadius.value * density.density
            val dotPlaceable = dot.measure(Constraints.fixed((r * 2).roundToInt(), (r * 2).roundToInt()))
            val contentPlaceable = content.measure(constraints)
            val dotCenter = with(dotConfig) {
                Size(contentPlaceable.width.toFloat(), contentPlaceable.height.toFloat()).dotCenterRelative(density, layoutDirection)
            }
            val dotBoundsRelative = android.graphics.RectF(
                dotCenter.x - r,
                dotCenter.y - r,
                dotCenter.x + r,
                dotCenter.y + r
            ).toRect()
            val finalBounds = android.graphics.Rect(
                min(dotBoundsRelative.left, 0),
                min(dotBoundsRelative.top, 0),
                max(dotBoundsRelative.right, contentPlaceable.width),
                max(dotBoundsRelative.bottom, contentPlaceable.height)
            )
            val coordShift = IntOffset(
                if (dotBoundsRelative.left < 0) -dotBoundsRelative.left else 0,
                if (dotBoundsRelative.top < 0) -dotBoundsRelative.top else 0
            )
            layout(width = finalBounds.width(), height = finalBounds.height()) {
                contentPlaceable.place(IntOffset(0, 0) + coordShift, zIndex = 1f)
                val dotOrigin = dotCenter - Offset(r, r) + coordShift.toOffset()
                dotPlaceable.place(dotOrigin.x.roundToInt(), dotOrigin.y.roundToInt(), zIndex = 2f)
            }
        })
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CombinedAvatar() {

    val log by logs("CombinedAvatar")

    var dotConfig by remember { mutableStateOf(DotConfig.Design) }

    var useRtl by remember { mutableStateOf(false) }

    val scaffoldState = androidx.compose.material3.rememberBottomSheetScaffoldState()

    LaunchedEffect(Unit) {
        scaffoldState.bottomSheetState.expand()
    }

    CompositionLocalProvider(LocalLayoutDirection provides if (useRtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        FltCtlTheme {
            androidx.compose.material3.BottomSheetScaffold(scaffoldState = scaffoldState, sheetPeekHeight = 120.dp + BottomSheetDefaults.SheetPeekHeight, sheetContent = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.height(120.dp), contentAlignment = Alignment.Center) {
                        Column {
                            Text("Current config: $dotConfig")
                            Spacer(Modifier.height(8.dp))
                            Text("isRTL: $useRtl")
                        }
                    }
                    Row(modifier = Modifier.toggleable(value = useRtl, role = Role.Checkbox, onValueChange = {
                        useRtl = it
                    }), verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Checkbox(checked = useRtl, onCheckedChange = null)
                        Spacer(Modifier.size(8.dp))
                        Text ("Use RTL")
                    }

                    Spacer(Modifier.height(16.dp))

                    Text("Inner Radius: ${dotConfig.innerRadius.value.roundToInt()} dp")
                    Slider(
                        value = dotConfig.innerRadius.value,
                        onValueChange = {
                            val newInnerRadius = it.dp
                            dotConfig = dotConfig.copy(innerRadius = newInnerRadius)
                        },
                        valueRange = 10f..dotConfig.outerRadius.value,
                        steps = (40f - dotConfig.innerRadius.value).toInt()
                    )

                    Text("Outer Radius: ${dotConfig.outerRadius.value.roundToInt()} dp")
                    Slider(
                        value = dotConfig.outerRadius.value,
                        onValueChange = {
                            val newOuterRadius = it.dp
                            dotConfig = dotConfig.copy(outerRadius = newOuterRadius,)
                        },
                        valueRange = dotConfig.innerRadius.value..40f,
                        steps = (40f - dotConfig.innerRadius.value).toInt()
                    )

                    Text("Offset X: ${dotConfig.centerOffsetDp.x.roundToInt()} dp")
                    Slider(
                        value = dotConfig.centerOffsetDp.x,
                        onValueChange = {
                            dotConfig = dotConfig.copy(centerOffsetDp = dotConfig.centerOffsetDp.copy(x = it))
                        },
                        valueRange = -200f..200f
                    )

                    Text("Offset Y: ${dotConfig.centerOffsetDp.y.roundToInt()} dp")
                    Slider(
                        value = dotConfig.centerOffsetDp.y,
                        onValueChange = {
                            dotConfig = dotConfig.copy(centerOffsetDp = dotConfig.centerOffsetDp.copy(y = it))
                        },
                        valueRange = -200f..200f
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        dotConfig = DotConfig.Design
                    }) {
                        Text("Reset")
                    }
                    Spacer(Modifier.height(16.dp))

                }
            }) { pv ->
//                log.d(scaffoldState.bottomSheetState.requireOffset().toString())
                Box(Modifier.fillMaxSize().padding(top = 100.dp).background(Color.Yellow), contentAlignment = Alignment.TopCenter) {
                    DotOverlayLayout(
                        dotConfig = dotConfig,
                        dotContent = {
                            Icon(imageVector = Icons.Filled.BorderColor, contentDescription = "Edit", tint = Color.White, modifier = it.drawBehind {
                                drawCircle(
                                    color = dotConfig.backgroundColor,
                                    radius = dotConfig.innerRadius.toPx()
                                )
                            }.padding(10.dp))
                        },
                        content = {
                            Image(
                                painterResource(id = com.example.fltctl.R.drawable.avatar_sq_1),
                                contentDescription = "Avatar",
                                modifier = it
                                    .size(142.dp)
                                    .clip(dotConfig.cutoutShape(RoundedCornerShape(48.dp)))
                            )
                        }

                    )
                }
            }
        }
    }

}