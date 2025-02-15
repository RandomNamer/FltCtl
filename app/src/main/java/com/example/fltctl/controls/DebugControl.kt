package com.example.fltctl.controls

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import com.example.fltctl.controls.arch.FloatingControl
import com.example.fltctl.widgets.view.takeDp

class DebugControl: FloatingControl() {
    override fun onCreateView(context: Context): View {
        return View(context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(100.takeDp(), 100.takeDp())
            background = ColorDrawable(Color.RED)
        }
    }
}