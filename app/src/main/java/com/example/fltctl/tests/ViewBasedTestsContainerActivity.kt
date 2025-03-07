package com.example.fltctl.tests

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.annotation.IntDef
import androidx.appcompat.app.AppCompatActivity
import com.example.fltctl.controls.arch.FloatingControl
import com.example.fltctl.controls.arch.FloatingControlInfo
import com.example.fltctl.ui.toast

class ViewBasedTestsContainerActivity : AppCompatActivity() {

    companion object {
        private const val LAUNCH_MODE_NOTHING = 0
        private const val LAUNCH_MODE_FLTCTL = 1
        private const val LAUNCH_MODE_VIEW_CREATER = 2
        private const val LAUNCH_MODE_VIEW_XML = 3
        private const val LAUNCH_MODE_COMPOSE = 4

        @Retention(AnnotationRetention.SOURCE)
        @Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
        @IntDef(LAUNCH_MODE_NOTHING, LAUNCH_MODE_FLTCTL, LAUNCH_MODE_VIEW_CREATER, LAUNCH_MODE_VIEW_XML, LAUNCH_MODE_COMPOSE)
        annotation class LaunchMode

        private const val EXTRA_LAUNCH_MODE = "launch_mode"

        private val transactionObjStore = mutableMapOf<String, Any>()
        private const val TRANSACTION_OBJ_FLTCTL = "TRANSACTION_OBJ_FLTCTL"

        fun <V> MutableMap<String, Any>.getAndRemove(key: String): V? = (get(key) as? V)?.also { remove(key) }

        @JvmStatic
        fun launch(context: Context, floatingControl: FloatingControlInfo) {
            transactionObjStore.put(TRANSACTION_OBJ_FLTCTL, floatingControl)
            context.startActivity(Intent(context, ViewBasedTestsContainerActivity::class.java).apply {
                putExtra(EXTRA_LAUNCH_MODE, LAUNCH_MODE_FLTCTL)
            })
        }
    }

    @LaunchMode
    private var mode: Int = LAUNCH_MODE_NOTHING

    private lateinit var rootContainer: ViewGroup
    private var floatingControlInstance: FloatingControl? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mode = intent.extras?.getInt(EXTRA_LAUNCH_MODE) ?: LAUNCH_MODE_NOTHING
        rootContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }
        initView(rootContainer)
        setContentView(rootContainer)
    }

    override fun onStart() {
        super.onStart()
        floatingControlInstance?.dispatchStart()
    }

    override fun onStop() {
        super.onStop()
        floatingControlInstance?.dispatchStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingControlInstance?.destroy()
    }

    private fun initView(container: ViewGroup) {
        when (mode) {
           LAUNCH_MODE_FLTCTL -> {
               transactionObjStore.getAndRemove<FloatingControlInfo>(TRANSACTION_OBJ_FLTCTL)?.let { fi ->
                   floatingControlInstance = fi.klass.java.newInstance()
                   title = fi.displayName
//                   supportActionBar?.title = fi.displayName
                   floatingControlInstance?.run {
                       create(this@ViewBasedTestsContainerActivity, true)
                       val view = getView().apply {
                           val originalLp = layoutParams
                           layoutParams = FrameLayout.LayoutParams(originalLp.width, originalLp.height, Gravity.CENTER)
                       }
                       container.addView(view)
                   }
               }
           }
            LAUNCH_MODE_NOTHING -> toast("Illegal Launch")
        }

    }

}