package com.example.fltctl.widgets.composable

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBox
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.fltctl.ui.takeNext
import kotlin.math.roundToInt

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
    frontLayerShape: RoundedCornerShape = RoundedCornerShape(
        topStart = 12.dp,
        topEnd = 12.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    ),
    frontLayerPadding: PaddingValues = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
    frontLayerElevation: Float = 4f,
    backLayerPeekHeight: Dp = 0.dp,
    enableGesture: Boolean = false,
    expandInitially: Boolean = false,
    backLayerContent: @Composable BoxScope.() -> Unit = { Spacer(modifier = Modifier.height(4.dp)) },
    frontLayerContent: @Composable BoxScope.() -> Unit
) {
    val peekHeightPx = backLayerPeekHeight.toPxFloat()
    val frontLayerVisibleHeightPx = remember {
        mutableStateOf(if (expandInitially) peekHeightPx else 0f)
    }
    val expanded by remember {
        derivedStateOf { frontLayerVisibleHeightPx.value == peekHeightPx }
    }
    val collapsed by remember {
        derivedStateOf { frontLayerVisibleHeightPx.value == 0f }
    }
    val backLayerNestedScrollConnection = remember {
        object: NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (enableGesture) {
                    var remaining: Offset = available.copy(y = 0f)
                    val newHeight = available.y + frontLayerVisibleHeightPx.value
                    if (newHeight > peekHeightPx) remaining = remaining.copy(y = newHeight - peekHeightPx)
                    if (newHeight < 0f) remaining = remaining.copy(y = newHeight)
                    frontLayerVisibleHeightPx.value = newHeight.coerceIn(0f, peekHeightPx)
                    return remaining
                } else return available
            }
        }
    }
    Scaffold(modifier, topBar, bottomBar, snackbarHost, floatingActionButton, floatingActionButtonPosition, containerColor, contentColor, contentWindowInsets) { pv ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(pv)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                backLayerContent.invoke(this)
            }
            val baseFrontLayerModifier = remember {
                Modifier
                    .fillMaxSize()
                    .padding(frontLayerPadding)
                    .offset { IntOffset(x = 0, y = frontLayerVisibleHeightPx.value.roundToInt()) }
                    .graphicsLayer {
                        clip = true
                        shape = frontLayerShape
                        shadowElevation = frontLayerElevation
                    }
                    .background(color = containerColor)
            }
            Box(
                modifier = if (enableGesture) {
                    baseFrontLayerModifier
                        .nestedScroll(backLayerNestedScrollConnection)
                        .verticalScroll(rememberScrollState())
                } else baseFrontLayerModifier
            ) {
                frontLayerContent.invoke(this)
            }
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
        expandInitially = true,
        enableGesture = true,
        frontLayerElevation = 10f,
        frontLayerPadding = PaddingValues(top = 10.dp, start = 4.dp, end = 4.dp, bottom = 0.dp),
        backLayerPeekHeight = 100.dp,
        backLayerContent = {
            Spacer(modifier = Modifier
                .fillMaxSize()
                .background(Color.Cyan))
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Yellow)
        ) {
            Text(
                text ="Inner content",
                modifier = Modifier.align(Alignment.Center),
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun BackdropScaffoldAnimPreview() {

    val topBarTargetHeights = remember {
        listOf(
            50.dp,
            100.dp,
            200.dp,
            400.dp
        )
    }

    val topBarExpansionState = remember {
        MutableTransitionState(100.dp)
    }
    val transition = updateTransition(targetState = topBarExpansionState, label = "114514")
    val animatedHeight = transition.animateDp({
        tween(durationMillis = 300)
    }, label = "") {
        it.targetState
    }

    val onClickChangeHeight = {
        val next = topBarTargetHeights.takeNext(topBarExpansionState.currentState)
        Log.e("preview", "next height: $next, ${topBarExpansionState.targetState}")
        topBarExpansionState.targetState = next
    }

    BackdropScaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.mediumTopAppBarColors(containerColor = Color.Red),
                title = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text(text = "AppBar", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            )
        },
        backLayerPeekHeight = animatedHeight.value,
        enableGesture = false,
        expandInitially = true,
        backLayerContent = {
            Box(modifier = Modifier
                .fillMaxSize()
                .background(Color.White), contentAlignment = Alignment.TopStart) {
                Text(text = "current height: ${animatedHeight.value}")
            }
        }
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color.Cyan), contentAlignment = Alignment.Center) {
            Button(onClick = onClickChangeHeight) {
                Text(text = "Animate")
            }
        }
    }
}