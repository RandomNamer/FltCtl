package com.example.fltctl.widgets.window

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.UiThread
import androidx.core.math.MathUtils
import com.example.fltctl.widgets.view.navBarHeight
import com.example.fltctl.widgets.view.screenHeight
import com.example.fltctl.widgets.view.screenWidth
import com.example.fltctl.widgets.view.statusBarHeight
import com.example.fltctl.widgets.view.takeDp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * TODO: Add support for top & bottom edge adhering
 */
open class FloatingWindow(private val context: Context) {

    companion object {
        private const val _DEBUG = true
        private const val TAG = "FloatingWindow"
        private val EDGE_PADDING = 10.takeDp()
    }

    inner class DecorView(context: Context): FrameLayout(context) {
        private var downTouchX = 0f
        private var downTouchY = 0f
        private var touchDownId = 0
        private var lastX = -1F
        private var lastY = -1F

        private val touchSlop by lazy {
            ViewConfiguration.get(context).scaledTouchSlop
        }

        override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
            var intercepted = false
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    touchDownId = ev.getPointerId(ev.actionIndex)
                    downTouchX = ev.getX(ev.actionIndex)
                    downTouchY = ev.getY(ev.actionIndex)
                }

                MotionEvent.ACTION_MOVE -> {
                    intercepted = abs(downTouchX - ev.x) >= touchSlop
                }
            }
            return intercepted
        }

        override fun onTouchEvent(ev: MotionEvent): Boolean {
            when (ev.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    val pointerId = ev.findPointerIndex(touchDownId)
                    if (pointerId >= 0) {
                        val offsetX = if (lastX < 0) 0 else (ev.rawX - lastX).toInt()
                        val offsetY = if (lastY < 0) 0 else (ev.rawY - lastY).toInt()
                        moveByOffset(offsetX, offsetY)
                        lastX = ev.rawX
                        lastY = ev.rawY
                    }
                }
                MotionEvent.ACTION_UP -> {
                    lastX = -1f
                    lastY = -1f
                    adhereToScreenEdge()
                }
            }
            return super.onTouchEvent(ev)
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            onDecorViewSizeChanged(w, h)
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            onDecorViewSizeChanged(measuredWidth, measuredHeight)
        }
    }

    enum class WindowAdhereState {
        NONE,
        LEFT,
        RIGHT,
        TOP,
        BOTTOM
    }

    data class RememberableWindowState(
        val absolutePosition: Point = Point(0, 0),
        val relativePosition: Pair<Float, Float> = 0f to 0f,
        val adhereState: WindowAdhereState = WindowAdhereState.NONE
    )

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var decorView: ViewGroup? = null

    private val windowPosition = Point()

    private var windowAdhereState = WindowAdhereState.NONE

    private var screenWidth = context.screenWidth

    private var screenHeight = context.screenHeight

    private var statusBarHeight = context.statusBarHeight

    private var navBarHeight = context.navBarHeight

    private var relativeWindowPositionY: Float = 0f

    private var relativeWindowPositionX: Float = 0f

    private val viewBound = Rect()

    private val draggableBound = Rect()

    private var currentWindowHeight: Int = 0

    private var currentWindowWidth: Int = 0

    private val defaultPositionX = screenWidth - EDGE_PADDING

    private val defaultPositionY = statusBarHeight + 40.takeDp()

    var isShowing = false
        private set

    fun show(with: View?, toPosition: Point? = null): Boolean {
        if (windowManager.defaultDisplay.state != Display.STATE_ON) {
            Log.e(TAG, "Cannot show window when screen is off")
            return false
        }
        if (isShowing) return false
        (decorView ?: DecorView(context).also { decorView = it }).run {
            removeAllViews()
            addView(with ?: createDefaultView())
        }
        val lp = WindowManager.LayoutParams()
        setInitialParams(lp)
        lp.x = toPosition?.x ?: defaultPositionX
        lp.y = toPosition?.y ?: defaultPositionY
        windowPosition.set(lp.x, lp.y)
        windowManager.addView(decorView, lp)
        relativeWindowPositionY = calculateCurrentWindowPositionRatioY()
        relativeWindowPositionX = calculateCurrentWindowPositionRatioX()
        ConfigurationHelper.addOnChangeListener {
            decorView?.postDelayed({
                updateScreenParams {
                    restoreWindowPosition()
                }
            }, 100L)
        }
        isShowing = true
        Log.d(TAG, "Window $this shown")
        return true
    }

    fun hide(stateSaver: ((RememberableWindowState) -> Unit)? = null) {
        if (decorView == null) return
        stateSaver?.invoke(
            RememberableWindowState(
                absolutePosition = Point(windowPosition),
                relativePosition = relativeWindowPositionX to relativeWindowPositionY,
                adhereState = windowAdhereState
            )
        )
        windowManager.removeViewImmediate(decorView)
        decorView?.removeAllViews()
        decorView = null
        isShowing = false
        ConfigurationHelper.removeAllOnChangeListeners()
        Log.d(TAG, "Window $this hidden")
    }

    @UiThread
    private fun updateScreenParams(onCompleted: (() -> Unit)? = null) {
        decorView?.run {
            resolveScreenDimen(resources.configuration)
            navBarHeight = context.navBarHeight
            statusBarHeight = context.statusBarHeight
        }
        Log.i(TAG, "Screen config changed. Current size: ($screenHeight, $screenWidth) insetHeights: ($statusBarHeight, $navBarHeight) rotation: ${windowManager.defaultDisplay.rotation}, relativeY: $relativeWindowPositionY")
        onCompleted?.invoke()
    }

    private fun resolveScreenDimen(config: Configuration) {
        //assert when rotation 0, H > W
        val rawH = config.screenHeightDp
        val rawW = config.screenWidthDp
        val h = max(rawW, rawH).takeDp()
        val w = min(rawW, rawH).takeDp()
        when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 -> {
                screenHeight = h
                screenWidth = w
            }
            Surface.ROTATION_90, Surface.ROTATION_270 -> {
                screenWidth = h
                screenHeight = w
            }
        }
    }

    private fun setInitialParams(lp: WindowManager.LayoutParams) {
        lp.apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }
            format = PixelFormat.RGBA_8888
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                fitInsetsTypes = WindowInsets.Type.systemBars()
            }
            gravity = Gravity.LEFT or Gravity.TOP
            flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            width = ViewGroup.LayoutParams.WRAP_CONTENT
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
    }

    /**
     * Debug purpose only
     */
    protected open fun createDefaultView(): View? {
        return View(context).apply {
            layoutParams = FrameLayout.LayoutParams(20.takeDp(), 20.takeDp())
            background = if (_DEBUG) ColorDrawable(Color.MAGENTA) else null
            isClickable = true
            isLongClickable = true
            setOnClickListener {
                background = if (_DEBUG) ColorDrawable(Color.YELLOW) else null
            }
            setOnLongClickListener {
                layoutParams.height = 40.takeDp()
                layoutParams.width = 40.takeDp()
                true
            }
        }
    }


    private fun restoreWindowPosition() {
        updateBounds(currentWindowHeight, currentWindowWidth)
        val lp = decorView?.layoutParams as? WindowManager.LayoutParams ?: return
        var newY = recoverYPositionValue(relativeWindowPositionY)
        newY = MathUtils.clamp(newY, viewBound.top, viewBound.bottom)
        var newX = recoverXPositionValue(relativeWindowPositionX)
        newX = MathUtils.clamp(newX, viewBound.left, viewBound.right)
        lp.x = newX
        lp.y = newY
        updateLp(lp)
    }

    private fun onDecorViewSizeChanged(w: Int, h: Int) {
        updateBounds(w, h)
        currentWindowHeight = w
        currentWindowWidth = h
    }

    private fun updateBounds(viewWidth: Int, viewHeight: Int) {
        viewBound.set(
            EDGE_PADDING,
            0,
            screenWidth - viewWidth - EDGE_PADDING,
            screenHeight - viewHeight - EDGE_PADDING
        )
        draggableBound.set(
            viewBound.left - viewWidth,
            viewBound.top,
            viewBound.right + viewWidth,
            viewBound.bottom
        )
    }

    private fun moveByOffset(offsetX: Int, offsetY: Int) {
        val nextPosition = Point(windowPosition).apply{ offset(offsetX, offsetY) }
        setWindowPosition(nextPosition)
    }

    private fun setWindowPosition(pos: Point) {
        decorView?.run {
            constraintPositionByBound(pos, draggableBound)
            if (pos == windowPosition) return
            val lp = layoutParams as WindowManager.LayoutParams
            lp.x = pos.x
            lp.y = pos.y
            updateLp(lp)
        }
    }

    private fun updateLp(lp: WindowManager.LayoutParams) {
        decorView ?: return
        windowManager.updateViewLayout(decorView, lp)
        windowPosition.set(lp.x, lp.y)
        relativeWindowPositionX = calculateCurrentWindowPositionRatioX()
        relativeWindowPositionY = calculateCurrentWindowPositionRatioY()
    }

    private fun calculateCurrentWindowPositionRatioY(): Float {
        val remainingScreenHeight = screenHeight - statusBarHeight - navBarHeight
        return (windowPosition.y - statusBarHeight) / remainingScreenHeight.toFloat()
    }

    private fun calculateCurrentWindowPositionRatioX(): Float {
        val remainingScreenWidth = screenWidth - 2 * EDGE_PADDING
        return (windowPosition.x - EDGE_PADDING) / remainingScreenWidth.toFloat()
    }

    fun recoverYPositionValue(ratio: Float): Int {
        val remainingScreenHeight = screenHeight - statusBarHeight - navBarHeight
        return (ratio * remainingScreenHeight).toInt() + statusBarHeight
    }

    fun recoverXPositionValue(ratio: Float): Int {
        val remainingScreenWidth = screenWidth - 2 * EDGE_PADDING
        return (ratio * remainingScreenWidth).toInt() + EDGE_PADDING
    }

    private fun constraintPositionByBound(point: Point, bound: Rect) {
        point.set(
            MathUtils.clamp(point.x, bound.left, bound.right),
            MathUtils.clamp(point.y, bound.top, bound.bottom)
        )
    }

    private fun adhereToScreenEdge(withAnimation: Boolean = false) {

    }
}