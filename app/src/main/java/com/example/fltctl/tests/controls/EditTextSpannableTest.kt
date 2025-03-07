package com.example.fltctl.tests.controls

import android.content.Context
import android.view.View
import android.widget.EditText
import com.example.fltctl.controls.arch.FloatingControl

/**
 * Created by zeyu.zyzhang on 3/6/25
 * @author zeyu.zyzhang@bytedance.com
 */
class EditTextSpannableTest: FloatingControl(){

    override fun onCreateView(context: Context): View {
        return EditText(context)
    }
}