package com.example.fltctl.controls.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import android.graphics.Path
import android.graphics.PointF
import com.example.fltctl.controls.arch.FloatingControlManager
import java.util.*
import kotlin.collections.ArrayDeque

class FloatingControlAccessibilityService: AccessibilityService() {
    companion object {
        private const val TAG = "ButtonSvc"
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        Log.i(TAG, event.toString())
    }

    override fun onInterrupt() {
        Log.i(TAG, "interrupted")
    }

    override fun onServiceConnected() {
        Log.i(TAG, "onConnect")
        super.onServiceConnected()
        FloatingControlManager.connectToService(this)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "onUnbind")
        FloatingControlManager.onServiceOffline()
        return super.onUnbind(intent)
    }

    fun turnPage(forward: Boolean) {
        val result = findPageBy(rootInActiveWindow)?.run {
            if (forward) nextAction.invoke(node) else prevAction.invoke(node)
        } ?: false
        if (!result && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) tryTurnPageByClickNodeEdge(rootInActiveWindow, forward)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun tryTurnPageByClickNodeEdge(node: AccessibilityNodeInfo, forward: Boolean) {
        val outRect = Rect()
        node.getBoundsInScreen(outRect)
        val touchPoint = PointF(if (forward) outRect.right - CLICK_PADDING else outRect.left + CLICK_PADDING, outRect.exactCenterY())
        val clickPath = Path().apply {
            moveTo(touchPoint.x, touchPoint.y)
        }
        val gb = GestureDescription.Builder().apply {
            addStroke(GestureDescription.StrokeDescription(clickPath, 0, 10))
        }
        Log.i(TAG, "Final try with $touchPoint")
        dispatchGesture(gb.build(), null, null)
        node.performAction(AccessibilityNodeInfo.ACTION_CLICK, )
    }

}