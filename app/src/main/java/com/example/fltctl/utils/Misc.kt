package com.example.fltctl.utils

import kotlin.random.Random

/**
 * Created by zeyu.zyzhang on 4/10/25
 * @author zeyu.zyzhang@bytedance.com
 */

val globalRandom = Random(System.currentTimeMillis())

inline fun chance(@androidx.annotation.IntRange(-1, 100) pct: Int, crossinline payload: () -> Unit) {
    if (globalRandom.nextInt(100) < pct) payload.invoke()
}