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
            file.name.startsWith("log-$dateStr-") && file.name.endsWith(".log")
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

    internal fun log(timestamp: Long, level: Int, tag: String, message: String) {
        scope.launch {
            writeTextEntry(timestamp, level, tag, message)
        }
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

    fun flush() {
        synchronized(currentBuffer) {
            currentBuffer.force()
        }
    }

    fun getLogFiles(): List<File> {
        val logDir = File(context.filesDir, "logs")
        return logDir.listFiles()?.filter { it.name.startsWith("log-") }?.sortedBy { it.name } ?: emptyList()
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
}

/**
 * Proxy that handles both Android logging and memory-mapped logging
 */
class MmapLogProxy private constructor(
    context: Context,
    bufferSize: Int = DEFAULT_BUFFER_SIZE
) {
    // Pure logger for memory-mapped operations
    private val mmapLogger = MmapLogger.initialize(context, bufferSize)

    companion object {
        // Make log levels available from the proxy
        const val VERBOSE = MmapLogger.VERBOSE
        const val DEBUG = MmapLogger.DEBUG
        const val INFO = MmapLogger.INFO
        const val WARN = MmapLogger.WARN
        const val ERROR = MmapLogger.ERROR

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

    fun log(level: Int, tag: String, message: String) {
        // 1. Log to Android's logcat (on original thread)
        when (level) {
            VERBOSE -> AndroidLog.v(tag, message)
            DEBUG -> AndroidLog.d(tag, message)
            INFO -> AndroidLog.i(tag, message)
            WARN -> AndroidLog.w(tag, message)
            ERROR -> AndroidLog.e(tag, message)
        }

        // 2. Log to memory-mapped file (will be processed in background)
        mmapLogger.log(System.currentTimeMillis(), level, tag, message)
    }

    fun flush() {
        this.log(DEBUG, MmapLogger.TAG, "flush requested")
        mmapLogger.flush()
    }

    fun getCurrentLogs(): String {
        return mmapLogger.getCurrentLogs()
    }

    fun getLogFiles(): List<File> {
        return mmapLogger.getLogFiles()
    }

    // Get all logs from the current session
    fun exportLogsToFile(outputFile: File) {
        outputFile.bufferedWriter().use { writer ->
            // First write current buffer
            writer.write(mmapLogger.getCurrentLogs())

            // Then append any historical log files
            mmapLogger.getLogFiles().forEach { file ->
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

/**
 * Logger class for convenient logging
 */
class Logger(
    private val tag: String,
    private val proxy: MmapLogProxy = MmapLogProxy.getInstance()
) {
    fun v(message: String) = proxy.log(MmapLogProxy.VERBOSE, tag, message)
    fun d(message: String) = proxy.log(MmapLogProxy.DEBUG, tag, message)
    fun i(message: String) = proxy.log(MmapLogProxy.INFO, tag, message)
    fun w(message: String) = proxy.log(MmapLogProxy.WARN, tag, message)
    fun e(message: String) = proxy.log(MmapLogProxy.ERROR, tag, message)
}

fun String.logLevel(): Int {
    return when {
        contains(" V/") -> MmapLogProxy.VERBOSE
        contains(" D/") -> MmapLogProxy.DEBUG
        contains(" I/") -> MmapLogProxy.INFO
        contains(" W/") -> MmapLogProxy.WARN
        contains(" E/") -> MmapLogProxy.ERROR
        else -> -1
    }
}