package com.example.fltctl.ui

import android.content.res.Resources
import android.icu.text.DateFormat
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