package com.example.fltctl.controls

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.example.fltctl.R
import com.example.fltctl.controls.arch.FloatingControl
import com.example.fltctl.controls.service.ActionPerformer
import com.example.fltctl.widgets.view.*

class DefaultTurnPageControl: FloatingControl() {

    companion object {
        private val BUTTON_PADDING = 10.takeDp()
    }

    private lateinit var up: EInkCompatRoundedButton
    private lateinit var down: EInkCompatRoundedButton
    private var backgroundAlpha = 0.6f
    private var buttonSize = 40.takeDp()
        set(value) {
            beforeButtonSizeChange()
            field = value
        }
    private val containerCornerRadius: Float
        get() = buttonSize / 2f + BUTTON_PADDING

    private lateinit var containerBackgroundNormal: Drawable
    private lateinit var containerBackgroundEInk: Drawable

    override fun onCreateView(context: Context): View {
        recreateBackgrounds()
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
            }.also { up = it })
            addView(EInkCompatIconButton(context).apply {
                layoutParams = LinearLayout.LayoutParams(buttonSize, buttonSize)
                margin(start = BUTTON_PADDING, top = 0, end = BUTTON_PADDING, bottom = BUTTON_PADDING)
                elevation = 6.takeDpAsFloat()
                setIcon(resources.getDrawable(R.drawable.baseline_remove_24))
                setIconTint(Color.WHITE)
                setOnClickListener {
                    ActionPerformer.tryTurnPage(false)
                }
            }.also { down = it })
        }
    }

    override fun onViewCreated(view: View) {
        fitViewWithEInkMode(isInEInkMode)
    }

    override fun onEInkConfigChanged(isEInk: Boolean) {
        super.onEInkConfigChanged(isEInk)
        fitViewWithEInkMode(isEInk)
    }
    private fun beforeButtonSizeChange() {
        recreateBackgrounds()
    }

    private fun recreateBackgrounds() {
        containerBackgroundNormal = createRoundedCornerColorDrawable(Color.WHITE, containerCornerRadius)
        containerBackgroundEInk = createRoundedCornerEInkDrawable(containerCornerRadius)
    }

    private fun fitViewWithEInkMode(isEInk: Boolean) {
        rootView?.background = if (isInEInkMode) containerBackgroundEInk else containerBackgroundNormal
        rootView?.background?.alpha = if (isEInk) 255 else (backgroundAlpha * 255).toInt()
    }

}