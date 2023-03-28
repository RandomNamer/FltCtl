package com.example.fltctl.controls

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.example.fltctl.R
import com.example.fltctl.controls.arch.FloatingControl
import com.example.fltctl.controls.service.ActionPerformer
import com.example.fltctl.widgets.view.*

class DefaultVolumeKeyControl: FloatingControl() {

    companion object {
        private val BUTTON_PADDING = 10.takeDp()
    }

    private lateinit var volumeUp: EInkCompatRoundedButton
    private lateinit var volumeDown: EInkCompatRoundedButton
    private var backgroundAlpha = 0.6f
    private var buttonSize = 40.takeDp()
    private val containerCornerRadius: Float
        get() = buttonSize / 2f + BUTTON_PADDING
    override fun onCreateView(context: Context): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            addView(EInkCompatIconButton(context).apply {
                layoutParams = LinearLayout.LayoutParams(buttonSize, buttonSize)
                margin(BUTTON_PADDING)
                elevation = 6.takeDpAsFloat()
                setIcon(resources.getDrawable(R.drawable.baseline_add_24))
                setIconTint(Color.WHITE)
                setOnClickListener {
                    ActionPerformer.tryTurnPage(true)
                }
            }.also { volumeUp = it })
            addView(EInkCompatIconButton(context).apply {
                layoutParams = LinearLayout.LayoutParams(buttonSize, buttonSize)
                margin(start = BUTTON_PADDING, top = 0, end = BUTTON_PADDING, bottom = BUTTON_PADDING)
                elevation = 6.takeDpAsFloat()
                setIcon(resources.getDrawable(R.drawable.baseline_remove_24))
                setIconTint(Color.WHITE)
                setOnClickListener {
                    ActionPerformer.tryTurnPage(false)
                }
            }.also { volumeDown = it })
        }
    }

    override fun onViewCreated(view: View) {
        fitViewWithEInkMode(isInEInkMode)
    }

    override fun onEInkConfigChanged(isEInk: Boolean) {
        fitViewWithEInkMode(isEInk)
    }

    private fun fitViewWithEInkMode(isEInk: Boolean) {
        volumeUp.isInEInkMode = isEInk
        volumeDown.isInEInkMode = isEInk
        rootView?.background = if (!isInEInkMode) createRoundedCornerColorDrawable(Color.WHITE, containerCornerRadius) else createRoundedCornerEInkDrawable(containerCornerRadius)
        rootView?.background?.alpha = if (isEInk) 255 else (backgroundAlpha * 255).toInt()
    }



}