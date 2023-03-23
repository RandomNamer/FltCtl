package com.example.fltctl.widgets.composable

import android.animation.TypeConverter
import android.widget.Space
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Expand
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fltctl.R

@Composable
fun EInkCompatCard(
    isInEInkMode: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    colors: CardColors = CardDefaults.elevatedCardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val outlineColor = MaterialTheme.colorScheme.onBackground
    if (isInEInkMode) modifier.background(Color.White)
    Card(
        modifier = modifier,
        shape,
        if (!isInEInkMode) colors else CardDefaults.outlinedCardColors(containerColor = Color.White),
        if (!isInEInkMode) elevation else CardDefaults.cardElevation(),
        border = if (isInEInkMode) BorderStroke(2.dp, outlineColor) else border,
        content
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EInkCompatExpandableCard(
    isInEInkMode: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    colors: CardColors = CardDefaults.elevatedCardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
    EInkCompatCard(isInEInkMode, modifier, shape, colors, elevation, border) {
        val expandState = remember {
            MutableTransitionState(expandedInitially)
        }
        val transition = updateTransition(expandState, label = "")
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