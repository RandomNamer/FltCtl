package com.example.fltctl.controls.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.graphics.Path
import android.graphics.PointF
import com.example.fltctl.configs.verticalTurnPageWhitelist
import com.example.fltctl.controls.service.appaware.AppTriggerManager

class FloatingControlAccessibilityService: AccessibilityService(), RequireAccessibilityServiceActions {
    companion object {
        private const val TAG = "AccSvc"
    }

    private var cachedRootNode: AccessibilityNodeInfo? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
//        Log.i(TAG, event.toString())
        if (event != null) {
            AppTriggerManager.onAccessibilityEvent(event)
        }
//        cachedRootNode = event?.source?.window?.root
    }

    override fun onInterrupt() {
        Log.i(TAG, "interrupted")
    }

    override fun onServiceConnected() {
        Log.i(TAG, "onConnect")
        super.onServiceConnected()
        ActionPerformer.onServiceConnect(this)
//        FloatingControlManager.tryShowWindowWithCurrentControl()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "onUnbind")
        ActionPerformer.onServiceDisconnect()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        ActionPerformer.onServiceDisconnect()
        super.onDestroy()
    }



    private fun turnPageHorizontally(forward: Boolean) {
        val result = findPageBy(rootInActiveWindow)?.run {
            if (forward) nextAction.invoke(node) else prevAction.invoke(node)
        } ?: false
        //TODO: add setting for this behavior when auto-turn fails
        if (!result) tryTurnPageByClickNodeEdge(rootInActiveWindow, forward)
    }

    private fun turnPageVertically(forward: Boolean) {
        if (rootInActiveWindow?.packageName in verticalTurnPageWhitelist) tryTurnPageVertically(rootInActiveWindow, forward)
    }

    private fun tryTurnPageByClickNodeEdge(node: AccessibilityNodeInfo?, forward: Boolean) {

        val outRect = Rect(0, 0, resources.displayMetrics.widthPixels, resources.displayMetrics.widthPixels)
        node?.getBoundsInScreen(outRect)
        val touchPoint = PointF(if (forward) outRect.right - CLICK_PADDING else outRect.left + CLICK_PADDING, outRect.exactCenterY())
        val clickPath = Path().apply {
            moveTo(touchPoint.x, touchPoint.y)
        }
        val gb = GestureDescription.Builder().apply {
            addStroke(GestureDescription.StrokeDescription(clickPath, 0, 10))
        }
        Log.i(TAG, "Try touch to turn page by $touchPoint")
        dispatchGesture(gb.build(), null, null)
    }

    /**
     * TikTok turning pages
     */
    private fun tryTurnPageVertically(node: AccessibilityNodeInfo, forward: Boolean) {
        val outRect = Rect()
        node.getBoundsInScreen(outRect)
        val scrollPath = Path().apply {
            moveTo(outRect.exactCenterX(), outRect.top * 1/4f + outRect.bottom * 3/4f)
            lineTo(outRect.exactCenterX(), outRect.top * 3/4f + outRect.bottom * 1/4f)
        }
        dispatchGesture(GestureDescription.Builder().apply {
            addStroke(GestureDescription.StrokeDescription(scrollPath, 100, 200))
        }.build(), null, null)
    }

    override fun pressHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    override fun pressBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    override fun tryTurnPage(forward: Boolean) {
        turnPageHorizontally(forward)
    }

    override fun tryTurnPageVertically(forward: Boolean) {
        turnPageVertically(forward)
    }

    override fun queryFocusedApp(): String? = rootInActiveWindow?.packageName?.toString()
}