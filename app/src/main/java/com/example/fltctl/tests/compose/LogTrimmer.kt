import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.fltctl.configs.SettingKeys
import com.example.fltctl.configs.SettingsCache
import com.example.fltctl.tests.UiTest
import com.example.fltctl.tests.controls.SimpleLogAdapter.LogViewHolder.Companion.logLevelAsColor
import com.example.fltctl.ui.theme.FltCtlTheme
import com.example.fltctl.ui.theme.LocalEInkMode
import com.example.fltctl.utils.MmapLogProxy
import com.example.fltctl.utils.logLevel
import com.example.fltctl.utils.track
import com.example.fltctl.widgets.composable.DualStateListDialog
import com.example.fltctl.widgets.composable.DualStateListItem
import com.example.fltctl.widgets.composable.EInkCompatCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Data class to hold log file information
 */
data class LogFileInfo(
    val fileName: String,
    val diskSize: Long,
    val realSize: Int,
    val isCurrent: Boolean
)

internal val logTrimmer = UiTest.ComposeUiTest(
    title = "Log Trimmer",
    description = "",
    content = { LogTrimmerScreen() },
    fullscreen = true
)

/**
 * UI for manual log trimming
 */
@Composable
fun LogTrimmerScreen() {
    var logFiles by remember { mutableStateOf<List<LogFileInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Load log files info
    suspend fun loadLogFiles() {
        isLoading = true
        withContext(Dispatchers.IO) {
            track("getLogListIO"){
                logFiles = MmapLogProxy.getInstance().listLogFiles().map { (f, lp) ->
                    LogFileInfo(
                        fileName = f.name,
                        diskSize = f.length(),
                        realSize = lp,
                        isCurrent = false
                    )
                }.asReversed()
            }
        }
        isLoading = false
    }

    fun trimLogs() {
        scope.launch {
            isLoading = true
            withContext(Dispatchers.IO) {
                track("trimLogsIO") {
                    MmapLogProxy.getInstance().trim()
                }
                loadLogFiles()
            }
        }
    }

    // Load files on first composition
    LaunchedEffect(Unit) {
        scope.launch {
            loadLogFiles()
        }
    }

    FltCtlTheme(
        eInkTheme = SettingsCache[SettingKeys.UI_EINK_MODE] ?: false
    ) {
        Surface {
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)) {
                Text(
                    text = "Log Files",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Button(
                        onClick = {
                            trimLogs()
                        },
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(bottom = 8.dp)
                    ) {
                        Text("Trim Now")
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(logFiles) { fileInfo ->
                            LogFileItem(
                                fileInfo,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogFileItem(
    fileInfo: LogFileInfo,
) {
    var showLogContent by remember { mutableStateOf(false) }
    
    if (showLogContent) {
        LogContentDialog(
            fileInfo = fileInfo,
            onDismiss = {
                showLogContent = false
            }
        )
    }
    
    EInkCompatCard (
        isInEInkMode = LocalEInkMode.current,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                showLogContent = true
            },
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileInfo.fileName + (if (fileInfo.isCurrent) " (Current)" else ""),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Disk size: ${formatFileSize(fileInfo.diskSize)}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "Actual content: ${formatFileSize(fileInfo.realSize.toLong())}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

            }
        }
    }
}

@Composable
fun LogContentDialog(fileInfo: LogFileInfo, onDismiss: () -> Unit) {
    val contentState = produceState<List<DualStateListItem<Int>>>(listOf(DualStateListItem(true, "Loading", null))) {
       launch(Dispatchers.IO) {
           track("LoadLogFileContent-${fileInfo.fileName}") {
               val logString = MmapLogProxy.getInstance().getLogsOfFile(fileInfo.fileName)
               value = logString.lines().map { DualStateListItem(true, it, it.logLevel().logLevelAsColor()) }
           }
       }
    }

    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    FltCtlTheme(darkTheme = false) {
        DualStateListDialog(
            items = contentState.value,
            title = "Log Content",
            onItemSelected = {},
            customItemContent = {
                Text(
                    text = it.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(it.payload ?: android.graphics.Color.BLACK),
                    modifier = Modifier.padding(end = 8.dp)
                )
            },
            eInkMode = LocalEInkMode.current,
//            mainAction = "Export" to {
//                scope.launch(Dispatchers.IO) {
//                    val text = MmapLogProxy.getInstance().getLogsOfFile(fileInfo.fileName)
//                    withContext(Dispatchers.Main) {
//                        val shareIntent = Intent().apply {
//                            action = Intent.ACTION_SEND
//                            putExtra(Intent.EXTRA_TEXT, text)
//                            type = "text/plain"
//
//                        }
//                        ctx.startActivity(Intent.createChooser(shareIntent, fileInfo.fileName))
//                    }
//
//                }
//
//            },
            onDismissRequest = onDismiss,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Format file size in human-readable format
 */
private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> String.format("%.2f MB", size / (1024.0 * 1024.0))
    }
}