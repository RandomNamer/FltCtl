package com.example.fltctl.tests.views

import android.text.TextUtils
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import com.example.fltctl.R
import com.example.fltctl.tests.UiTest
import com.example.fltctl.widgets.view.takeDp

/**
 * Created by zeyu.zyzhang on 3/27/25
 * @author zeyu.zyzhang@bytedance.com
 */
val textViewEllipsizeTest = UiTest.ViewProviderUiTest(
    title = "TextView Ellipsize",
    viewProvider = { context ->
        val username = "dasagewffsdfadasasfsgweq"
        TextView(context).apply {
            layoutParams = FrameLayout.LayoutParams(200.takeDp(), FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            }
            textSize = 14f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.MIDDLE
            text = context.getString(R.string.reply_title, username)
        }
    }
)