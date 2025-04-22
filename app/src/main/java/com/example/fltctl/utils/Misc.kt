package com.example.fltctl.utils

import android.text.format.DateFormat
import com.example.fltctl.AppMonitor
import java.util.Date
import kotlin.random.Random

/**
 * Created by zeyu.zyzhang on 4/10/25
 * @author zeyu.zyzhang@bytedance.com
 */

val globalRandom = Random(System.currentTimeMillis())

inline fun chance(@androidx.annotation.IntRange(-1, 100) pct: Int, crossinline payload: () -> Unit) {
    if (globalRandom.nextInt(100) < pct) payload.invoke()
}

fun Long.asLocaleTimeString(): String {
    return DateFormat.getTimeFormat(AppMonitor.appContext).format(Date(this))
}