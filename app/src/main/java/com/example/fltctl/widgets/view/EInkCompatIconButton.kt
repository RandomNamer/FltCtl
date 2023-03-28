package com.example.fltctl.widgets.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.ImageView
import androidx.annotation.ColorInt

class EInkCompatIconButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.borderlessButtonStyle
): EInkCompatRoundedButton(context, attrs, defStyleAttr) {

    private val imageView = ImageView(context)

    private var tintColor = ColorStateList.valueOf(Color.BLACK)

    private val eInkColors = ColorStateList.valueOf(Color.BLACK)

    init {
        setContent(imageView)
    }

    fun setIcon(drawable: Drawable) {
        imageView.setImageDrawable(drawable)
        postInvalidate()
    }

    fun setIconTint(@ColorInt color: Int) {
        tintColor = ColorStateList.valueOf(color)
        if (!isInEInkMode) imageView.imageTintList = tintColor
        postInvalidate()
    }

    override fun onEInkStateChanged(value: Boolean) {
        super.onEInkStateChanged(value)
        imageView.imageTintList = if (value) eInkColors else tintColor
    }
}