package com.example.fltctl.utils

/**
 * Created by zeyu.zyzhang on 3/10/25
 * @author zeyu.zyzhang@bytedance.com
 */

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KProperty
import android.util.Log as AndroidLog

/**
 * Pure MmapLogger that handles memory-mapped file operations
 * Writes logs directly in plain text format
 */
class MmapLogger private constructor(
    private val context: Context,
    private val bufferSize: Int
) {
    // File and buffer management
    private var currentBuffer: MappedByteBuffer
    private var currentFile: File
    private var position = AtomicInteger(0)

    // Coroutine scope for background operations
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob()  + CoroutineExceptionHandler { _, t ->
        AndroidLog.e(TAG, "$TAG worker error: $t")
    })

    // Date formatter for log entries
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    init {
        currentFile = createLogFile()
        currentBuffer = mapFile(currentFile, bufferSize)
    }

    companion object {
        internal const val TAG = "MMapLogger"
        // Match Android's log levels
        const val VERBOSE = AndroidLog.VERBOSE
        const val DEBUG = AndroidLog.DEBUG
        const val INFO = AndroidLog.INFO
        const val WARN = AndroidLog.WARN
        const val ERROR = AndroidLog.ERROR

        internal const val DEFAULT_BUFFER_SIZE = 1 * 1024 * 1024 // 1MB
        private const val MIN_REMAINING_SPACE = 10 * 1024 //10KB
        private const val PADDING_END = 1024

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: MmapLogger? = null

        internal fun initialize(
            context: Context,
            bufferSize: Int = DEFAULT_BUFFER_SIZE
        ): MmapLogger {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MmapLogger(context, bufferSize).also { INSTANCE = it }
            }
        }

        internal fun getInstance(): MmapLogger {
            return INSTANCE ?: throw IllegalStateException("MmapLogger not initialized")
        }
    }

    /*
     * Create a new log file or reuse current. In general given 1MB max buffer size this should take couple milliseconds.
     */
    private fun createLogFile(): File {
        val logDir = File(context.filesDir, "logs").apply {
            if (!exists()) mkdirs()
        }

        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val existingLogFiles = logDir.listFiles { file ->
            file.name.startsWith("log-$dateStr-") && file.name.endsWith(".log") && !file.name.contains("trim")
        }?.sortedBy { it.name }

        if (!existingLogFiles.isNullOrEmpty()) {
            for (logFile in existingLogFiles) {
                try {
                    val enter = SystemClock.elapsedRealtime()
                    val endPosition = findEndPosition(logFile)
                    AndroidLog.d(TAG, "Find last logger pos dur ${SystemClock.elapsedRealtime() - enter}ms")

                    // If file has significant space left, reuse it
                    if (endPosition < bufferSize - MIN_REMAINING_SPACE) {
                        position.set(endPosition)
                        AndroidLog.d(TAG, "Use file $logFile at pos $endPosition")
                        return logFile
                    }
                } catch (e: Exception) {
                    // If there's an issue reading the file, move to the next one
                    continue
                }
            }
        }

        // If no suitable file found, create a new one
        var fileNumber = 1
        var logFile: File
        do {
            logFile = File(logDir, "log-$dateStr-$fileNumber.log")
            fileNumber++
        } while (logFile.exists())

        // Reset position for the new file
        position.set(0)
        AndroidLog.d(TAG, "Create file $logFile")
        return logFile
    }

    /**
     * No logs can be bigger than GB. Int is safe and provides a weak type constraint in pair/triple.
     */
    private fun findEndPosition(file: File): Int {
        RandomAccessFile(file, "r").use { randomAccessFile ->
            val fileSize = minOf(randomAccessFile.length(), bufferSize.toLong()).toInt()
            if (fileSize == 0) return 0

            val bytes = ByteArray(fileSize)
            randomAccessFile.readFully(bytes)

            for (i in fileSize - 1 downTo 0) {
                if (bytes[i] == '\n'.code.toByte()) {
                    return i + 1
                }
            }
            return 0
        }
    }

    private fun mapFile(file: File, size: Int): MappedByteBuffer {
        val channel = RandomAccessFile(file, "rw").channel
        return channel.map(FileChannel.MapMode.READ_WRITE, 0, size.toLong())
    }

    internal fun logAsync(timestamp: Long, level: Int, tag: String, message: String) {
        scope.launch {
            writeTextEntry(timestamp, level, tag, message)
        }
    }

    internal fun logSync(timestamp: Long, level: Int, tag: String, message: String) {
        writeTextEntrySync(timestamp, level, tag, message)
    }

    private fun getLevelChar(level: Int): Char {
        return when (level) {
            VERBOSE -> 'V'
            DEBUG -> 'D'
            INFO -> 'I'
            WARN -> 'W'
            ERROR -> 'E'
            else -> '?'
        }
    }

    private suspend fun writeTextEntry(timestamp: Long, level: Int, tag: String, message: String) {
        withContext(Dispatchers.IO) {
            writeTextEntrySync(timestamp, level, tag, message)
        }
    }

    private inline fun writeTextEntrySync(timestamp: Long, level: Int, tag: String, message: String) {
        synchronized(currentBuffer) {
            // Format log entry as plain text: "yyyy-MM-dd HH:mm:ss.SSS L/TAG: Message\n"
            val formattedTime = dateFormat.format(Date(timestamp))
            val levelChar = getLevelChar(level)
            val logText = "$formattedTime $levelChar/$tag: $message\n"
            val logBytes = logText.toByteArray()

            // Check if we need to rotate
            if (position.get() + logBytes.size > bufferSize) {
                rotateLogFile()
            }

            // Write to buffer
            val pos = position.get()
            currentBuffer.position(pos)
            currentBuffer.put(logBytes)

            // Update position
            position.addAndGet(logBytes.size)
        }
    }

    private fun rotateLogFile() {
        synchronized(currentBuffer) {
            // Force write current buffer
            currentBuffer.force()

            // Create new file
            currentFile = createLogFile()
            currentBuffer = mapFile(currentFile, bufferSize)
            position.set(0)
        }
    }

    internal fun trimLogs() {
        listLogFiles().filter { (f, _) -> f.name != currentFile.name && !f.name.contains("trim") }
            .forEach { (f, lastPos) ->
                if (f.exists()) {
                    if (lastPos < f.length() - MIN_REMAINING_SPACE) {
                        //If file has significant unused space, trim it so contain only useful contents
                        val bytes = ByteArray(lastPos + PADDING_END)
                        f.readBytes().copyInto(bytes, 0, 0, lastPos)
                        val newFile = File(f.parent, "${f.nameWithoutExtension}-trimmed.log")
                        newFile.writeBytes(bytes)
                        f.delete()
                        LogProxy.getDefault().log(LogProxy.INFO, TAG, "Trim file ${f.name} to $lastPos bytes")
                    }
                }
            }
    }

    fun flush() {
        synchronized(currentBuffer) {
            currentBuffer.force()
        }
    }

    fun listLogFiles(): List<Pair<File, Int>> {
        val logDir = File(context.filesDir, "logs").apply {
            if (!exists()) mkdirs()
        }
        return (logDir.listFiles()?.toList() ?: emptyList())
            .filter { it.name.startsWith("log-") && it.extension == "log"}
            .sortedBy { it.name }
            .map { file ->
                val lastPos = if (file.name == currentFile.name) {
                    // For current file, use the last position from the log system
                    position.get()
                } else {
                    // For other files, calculate actual content size
                    findEndPosition(file)
                }
                Pair(file, lastPos)
            }
    }

    // Get readable content directly from the current buffer
    fun getCurrentLogs(): String {
        synchronized(currentBuffer) {
            val currentPos = position.get()
            if (currentPos == 0) return ""

            val bytes = ByteArray(currentPos)
            val duplicate = currentBuffer.duplicate()
            duplicate.position(0)
            duplicate.get(bytes, 0, currentPos)

            return String(bytes)
        }
    }

    fun getLogsOfFile(filename: String): String {
        if (filename == currentFile.name) {
            return getCurrentLogs()
        } else {
            val logFile = File(context.filesDir, "logs/$filename")
            if (!logFile.exists()) {
                return "File not found"
            }
            val bytes = logFile.readBytes()
            return String(bytes)
        }
    }
}

