package com.example.fltctl.tests.scripting

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import androidx.annotation.Keep
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.fltctl.AppMonitor
import com.example.fltctl.tests.UiTest
import com.example.fltctl.ui.theme.FltCtlTheme
import com.example.fltctl.ui.toast
import com.example.fltctl.utils.LogProxy
import com.example.fltctl.utils.Logger
import com.example.fltctl.utils.MmapLogProxy
import com.example.fltctl.widgets.composable.DualStateListDialog
import com.example.fltctl.widgets.composable.DualStateListItem
import com.example.fltctl.widgets.composable.EInkCompatCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

/**
 * Created by zeyu.zyzhang on 5/14/25
 * @author zeyu.zyzhang@bytedance.com
 */
@Keep
@ConsistentCopyVisibility
data class AndroidScriptConfig internal constructor(
    val main: suspend AndroidScriptRunnerScope.() -> Unit,
    val title: String,
    val enableErrorCatching: Boolean = true,
    val enableLocalLogging: Boolean = false,
)

internal const val UNNAMED = "__unnamed"


fun script(
    name: String = UNNAMED,
    enableErrorCatching: Boolean = true,
    enableLocalLogging: Boolean = false,
    main: suspend AndroidScriptRunnerScope.() -> Unit
) = AndroidScriptConfig(
    main, name, enableErrorCatching, enableLocalLogging
).also {
    registry.add(it)
}

interface AndroidScriptRunnerScope {
    val log: Logger
    fun println(message: Any?)
    fun toast(message: String)

    /**
     * Provides concurrency via another coroutineScope on other thread
     */
    val coroutineScope: CoroutineScope
    val androidContext: Context
}

interface ScriptRunnerListener {
    fun onScriptOutput(message: String)
    fun onScriptError(error: Throwable)
    fun onScriptComplete(executionTimeMs: Long)
}

class ScriptOutputFlow {
    private val _outputFlow = MutableSharedFlow<String>(replay = 100, extraBufferCapacity = 101)
    val outputFlow = _outputFlow.asSharedFlow()
    
    suspend fun emit(message: String) {
        _outputFlow.emit(message)
    }
    
    @OptIn(ExperimentalCoroutinesApi::class)
    fun clear() {
        // Clear replay cache by creating a new flow
        // This is a workaround as SharedFlow doesn't have a clear method
        _outputFlow.resetReplayCache()
    }
}

class AndroidScriptRunner(
    private val context: Context,
    private val config: AndroidScriptConfig,
    private val listener: ScriptRunnerListener? = null
) {
    private val outputList = mutableListOf<String>()
    private var isRunning = false
    private var executionTimeMs = 0L
    private var hasError = false
    val outputFlow = ScriptOutputFlow()
    private var suppressOutput = false
    private val runnerMain = CoroutineScope(Dispatchers.Default)
    private val auxiliaryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val runnerScope = object : AndroidScriptRunnerScope {
        override val log: Logger = if (config.enableLocalLogging) {
            Logger(config.title, MmapLogProxy.getInstance()) { level, message ->
                println("[LocalFileLog] $message")
                true
            }
        } else {
            Logger(config.title, LogProxy.getAndroid())
        }
        
        override val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

        override val androidContext: Context
            get() = context

        override fun println(message: Any?) {
            if (suppressOutput) return
            val msgString = message.toString()
            outputList.add(msgString)
            auxiliaryScope.launch {
                outputFlow.emit(msgString)
            }
            listener?.onScriptOutput(msgString)
        }
        
        override fun toast(message: String) {
            if (suppressOutput) return
            auxiliaryScope.launch(Dispatchers.Main) {
                context.toast(message)
            }
        }
    }

    fun bench(maxIter: Int = 1000, warmupIter: Int = 10, timeoutMs: Long = 15000L) {
        if (isRunning) return
        isRunning = true
        hasError = false
        outputList.clear()
        outputFlow.clear()
        suppressOutput = true
        runnerScope.log.enable(false)
        runnerMain.launch {
            repeat(warmupIter) {
                try {
                    config.main(runnerScope)
                } catch (e: Exception) {}
            }
            var iter = 0
            val start = SystemClock.elapsedRealtime()
            while (iter < maxIter) {
                try {
                    config.main(runnerScope)
                } catch (e: Exception) {
                    hasError = true
                    break
                }
                iter++
                if (SystemClock.elapsedRealtime() - start > timeoutMs) {
                    break
                }
            }
            suppressOutput = false
            isRunning = false
            val time = SystemClock.elapsedRealtime() - start
            listener?.onScriptComplete(time)
            runnerScope.println("Bench completed in ${time}ms")
            runnerScope.println("Benched $iter rounds")
        }
    }
    
    suspend fun execute() {
        if (isRunning) return
        isRunning = true
        hasError = false
        outputList.clear()
        outputFlow.clear()
        suppressOutput = false
        try {
            runnerMain.launch {
                executionTimeMs = measureTimeMillis {
                    if (config.enableErrorCatching) {
                        try {
                            config.main(runnerScope)
                        } catch (e: Throwable) {
                            hasError = true
                            runnerScope.log.e("Script error: ${e.message}")
                            runnerScope.println("ERROR: ${e.message}")
                            e.stackTrace.forEach { line ->
                                runnerScope.println("  at $line")
                            }
                            listener?.onScriptError(e)
                        }
                    } else {
                        config.main(runnerScope)
                    }
                }
                delay(1)
                runnerScope.println("Script completed in ${executionTimeMs}ms")
                listener?.onScriptComplete(executionTimeMs)
            }
        } finally {
            isRunning = false
        }
    }
    
    fun getOutputs(): List<String> = outputList.toList()
    
    fun isRunning(): Boolean = isRunning
    
    fun hasError(): Boolean = hasError

    fun cancel() {
        runnerMain.cancel()
        auxiliaryScope.cancel()
        runnerScope.coroutineScope.cancel()
    }
}

