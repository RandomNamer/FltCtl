package com.example.fltctl.ui

import androidx.annotation.FloatRange


fun IntRange.takeProportion(@FloatRange(0.0, 1.0) proportion: Float): Float {
    return (last - start) * proportion + start
}