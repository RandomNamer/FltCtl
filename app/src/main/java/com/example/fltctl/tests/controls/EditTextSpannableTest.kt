package com.example.fltctl.tests.controls

import android.content.Context
import android.view.View
import android.widget.EditText
import com.example.fltctl.controls.arch.FloatingControlInfo
import com.example.fltctl.tests.UiTest

/**
 * Created by zeyu.zyzhang on 3/6/25
 * @author zeyu.zyzhang@bytedance.com
 */
class EditTextSpannableTest: UiTest.FltCtlUiTest() {

    override val info: FloatingControlInfo
        get() = FloatingControlInfo(
            displayName = "EditText Spannable",
            klass = EditTextSpannableTest::class
        )

    override fun onCreateView(context: Context): View {
        return EditText(context)
    }
}