private val registry = mutableSetOf<AndroidScriptConfig>()

val androidScriptingRunnerEntry = UiTest.ComposeUiTest(
    title = "Scripting Host",
    content = {
        AndroidScriptingRunnerUI()
    },
    fullscreen = true,
    onActivityCreate = {
        initializeScriptRoots()
    }
).also {
    AppMonitor.addStartupTestPage(it)
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AndroidScriptingRunnerUI() {
    val context = LocalContext.current
    
    // State for script selection and dialog visibility
    var selectedScript by remember { mutableStateOf<AndroidScriptConfig?>(null) }
    var showOutputDialog by remember { mutableStateOf(false) }
    
    // Main UI
    FltCtlTheme(densityScale = 0.9f) {
        Scaffold(
            topBar = {
                androidx.compose.material3.TopAppBar(
                    title = {
                        Text(text = "Android Script Runner",)
                    }
                )
            }

        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Android Script Runner",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Display scripts as cards in a grid layout
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    maxItemsInEachRow = 114514,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    registry.forEach { script ->
                        ScriptCard(
                            script = script,
                            onClick = {
                                selectedScript = script
                                showOutputDialog = true
                            }
                        )
                    }
                }
                
                // Show script output dialog when a script is selected
                if (showOutputDialog && selectedScript != null) {
                    ShowScriptOutputDialog(
                        script = selectedScript!!,
                        onDismiss = {
                            showOutputDialog = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScriptCard(
    script: AndroidScriptConfig,
    onClick: () -> Unit
) {
    EInkCompatCard(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 8.dp,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        )
    ) {
        Column(
            modifier = Modifier
                .wrapContentSize()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Script icon or indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = script.title.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Script title
            Text(
                text = script.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            // Script features indicator
            Column (
                modifier = Modifier.padding(top = 4.dp),
                 verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (script.enableErrorCatching) "Safe Mode" else "Raw Mode",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.size(4.dp))

                if (script.enableLocalLogging) {
                    Text(
                        text = "With Local Log",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

fun String.formatWithColor(): Pair<Color, String> = when {
    startsWith("ERROR:") -> Color.Red
    startsWith("  at ") -> Color.Red.copy(alpha = 0.7f)
    contains("completed in") -> Color.Green
    else -> Color.Unspecified
} to this


@Composable
private fun ShowScriptOutputDialog(
    script: AndroidScriptConfig,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // State for script outputs and running state
    val coloredOutputList = remember { mutableStateListOf<Pair<Color, String>>() }
    var isRunning by remember { mutableStateOf(true) }
    var executionTime by remember { mutableStateOf(0L) }
    var hasError by remember { mutableStateOf(false) }
    
    // Create script runner specifically for this dialog
    val scriptRunner = remember(script) {
        AndroidScriptRunner(
            context = context,
            config = script,
            listener = object : ScriptRunnerListener {
                override fun onScriptOutput(message: String) {
                    // We'll use the flow instead of directly adding to the list
                }
                
                override fun onScriptError(error: Throwable) {
                    hasError = true
                }
                
                override fun onScriptComplete(executionTimeMs: Long) {
                    executionTime = executionTimeMs
                    isRunning = false
                }
            }
        )
    }
    
    // Collect outputs from flow
    LaunchedEffect(scriptRunner) {
        scriptRunner.outputFlow.outputFlow.collect { message ->
            coloredOutputList.add(message.formatWithColor())
        }
    }
    
    // Execute the script when the dialog is shown
    LaunchedEffect(scriptRunner) {
        coloredOutputList.clear()
        hasError = false
        scriptRunner.execute()
    }
    
    // Clean up when dialog is dismissed
    DisposableEffect(scriptRunner) {
        onDispose {
            // Mark as not running when dialog is dismissed
            // We can't actually cancel the script execution,
            // but we can mark it as not running
            scriptRunner.cancel()
            isRunning = false
        }
    }
    
    val items = coloredOutputList.map {
        
        DualStateListItem(true, it.second, it.first)
    }
    
    val hasErrorInOutput = scriptRunner.hasError()
    
    val dialogTitle = when {
        isRunning -> "${script.title} (Running...)"
        hasErrorInOutput -> "${script.title} (Error in ${executionTime}ms)"
        else -> "${script.title} (Completed in ${executionTime}ms)"
    }

    DualStateListDialog<Color>(
        items = items,
        title = dialogTitle,
        onDismissRequest = {},
        mainAction = "Bench" to {
            scriptRunner.bench(10000, 100)
        },
        customCancelAction = null to {
            scriptRunner.cancel()
            onDismiss.invoke()
        },
        customItemContent = {
            Text(text = it.text, color = it.payload ?: Color.Unspecified)
        }
    )
}