package com.example.fltctl.tests

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.annotation.IntDef
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import com.example.fltctl.controls.arch.FloatingControl
import com.example.fltctl.controls.arch.FloatingControlInfo
import com.example.fltctl.ui.toast

/**
 * We dont like fragments. For complex UI, compose is sufficient for SAA.
 */
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
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_DESC = "desc"

        private val transactionObjStore = mutableMapOf<String, Any>()
        private const val TRANSACTION_OBJ_FLTCTL = "TRANSACTION_OBJ_FLTCTL"
        private const val TRANSACTION_OBJ_COMPOSE = "TRANSACTION_OBJ_COMPOSE"

        fun <V> MutableMap<String, Any>.getAndRemove(key: String): V? = (get(key) as? V)?.also { remove(key) }

        @JvmStatic
        fun launch(context: Context, floatingControl: FloatingControlInfo) {
            transactionObjStore.put(TRANSACTION_OBJ_FLTCTL, floatingControl)
            context.startActivity(Intent(context, ViewBasedTestsContainerActivity::class.java).apply {
                putExtra(EXTRA_LAUNCH_MODE, LAUNCH_MODE_FLTCTL)
            })
        }

        @JvmStatic
        fun launch(context: Context, test: UiTest) {
            val intent = Intent(context, ViewBasedTestsContainerActivity::class.java).apply {
                putExtra(EXTRA_TITLE, test.title)
                putExtra(EXTRA_DESC, test.description)
            }

            when(test) {
                is UiTest.FltCtlUiTest -> {
                    transactionObjStore.put(TRANSACTION_OBJ_FLTCTL, test.info)
                    intent.putExtra(EXTRA_LAUNCH_MODE, LAUNCH_MODE_FLTCTL)
                }

                is UiTest.ComposeUiTest -> {
                    transactionObjStore.put(TRANSACTION_OBJ_COMPOSE, test)
                    intent.putExtra(EXTRA_LAUNCH_MODE, LAUNCH_MODE_COMPOSE)
                }
                is UiTest.ViewProviderUiTest -> {
                    intent.putExtra(EXTRA_LAUNCH_MODE, LAUNCH_MODE_VIEW_CREATER)
                    //TODO: support it
                }
            }
            context.startActivity(intent)
        }
    }

    @LaunchMode
    private var mode: Int = LAUNCH_MODE_NOTHING

    private lateinit var rootContainer: FrameLayout
    private var floatingControlInstance: FloatingControl? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parseIntent()
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

    private fun parseIntent() {
        intent.extras?.run {
            mode = getInt(EXTRA_LAUNCH_MODE, LAUNCH_MODE_NOTHING)
            getString(EXTRA_TITLE)?.let { title = it }
        }

    }

    private fun initView(container: FrameLayout) {
        when (mode) {
            LAUNCH_MODE_FLTCTL -> {
                transactionObjStore.getAndRemove<FloatingControlInfo>(TRANSACTION_OBJ_FLTCTL)
                    ?.let { fi ->
                        floatingControlInstance = fi.klass.java.newInstance()
                        title = fi.displayName
//                   supportActionBar?.title = fi.displayName
                        floatingControlInstance?.run {
                            create(this@ViewBasedTestsContainerActivity, true)
                            val view = getView().apply {
                                val originalLp = layoutParams
                                layoutParams = FrameLayout.LayoutParams(
                                    originalLp.width,
                                    originalLp.height,
                                    Gravity.CENTER
                                )
                            }
                            container.addView(view)
                        }
                    }
            }
            LAUNCH_MODE_VIEW_CREATER, LAUNCH_MODE_VIEW_XML -> {
                toast("To be supported")
            }
            LAUNCH_MODE_COMPOSE -> initCompose(container)
            LAUNCH_MODE_NOTHING -> toast("Illegal Launch")
        }

    }

    private fun initCompose(container: FrameLayout) {
        val composeUiTest = transactionObjStore.getAndRemove<UiTest.ComposeUiTest>(TRANSACTION_OBJ_COMPOSE)
        container.addView(ComposeView(this).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setContent {
                Box {
                    composeUiTest?.run {
                        Content()
                    } ?: run {
                        Box {
                            Text("No compatible compose test found", Modifier.align(Alignment.Center))
                        }
                    }
                }
            }
        })
    }

}