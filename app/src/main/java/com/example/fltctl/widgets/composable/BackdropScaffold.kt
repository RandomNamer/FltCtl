package com.example.fltctl.widgets.composable

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBox
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * As Google mentioned,
 * BackdropScaffold is not currently supported by Material 3, so this is merely a simple mimic of it.
 * @see <a href="https://developer.android.com/jetpack/compose/designsystems/material2-material3">Migrate from M2 to M3</a>
 */
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackdropScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    backLayerPaddingTop: Dp = 8.dp,
    backLayerPaddingHorizontal: Dp = 8.dp,
    cornerRadius: Dp = 12.dp,
    frontLayerColor: Color = MaterialTheme.colorScheme.surface,
    backLayerColor: Color,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(modifier, topBar, bottomBar, snackbarHost, floatingActionButton, floatingActionButtonPosition, containerColor, contentColor, contentWindowInsets) { pd ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(pd)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = backLayerPaddingHorizontal,
                        end = backLayerPaddingHorizontal,
                        top = backLayerPaddingTop
                    ).background(color = frontLayerColor)) {
                content.invoke(PaddingValues(top = cornerRadius , start = cornerRadius, end = cornerRadius))
            }
            Canvas(
                modifier = Modifier
                    .fillMaxSize(),
                onDraw = {
                    val mask = RoundRect(
                        left = backLayerPaddingHorizontal.toPx(),
                        top = backLayerPaddingTop.toPx(),
                        right = size.width - backLayerPaddingHorizontal.toPx(),
                        bottom = size.height,
                        topLeftCornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                        topRightCornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                    )
                    val p1 = Path().apply {
                        addRoundRect(mask)
                    }
                    val p2 = Path().apply {
                        addRect(Rect(0f, 0f, size.width, size.height))
                    }

                    p1.op(p1, p2, PathOperation.Xor)

//                    drawPath(p1, SolidColor(Color.Red))

                    clipPath(path = p1) {
                        drawRect(backLayerColor)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun BackdropScaffoldPreview() {
    BackdropScaffold(
        topBar = { TopAppBar(
            title = { Text(text = "Test")},
            colors = TopAppBarDefaults.mediumTopAppBarColors(
                containerColor = Color.Red
            )
        ) },
        bottomBar = {
            BottomAppBar(containerColor = Color.Magenta, contentColor = Color.White) {
                Icon(Icons.Rounded.Build, null)
                Icon(Icons.Rounded.AccountBox, null)
            }
        },
        backLayerColor = Color.Red
    ) { pd ->
        Box(modifier = Modifier
            .background(Color.Yellow)
            .fillMaxSize()
            .padding(pd)) {
            Text(
                text ="Inner content",
                modifier = Modifier.align(Alignment.Center),
                textAlign = TextAlign.Center
            )
        }
    }
}