interface LogProxy {
    companion object {
        // Make log levels available from the proxy
        const val VERBOSE = MmapLogger.VERBOSE
        const val DEBUG = MmapLogger.DEBUG
        const val INFO = MmapLogger.INFO
        const val WARN = MmapLogger.WARN
        const val ERROR = MmapLogger.ERROR

        fun getDefault(): LogProxy = MmapLogProxy.getInstance()

        fun getAndroid() = AndroidOnlyLogProxy
    }
    fun log(level: Int, tag: String, message: String)
}

object AndroidOnlyLogProxy: LogProxy {

    override fun log(level: Int, tag: String, message: String) {
        when (level) {
            LogProxy.VERBOSE -> AndroidLog.v(tag, message)
            LogProxy.DEBUG -> AndroidLog.d(tag, message)
            LogProxy.INFO -> AndroidLog.i(tag, message)
            LogProxy.WARN -> AndroidLog.w(tag, message)
            LogProxy.ERROR -> AndroidLog.e(tag, message)
        }
    }
}

/**
 * Proxy that handles both Android logging and memory-mapped logging
 */
class MmapLogProxy private constructor(
    context: Context,
    bufferSize: Int = DEFAULT_BUFFER_SIZE
): LogProxy {
    // Pure logger for memory-mapped operations
    private val mmapLogger = MmapLogger.initialize(context, bufferSize)

    companion object {

        @Volatile
        private var INSTANCE: MmapLogProxy? = null

        fun initialize(
            context: Context,
            bufferSize: Int = MmapLogger.DEFAULT_BUFFER_SIZE
        ): MmapLogProxy {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MmapLogProxy(context, bufferSize).also { INSTANCE = it }
            }
        }

        fun getInstance(): MmapLogProxy {
            return INSTANCE ?: throw IllegalStateException("MmapLogProxy not initialized")
        }
    }

    override fun log(level: Int, tag: String, message: String) {
        // 1. Log to Android's logcat (on original thread)
        when (level) {
            LogProxy.VERBOSE -> AndroidLog.v(tag, message)
            LogProxy.DEBUG -> AndroidLog.d(tag, message)
            LogProxy.INFO -> AndroidLog.i(tag, message)
            LogProxy.WARN -> AndroidLog.w(tag, message)
            LogProxy.ERROR -> AndroidLog.e(tag, message)
        }

        // 2. Log to memory-mapped file (will be processed in background)
        mmapLogger.logAsync(System.currentTimeMillis(), level, tag, message)
    }

    internal fun logSync(level: Int, tag: String, message: String) {
        mmapLogger.logSync(System.currentTimeMillis(), level, tag, message)
    }

    fun flush() {
        this.log(LogProxy.VERBOSE, MmapLogger.TAG, "flush requested")
        mmapLogger.flush()
    }

    fun getCurrentLogs(): String {
        return mmapLogger.getCurrentLogs()
    }

    fun listLogFiles() = mmapLogger.listLogFiles()

    fun trim() = mmapLogger.trimLogs()

    fun getLogsOfFile(filename: String): String = mmapLogger.getLogsOfFile(filename)

    // Get all logs from the current session
    fun exportLogsToFile(outputFile: File) {
        outputFile.bufferedWriter().use { writer ->
            // First write current buffer
            writer.write(mmapLogger.getCurrentLogs())

            // Then append any historical log files
            mmapLogger.listLogFiles().forEach { (file, _) ->
                if (file != outputFile && file.exists()) {
                    writer.write(file.readText())
                }
            }
        }
    }
}

