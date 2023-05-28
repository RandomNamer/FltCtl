package com.example.fltctl.controls

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.FrameLayout
import com.example.fltctl.R
import com.example.fltctl.controls.arch.FloatingControl
import com.example.fltctl.controls.service.ActionPerformer
import com.example.fltctl.widgets.view.EInkCompatIconButton
import com.example.fltctl.widgets.view.margin
import com.example.fltctl.widgets.view.takeDp
import com.example.fltctl.widgets.view.takeDpAsFloat

class VerticalTurnPageControl: FloatingControl() {

    companion object {
        private const val AUTO_TURN_PAGE_INTERVAL = 3000L
    }

    private var buttonSize = 60.takeDp()
    private lateinit var playIcon: Drawable
    private lateinit var pauseIcon: Drawable
    private lateinit var button: EInkCompatIconButton
    private var isAutoTurning = false
    private val autoTurnPageRunner = object: Runnable {
        override fun run() {
            ActionPerformer.tryTurnPageVertically(true)
            handler?.postDelayed(this, AUTO_TURN_PAGE_INTERVAL)
        }
    }

    override fun onCreateView(context: Context): View {
        return EInkCompatIconButton(context).apply {
            layoutParams = FrameLayout.LayoutParams(buttonSize, buttonSize)
            margin(10.takeDp())
            elevation = 6.takeDpAsFloat()
            playIcon = resources.getDrawable(R.drawable.baseline_play_arrow_24)
            pauseIcon = resources.getDrawable(R.drawable.baseline_pause_24)
            setIcon(playIcon)
            setIconTint(Color.WHITE)
            setOnClickListener {
                onClick()
            }
        }.also { button = it }
    }

    private fun onClick() {
        if (isAutoTurning) {
            handler?.removeCallbacks(autoTurnPageRunner)
            button.setIcon(playIcon)
            isAutoTurning = false
        } else {
            handler?.postDelayed(autoTurnPageRunner, 500L)
            button.setIcon(pauseIcon)
            isAutoTurning = true
        }
    }
}