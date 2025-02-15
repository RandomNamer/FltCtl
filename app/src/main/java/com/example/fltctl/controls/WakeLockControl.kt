package com.example.fltctl.controls

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.FrameLayout
import com.example.fltctl.R
import com.example.fltctl.controls.arch.FloatingControl
import com.example.fltctl.controls.service.ActionPerformer
import com.example.fltctl.utils.registerScreenInteractiveStateListener
import com.example.fltctl.widgets.view.EInkCompatIconButton
import com.example.fltctl.widgets.view.margin
import com.example.fltctl.widgets.view.takeDp
import com.example.fltctl.widgets.view.takeDpAsFloat

/**
 * Created by zeyu.zyzhang on 2/5/25
 * @author zeyu.zyzhang@bytedance.com
 */
class WakeLockControl: FloatingControl() {
    private var buttonSize = 50.takeDp()
    private lateinit var lockedIcon: Drawable
    private lateinit var unlockedIcon: Drawable
    private var wakeLockOn = false
    private lateinit var button: EInkCompatIconButton

    init {
        registerScreenInteractiveStateListener(lifecycle) { active ->
            if (wakeLockOn) {
                if (active) ActionPerformer.tryAcquireWakeLockIndefinitely()
                else ActionPerformer.releaseWakeLock()
            }
        }
    }


    override fun onCreateView(context: Context): View {
        return EInkCompatIconButton(context).apply {
            layoutParams = FrameLayout.LayoutParams(buttonSize, buttonSize)
            margin(8.takeDp())
            elevation = 6.takeDpAsFloat()
            lockedIcon = resources.getDrawable(R.drawable.baseline_lock_24)
            unlockedIcon = resources.getDrawable(R.drawable.baseline_lock_open_24)
            setIcon(unlockedIcon)
            setIconTint(Color.WHITE)
            setOnClickListener {
                onClick()
            }
        }.also { button = it }
    }

    override fun onStop() {
        super.onStop()
        ActionPerformer.releaseWakeLock()
    }

    private fun onClick() {
        wakeLockOn =!wakeLockOn
        if (wakeLockOn) {
            button.setIcon(lockedIcon)
            ActionPerformer.tryAcquireWakeLockIndefinitely()
        } else {
            button.setIcon(unlockedIcon)
            ActionPerformer.releaseWakeLock()
        }
    }
}