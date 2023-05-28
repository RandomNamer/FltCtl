package com.example.fltctl.appselection.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fltctl.appselection.model.AppInfo
import com.example.fltctl.appselection.model.testAppInfo
import com.example.fltctl.widgets.composable.EInkCompatCard
import com.example.fltctl.widgets.view.takeDp
import com.example.fltctl.R
import com.example.fltctl.ui.theme.LocalEInkMode
import com.example.fltctl.ui.theme.isInEInkMode
import com.example.fltctl.ui.toLocaleDateString

@Composable
fun AppItem(info: AppInfo) {
    Row {
        Image(
            modifier = Modifier
                .size(width = 80.dp, height = 100.dp)
                .padding(horizontal = 10.dp, vertical = 20.dp),
            bitmap = info.iconBitmapCompose(60.takeDp()),
            contentDescription = null
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 10.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modifier = Modifier.weight(2f),
                    text = info.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = info.versionName,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    textAlign = TextAlign.End
                )
            }
            Text(text = "${info.packageName}", style = MaterialTheme.typography.bodySmall)
            Text(text = "${stringResource(id = R.string.app_selection_lastUsedTime)} ${info.lastUsedTime.toLocaleDateString()}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun AppItemCardWithSelection(info: AppInfoWithSelection, onSelect: (AppInfo) -> Unit, onUnselect: (AppInfo) -> Unit) {
    EInkCompatCard(isInEInkMode = isInEInkMode) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = info.selected,
                onCheckedChange = {
                    if (it) onSelect.invoke(info.value) else onUnselect.invoke(info.value)
            })
            AppItem(info = info.value)
        }
    }
}

@Composable
fun AppItemCard(info: AppInfo) {
    EInkCompatCard(isInEInkMode = isInEInkMode) {
        AppItem(info = info)
    }
}


@Preview
@Composable
fun AppItemCardPreview() {
    AppItemCardWithSelection(info = AppInfoWithSelection(testAppInfo, true), onSelect = {}, onUnselect = {})
}