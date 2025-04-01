package com.example.fltctl.widgets.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fltctl.R
import com.example.fltctl.ui.theme.isInEInkMode

/**
 * Created by zeyu.zyzhang on 1/20/25
 * @author zeyu.zyzhang@bytedance.com
 */

/**
 * @see AlertDialog
 * //Due to how the onDismissRequest callback works
 * // (it enforces a just-in-time decision on whether to update the state to hide the dialog)
 * // we need to unconditionally add a callback here that is always enabled,
 * // meaning we'll never get a system UI controlled predictive back animation
 * // for these dialogs
 */
@Stable
data class DualStateListItem<T:Any> (
    val enabled: Boolean,
    val text: String,
    val payload: T? = null
) {
    companion object {
        fun transformList(from: List<Pair<Boolean, String>>) = from.map { DualStateListItem<Any>(it.first, it.second) }
    }
}

fun List<Pair<Boolean, String>>.asDualStateList() = DualStateListItem.transformList(this)

private const val LIST_USE_LAZY_THRESH = 100
private const val LIST_USE_FULL_HEIGHT_THRESH = 20

@Composable
fun <T: Any> DualStateListDialog(
    items: List<DualStateListItem<T>>,
    title: String,
    onDismissRequest: () -> Unit,
    onItemSelected: (T?) -> Unit,
    mainAction: Pair<String, () -> Unit>? = null,
    footer: @Composable () -> Unit = {},
    customItemContent: (@Composable BoxScope.(DualStateListItem<T>) -> Unit)? = null,
    eInkMode: Boolean = isInEInkMode,
    modifier: Modifier = Modifier
) {
//    var dlgScale by remember { mutableFloatStateOf(1f) }
//    PredictiveBackHandler(true) {
//        try {
//            it.collect { backEvent ->
//                println("back ev $backEvent")
//                dlgScale = 1f - (backEvent.progress * 0.2f)
//            }
//            dlgScale = 0.8f
//        } catch (e: CancellationException) {
//            dlgScale = 1f
//        }
//    }
//    val modifier = Modifier.scale(dlgScale)

    @Composable
    fun ListItemImpl(idx: Int, item: DualStateListItem<T>) {
        val baseModifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.enabled) { onItemSelected(item.payload) }
        Box(modifier = if (eInkMode && idx > 0) baseModifier.borderTop(1.dp, Color.DarkGray, 16.dp) else baseModifier) {
            customItemContent?.let { it(item) } ?: Text(
                text = item.text,
                modifier = Modifier.align(Alignment.CenterStart).padding(16.dp),
                fontSize = 18.sp,
                color = if (item.enabled) MaterialTheme.colorScheme.onSurface else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled)
            )
        }
    }

    AlertDialog(
        modifier = if (eInkMode) modifier.border(width = 2.dp, color = Color.Black, shape = AlertDialogDefaults.shape) else modifier,
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        },
        text = {
            Column (Modifier.fillMaxWidth()) {
                Box(if (items.size > LIST_USE_FULL_HEIGHT_THRESH) Modifier else Modifier.height(300.dp)) {

                    if (items.size > LIST_USE_LAZY_THRESH) {
                        val lazyListState = rememberLazyListState()
                        LazyColumn(state = lazyListState) {
                            itemsIndexed(items, key = { idx, item -> idx + item.hashCode() }) { idx, item ->
                                ListItemImpl(idx, item)
                            }
                        }
                        VerticalScrollbar(
                            scrollState = lazyListState,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(vertical = 10.dp, horizontal = 5.dp)
                        )
                    } else {
                        val scrollState = rememberScrollState(0)
                        Column(Modifier.verticalScroll(scrollState)) {
                            items.forEachIndexed { p1, p2 ->
                                ListItemImpl(p1, p2)
                            }
                        }
                        VerticalScrollbar(
                            scrollState = scrollState,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(vertical = 10.dp, horizontal = 5.dp)
                        )
                    }

                }
                Spacer(Modifier.height(10.dp))
                Spacer(
                    Modifier
                        .height(10.dp)
                        .fillMaxWidth()
                        .borderTop(
                            if (eInkMode) 2.dp else 0.5.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = ContentAlpha.medium)
                        )
                )
                footer()
            }
        },
        confirmButton = {
            mainAction?.let { (actionTitle, actionOnClick) ->
                if (eInkMode) OutlinedButton(onClick = actionOnClick) {
                    Text(actionTitle)
                } else TextButton(onClick = actionOnClick) {
                    Text(actionTitle)
                }
            }
        },
        dismissButton = {
            if (eInkMode) OutlinedButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            } else TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}




@Preview
@Composable
fun SimpleSelectorDialogPreview() {
    DualStateListDialog<Any>(
        eInkMode = false,
        items = listOf(
            true to "Item 1",
            true to "Item 2",
            false to "Item 3",
            true to "Item 4",
            true to "Item 5",
            true to "Item 6",
            false to "Item 7",
            true to "Item 8",
            false to "Item 9",
            true to "Item 10"
        ).asDualStateList(),
        title = "Select One",
        mainAction = "mainAction" to {},
        footer = {
            Box(Modifier.background(Color.Red)) {
                Text(text="1145141919810")
            }
        },
        onItemSelected = {
            println(it)
        },
        onDismissRequest = {}
    )
}
