package com.example.fltctl.widgets.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.annotation.ColorInt
import kotlin.math.absoluteValue
import kotlin.math.min

open class EInkCompatRoundedButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.borderlessButtonStyle
): FrameLayout(context, attrs, defStyleAttr), EInkAware {

    private var elevationInNormalMode = elevation

    /**
     * Initial set value must be called after any setElevation call
     */
    var isInEInkMode: Boolean
        get() = _isInEInkMode
        set(value) {
            setEInkState(value)
        }
    private var _isInEInkMode = false

    override fun setEInkState(state: Boolean) {
        _isInEInkMode = state
        onEInkStateChanged(state)
        postInvalidate()
    }

    @CallSuper
    protected open fun onEInkStateChanged(value: Boolean) {
        if (value) {
            elevationInNormalMode = elevation
            elevation = 0f
            background = eInkBackgroundDrawable
        } else {
            elevation = elevationInNormalMode
            background = normalBackgroundDrawable
        }
    }

    override fun setElevation(elevation: Float) {
        super.setElevation(elevation)
        if (elevation > 0f) elevationInNormalMode = elevation
    }

    var cornerRadius:  Int = Int.MAX_VALUE
        set(value) {
            field = value
            refreshBackground()
            postInvalidate()
        }

    private val borderPaint by lazy {
        Paint().apply {
            isDither = false
            style = Paint.Style.STROKE
            color = Color.BLACK
            strokeWidth = 2.takeDpAsFloat()
            isAntiAlias = true
        }
    }

    private var backgroundColor = Color.MAGENTA

    private var rippleColor = Color.WHITE

    private var eInkBackgroundDrawable = createEInkModeBackground()

    private var normalBackgroundDrawable = createRippleDrawable()

    init {
        background = normalBackgroundDrawable
        outlineProvider = object: ViewOutlineProvider() {
            override fun getOutline(view: View?, outline: Outline?) {
                view?.background?.copyBounds()?.let { outline?.setRoundRect(it, cornerRadius.toFloat()) }
            }
        }
    }

    fun setBackgroundColors(@ColorInt background: Int?, @ColorInt ripple: Int?) {
        background?.let { backgroundColor = it }
        ripple?.let { rippleColor = it }
        refreshBackground()
    }

    fun setContent(view: View?) {
        removeAllViews()
        view?.let {
            addView(it.apply {
                layoutParams = LayoutParams((layoutParams ?: LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))).apply {
                    gravity = Gravity.CENTER
                }
            })
        }
    }



    private fun refreshBackground() {
        eInkBackgroundDrawable = createEInkModeBackground()
        normalBackgroundDrawable = createRippleDrawable()
        background = if (_isInEInkMode) eInkBackgroundDrawable else normalBackgroundDrawable
    }


    private fun createRippleDrawable(): Drawable {
        return RippleDrawable(
            ColorStateList.valueOf(rippleColor),
            colorDrawableWithRoundedCorner(backgroundColor),
            colorDrawableWithRoundedCorner(rippleColor)
        )
    }

    private fun createEInkModeBackground(): Drawable {
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), colorDrawableWithRoundedCorner(Color.BLACK))
            addState(intArrayOf(-android.R.attr.state_pressed), colorDrawableWithRoundedCorner(Color.TRANSPARENT))
            addState(intArrayOf(), colorDrawableWithRoundedCorner(Color.TRANSPARENT))
        }
    }

    private fun colorDrawableWithRoundedCorner(color: Int): Drawable = createRoundedCornerColorDrawable(color, cornerRadius.toFloat())

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (_isInEInkMode) canvas.drawBorderWithRoundCorner()
    }

    private fun Canvas.drawBorderWithRoundCorner() {
        val realRadius = cornerRadius.absoluteValue.coerceAtMost(min(width, height)).toFloat()
        //strokeWidth / 2
        val offset = 2.takeDpAsFloat() / 2
        drawRoundRect(RectF(offset, offset, width - offset, height - offset), realRadius, realRadius, borderPaint)
    }


}