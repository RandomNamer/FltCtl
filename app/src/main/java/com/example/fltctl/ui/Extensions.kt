package com.example.fltctl.ui

import android.content.Context
import android.content.res.Resources
import android.icu.text.DateFormat
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.FloatRange
import java.util.Date


fun IntRange.takeProportion(@FloatRange(0.0, 1.0) proportion: Float): Float {
    return (last - start) * proportion + start
}

fun <T> List<T>.takeNext(value: T): T {
    if (isEmpty()) throw IndexOutOfBoundsException("size is zero")
    return get((indexOf(value) + 1).takeIf { it < size } ?: 0)
}

val currentLocale by lazy { Resources.getSystem().configuration.locales[0] }
fun Long.toLocaleDateString(): String = DateFormat.getDateInstance(DateFormat.SHORT, currentLocale).format(Date(this))


fun Context.toast(msg: String, longDuration: Boolean = false) {
    val r = Runnable {
        Toast.makeText(this@toast, msg, if (longDuration) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }
    val mainLooper = Looper.getMainLooper()
    if (mainLooper.isCurrentThread) r.run() else Handler(mainLooper).post(r)
}