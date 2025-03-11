package com.example.fltctl.tests.controls

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.fltctl.controls.arch.FloatingControlInfo
import com.example.fltctl.tests.UiTest
import com.example.fltctl.utils.MmapLogProxy
import com.example.fltctl.utils.logLevel
import com.example.fltctl.widgets.view.margin
import com.example.fltctl.widgets.view.takeDp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by zeyu.zyzhang on 3/11/25
 * @author zeyu.zyzhang@bytedance.com
 * TOD
 */
class LogViewer: UiTest.FltCtlUiTest() {
    companion object {
        private const val DEFAULT_MAX_LINES = 300
        private const val DEFAULT_REFRESH_INTERVAL = 2000L
    }

    override fun onCreateView(context: Context): View {
        return RecyclerView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            )
            margin(start = 10.takeDp(), end = 10.takeDp())
            setupAsSimpleLogViewer(this@LogViewer, DEFAULT_MAX_LINES, DEFAULT_REFRESH_INTERVAL)
        }
    }

    override val info: FloatingControlInfo
        get() = FloatingControlInfo(
            displayName = "Log Viewer",
            klass = LogViewer::class
        )
}


/**
 * Simple log adapter that displays log lines in a RecyclerView
 * By claude
 */
class SimpleLogAdapter (private val level: Int, private val onNewLine: () -> Unit) :
    RecyclerView.Adapter<SimpleLogAdapter.LogViewHolder>() {
    private val logLines = mutableListOf<Pair<Int, String>>()
    private var refreshJob: Job? = null

    // Update logs with new content, with a limit on the number of lines
    fun fetchLogs(maxLines: Int = 1000, reversed: Boolean = true): List<Pair<Int, String>> {
        val logs = MmapLogProxy.getInstance().getCurrentLogs()
        val lines = if (reversed) {
            // Count backwards from the end to get only the latest maxLines
            logs.split('\n')
                .filter { it.isNotEmpty() }
                .takeLast(maxLines)
                .mapNotNull { str ->
                    str.logLevel().takeIf { it >= level }?.let { it to str }
                }
//                .reversed()
        } else {
            // Get the first maxLines
            logs.split('\n')
                .filter { it.isNotEmpty() }
                .take(maxLines)
                .mapNotNull {  str ->
                    str.logLevel().takeIf { it >= level }?.let { it to str }
                }
        }

       return lines
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(logLines[position])
    }

    override fun getItemCount() = logLines.size

    // Stop the refresh timer
    fun stopRefresh() {
        refreshJob?.cancel()
        refreshJob = null
    }

    // Start auto-refresh with specified interval and max lines
    fun startRefresh(lifecycleOwner: LifecycleOwner, maxLines: Int = 1000, intervalMs: Long = 1000) {
        stopRefresh()

        refreshJob = lifecycleOwner.lifecycleScope.launch {
            while (true) {
                withContext(Dispatchers.IO) {
                   val newLines = fetchLogs(maxLines, true)
                    // notify. While context switching other jobs cannot be fired.
                    withContext(Dispatchers.Main) {
                        if (newLines.lastOrNull() != logLines.lastOrNull()) onNewLine()
                        logLines.clear()
                        logLines.addAll(newLines)
                        notifyDataSetChanged()
                    }
                }
                delay(intervalMs)
            }
        }
    }

    class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(android.R.id.text1)

        fun bind(logLine: Pair<Int, String>) {
            textView.text = logLine.second

            // Simple color coding based on log level
            val color = when (logLine.first) {
                MmapLogProxy.VERBOSE -> Color.LTGRAY
                MmapLogProxy.DEBUG -> Color.GRAY
                MmapLogProxy.INFO -> Color.BLACK
                MmapLogProxy.WARN -> "#FFA500".toColorInt() // Orange
                MmapLogProxy.ERROR -> Color.RED
                else -> Color.BLACK
            }

            textView.setTextColor(color)
        }
    }
}

/**
 * Extension function to easily set up log viewing in any activity
 */
fun RecyclerView.setupAsSimpleLogViewer(
    lifecycleOwner: LifecycleOwner,
    maxLines: Int = 1000,
    refreshIntervalMs: Long = 1000,
    level: Int = MmapLogProxy.DEBUG,
) {
    // Create and set adapter
    val adapter = SimpleLogAdapter(level) {
        postDelayed({
            scrollToPosition((adapter?.itemCount ?: 1) - 1 )
        }, 100L)
    }
    this.adapter = adapter

    // Set layout manager (reverse layout for newest first)
    this.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context).apply {
        reverseLayout = true
        stackFromEnd = true
    }

    // Initial load with maxLines
    adapter.fetchLogs(maxLines)

    // Start refresh timer with maxLines
    adapter.startRefresh(lifecycleOwner, maxLines, refreshIntervalMs)

    // Stop refresh when recycler view is detached
    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {}
        override fun onViewDetachedFromWindow(v: View) {
            adapter.stopRefresh()
        }
    })
}