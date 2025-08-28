package com.example.fltctl.widgets.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.fltctl.R

@Composable
fun EInkCompatCard(
    isInEInkMode: Boolean = com.example.fltctl.ui.theme.isInEInkMode,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    colors: CardColors = CardDefaults.elevatedCardColors(),
    shadowElevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    tonalElevation: Dp = 4.dp,
    border: BorderStroke? = null,
    conformToRecommendedPadding: Boolean = false,
    content: @Composable ColumnScope.(PaddingValues) -> Unit
) {
    val outlineColor = MaterialTheme.colorScheme.onBackground
    if (isInEInkMode) modifier.background(Color.White)
    val recommendedPadding = (shape as? RoundedCornerShape)?.recommendedPadding() ?: PaddingValues(0.dp)
    Card(
        modifier = modifier.wrapContentSize(),
        shape,
        if (!isInEInkMode) colors else CardDefaults.outlinedCardColors(containerColor = Color.White),
        if (!isInEInkMode) shadowElevation else CardDefaults.cardElevation(0.dp),
        border = if (isInEInkMode) BorderStroke(2.dp, outlineColor) else border,
    ) {
        Surface(Modifier.wrapContentSize(), tonalElevation = if (isInEInkMode) 0.dp else tonalElevation) {
            Column(if (conformToRecommendedPadding) Modifier.padding(recommendedPadding) else Modifier) {
                content(recommendedPadding)
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EInkCompatExpandableCard(
    isInEInkMode: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    colors: CardColors = CardDefaults.elevatedCardColors(),
    shadowElevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    tonalElevation: Dp = 4.dp,
    border: BorderStroke? = null,
    expandedInitially: Boolean = false,
    animationDuration: Int = 300,
    beforeExpandPrompt: String = stringResource(R.string.expand),
    afterExpandPrompt: String = stringResource(R.string.collapse),
    switchExpandViewModifier: Modifier.() -> Modifier = { this },
    contentAboveExpandable: @Composable ColumnScope.() -> Unit,
    contentBelowExpandable: @Composable ColumnScope.() -> Unit,
    expendableContent: @Composable ColumnScope.(Boolean) -> Unit,
) {
    EInkCompatCard(isInEInkMode, modifier, shape, colors, shadowElevation, tonalElevation, border) {
        val expandState = remember {
            MutableTransitionState(expandedInitially)
        }
        val transition = rememberTransition(expandState, label = "")
        val expandIconRotation = transition.animateFloat({
            tween(durationMillis = animationDuration)
        }, label = "") {
            if (it) 180f else 0f
        }

        contentAboveExpandable.invoke(this)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(),
            horizontalArrangement = Arrangement.End
        ) {
            Row(
                Modifier
                    .wrapContentSize()
                    .switchExpandViewModifier()
                    .clickable(enabled = true) {
                        expandState.targetState = !expandState.currentState
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AnimatedContent(
                    targetState = expandState.targetState,
                    transitionSpec = {
                        if (targetState) {
                            slideInVertically(tween(animationDuration)) { height -> height } + fadeIn(tween(animationDuration)) with
                                    slideOutVertically(tween(animationDuration)) { height -> -height  } + fadeOut(tween(animationDuration))
                        } else {
                            slideInVertically(tween(animationDuration)) { height -> -height } + fadeIn(tween(animationDuration)) with
                                    slideOutVertically(tween(animationDuration)) { height -> height } + fadeOut(tween(animationDuration))
                        }.using(SizeTransform(clip = false))
                    }
                ) {
                    Text(
                        text = if (it) afterExpandPrompt else beforeExpandPrompt,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Icon(Icons.Outlined.ExpandMore, null, Modifier.rotate(expandIconRotation.value))
            }
        }
        AnimatedVisibility(visibleState = expandState) {
            expendableContent.invoke(this@EInkCompatCard, expandState.currentState)
        }
        contentBelowExpandable.invoke(this)
    }
}

@Preview
@Composable
fun ExpandableCardPreview() {
    EInkCompatExpandableCard(
        isInEInkMode = false,
        expandedInitially = true,
        contentAboveExpandable = {
            Spacer(modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(Color.Yellow))
        },
        contentBelowExpandable = {
            Spacer(modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(Color.Red))
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.Blue),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "state: $it")
        }
    }
}

@Composable
@Preview
fun CardElevationTest() {
    Column (modifier = Modifier
        .fillMaxSize()
        .background(Color.White)) {
        EInkCompatCard(
            isInEInkMode = false,
            modifier = Modifier
                .size(200.dp, 200.dp)
                .align(Alignment.CenterHorizontally),
            tonalElevation = 6.dp,
            colors = CardDefaults.elevatedCardColors()
        ) { Text("6") }
        Spacer(Modifier.size(20.dp))
        EInkCompatCard(
            isInEInkMode = false,
            modifier = Modifier
                .size(200.dp, 200.dp)
                .align(Alignment.CenterHorizontally),
            tonalElevation = 0.dp,
            colors = CardDefaults.elevatedCardColors()
        ) { Text("0") }
        Spacer(Modifier.size(20.dp))
        EInkCompatCard(
            isInEInkMode = false,
            modifier = Modifier
                .size(200.dp, 200.dp)
                .align(Alignment.CenterHorizontally),
            tonalElevation = 30.dp,
            colors = CardDefaults.elevatedCardColors()
        ) { Text("30") }

    }
}