class LogLazy(
    private val lifecycleOwner: LifecycleOwner?,
    private val tag: String
) {
    private var logger: Logger? = null
    private var lifecycleObserver: LifecycleEventObserver? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Logger {
        return logger ?: synchronized(this) {
            logger ?: createLogger().also { logger = it }
        }
    }

    private fun createLogger(): Logger {
        val proxy = MmapLogProxy.getInstance()
        val newLogger = Logger(tag, proxy)

        // Only register lifecycle observer if we have a lifecycleOwner
        lifecycleOwner?.let { owner ->
            lifecycleObserver = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE,
                    Lifecycle.Event.ON_STOP,
                    Lifecycle.Event.ON_DESTROY -> proxy.flush()
                    else -> {}
                }
            }

            owner.lifecycle.addObserver(lifecycleObserver!!)
        }

        return newLogger
    }
}

fun LifecycleOwner.logs(tag: String): LogLazy {
    return LogLazy(this, tag)
}

fun logs(tag: String): LogLazy {
    return LogLazy(null, tag)
}

fun androidLogs(tag: String): Lazy<Logger> = lazy {
    Logger(tag, LogProxy.getAndroid())
}

/**
 * Logger class for convenient logging
 * @param interceptor return false if swallow the log
 */
class Logger(
    private val tag: String,
    private val proxy: LogProxy = MmapLogProxy.getInstance(),
    private val interceptor: ((level: Int, message: String) -> Boolean)? = null
) {
    private var enabled = true

    private val realProxy = object: LogProxy {
        override fun log(level: Int, tag: String, message: String) {
            if (check(level, message)) proxy.log(level, tag, message)
        }
    }

    fun enable(value: Boolean) {
        enabled = value
    }

    private fun check(level: Int, content: String): Boolean {
        return enabled && interceptor?.invoke(level, content) != false
    }

    fun v(message: String) = realProxy.log(LogProxy.VERBOSE, tag, message)
    internal fun d(message: String) = realProxy.log(LogProxy.DEBUG, tag, message)
    fun i(message: String) = realProxy.log(LogProxy.INFO, tag, message)
    fun w(message: String) = realProxy.log(LogProxy.WARN, tag, message)
    fun e(message: String) = realProxy.log(LogProxy.ERROR, tag, message)
}

