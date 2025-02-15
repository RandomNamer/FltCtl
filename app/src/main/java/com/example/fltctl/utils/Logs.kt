package com.example.fltctl.utils

import android.util.Log

/**
 * Created by zeyu.zyzhang on 2/15/25
 * @author zeyu.zyzhang@bytedance.com
 */

internal const val STACKTRACE_ENABLE = false

fun logWithStacktrace(msg: String,  depth: Int = 5, tag:String? = null,) {
    if (!STACKTRACE_ENABLE) return
    val stackTrace = Thread.currentThread().stackTrace
    val sb = StringBuilder()
    sb.append(msg)
    for (i in 3..depth+2) {
        sb.append("\n")
        sb.append(stackTrace[i].toString())
    }
    Log.i(tag ?: stackTrace[3].className, sb.toString())
}