fun String.logLevel(): Int {
    return when {
        contains(" V/") -> LogProxy.VERBOSE
        contains(" D/") -> LogProxy.DEBUG
        contains(" I/") -> LogProxy.INFO
        contains(" W/") -> LogProxy.WARN
        contains(" E/") -> LogProxy.ERROR
        else -> -1
    }
}

private val trackLogger = Logger("TrackerLog", LogProxy.getAndroid())

fun track(name: String, payload: () -> Unit) {
//    val callPlace = Exception().stackTrace.firstOrNull {
//        !it.className.contains("LocalLogsKt") && !it.methodName.contains("track")
//    }
//    val callerStr = callPlace?.toString() ?: "Unknown"
    val start = SystemClock.elapsedRealtime()
    payload.invoke()
    val time = SystemClock.elapsedRealtime() - start
    trackLogger.d("$name: Duration $time ms.")
}

fun Throwable.deepStackTraceToString(): String {
    val sb = StringBuilder()
    var current: Throwable? = this
    var counter = 0

    while (current != null) {
        if (counter > 0) {
            sb.append("\nCaused by: ")
        }

        sb.append(current.toString())

        // Append stack trace elements
        for (element in current.stackTrace) {
            sb.append("\n    at ")
            sb.append(element.toString())
        }

        // Move to the cause
        current = current.cause

        // Avoid infinite loops if there's a circular reference
        if (current === this) {
            sb.append("\nCaused by: [CIRCULAR REFERENCE]")
            break
        }

        counter++
    }

    return sb.toString()
}