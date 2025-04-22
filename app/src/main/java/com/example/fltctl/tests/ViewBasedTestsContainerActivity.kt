package com.example.fltctl.tests

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.annotation.IntDef
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.util.fastForEachIndexed
import com.example.fltctl.controls.arch.FloatingControl
import com.example.fltctl.controls.arch.FloatingControlInfo
import com.example.fltctl.ui.toast
import kotlin.reflect.KClass

/**
 * We dont like fragments. For complex UI, compose is sufficient for SAA.
 */
class ViewBasedTestsContainerActivity : AppCompatActivity() {

    companion object {
        private const val PERSIST_WHEN_CONFIG_CHANGE = false

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
        private const val EXTRA_LAYOUT_ID = "layout_id"

        private val transactionObjStore = mutableMapOf<String, Any>()
        private const val TRANSACTION_OBJ_FLTCTL = "TRANSACTION_OBJ_FLTCTL"
        private const val TRANSACTION_OBJ_COMPOSE = "TRANSACTION_OBJ_COMPOSE"
        private const val TRANSACTION_OBJ_COMMON = "TRANSACTION_OBJ_ORIGINAL_OBJ"
        private const val TRANSACTION_OBJ_VIEW_CREATOR = "TRANSACTION_OBJ_VIEW_CREATOR"

        private const val LAUNCH_CLASSNAME = "OBJ_CLASSNAME"

        fun <V> MutableMap<String, Any>.getAndRemove(key: String): V? = (get(key) as? V)?.also { if (!PERSIST_WHEN_CONFIG_CHANGE) remove(key) }

        @JvmStatic
        fun launch(context: Context, floatingControl: FloatingControlInfo) {
            transactionObjStore.put(TRANSACTION_OBJ_FLTCTL, floatingControl)
            context.startActivity(Intent(context, ViewBasedTestsContainerActivity::class.java).apply {
                putExtra(EXTRA_LAUNCH_MODE, LAUNCH_MODE_FLTCTL)
            })
        }

        @JvmStatic
        @JvmOverloads
        fun launch(context: Context, test: UiTest, intentTransform: ((Intent) -> Unit)? = null) {
            val intent = Intent(context, ViewBasedTestsContainerActivity::class.java).apply {
                putExtra(EXTRA_TITLE, test.title)
                putExtra(EXTRA_DESC, test.description)
            }
            transactionObjStore.clear()
            transactionObjStore.put(TRANSACTION_OBJ_COMMON, test)

            when(test) {
                is UiTest.FltCtlUiTest -> {
                    transactionObjStore.put(TRANSACTION_OBJ_FLTCTL, test.info)
                    intent.putExtra(EXTRA_LAUNCH_MODE, LAUNCH_MODE_FLTCTL)
                }

                is UiTest.ComposeUiTest,
                is UiTest.ComposeUiTestWrap -> {
                    transactionObjStore.put(TRANSACTION_OBJ_COMPOSE, test)
                    intent.putExtra(EXTRA_LAUNCH_MODE, LAUNCH_MODE_COMPOSE)
                }
                is UiTest.ViewProviderUiTest -> {
                    intent.putExtra(EXTRA_LAUNCH_MODE, LAUNCH_MODE_VIEW_CREATER)
                    transactionObjStore.put(TRANSACTION_OBJ_VIEW_CREATOR, test.viewProvider)
                }
                is UiTest.XmlUiTest -> {
                    intent.putExtra(EXTRA_LAUNCH_MODE, LAUNCH_MODE_VIEW_XML)
                    intent.putExtra(EXTRA_LAYOUT_ID, test.layoutId)
                }
            }
            intentTransform?.invoke(intent)
            context.startActivity(intent)
        }

        fun launch(context: Context, testClz: KClass<out UiTest>, intentTransform: ((Intent) -> Unit)? = null) {
            launch(context, testClz.java.newInstance()) {
                intentTransform?.invoke(it)
                it.putExtra(LAUNCH_CLASSNAME, testClz.java.name)
            }
        }
    }

    @LaunchMode
    private var mode: Int = LAUNCH_MODE_NOTHING

    private lateinit var rootContainer: FrameLayout
    private var floatingControlInstance: FloatingControl? = null

    private val menuItems = mutableListOf<SimpleMenuItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        intent.getStringExtra(LAUNCH_CLASSNAME)?.let {
            transactionObjStore.put(TRANSACTION_OBJ_COMMON, Class.forName(it).newInstance())
        }
        transactionObjStore.getAndRemove<UiTest>(TRANSACTION_OBJ_COMMON)?.run {
            onActivityCreate(this@ViewBasedTestsContainerActivity)
            menuItems.clear()
            menuItems.addAll(produceMenuItems(this@ViewBasedTestsContainerActivity))
        }
        super.onCreate(savedInstanceState)
        parseIntent()
        rootContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }
        initView(rootContainer)
        setContentView(rootContainer)
        transactionObjStore.clear()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuItems.fastForEachIndexed { idx, item ->
            menu?.add(Menu.NONE, idx, Menu.NONE, item.title)?.apply {
                setIcon(item.iconRes)
                setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            }
        }
        return menuItems.isNotEmpty() || super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        item.itemId.takeIf { it in menuItems.indices }?.let {
            menuItems[it].callback(this)
        }
        return super.onOptionsItemSelected(item)
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
        transactionObjStore.clear()
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
            LAUNCH_MODE_VIEW_CREATER-> {
                transactionObjStore.getAndRemove<(Context) -> View>(TRANSACTION_OBJ_VIEW_CREATOR)?.let {
                    container.addView(it.invoke(this))
                }
            }
            LAUNCH_MODE_COMPOSE -> initCompose(container)
            LAUNCH_MODE_VIEW_XML -> {
                val id = intent.getIntExtra(EXTRA_LAYOUT_ID, 0)
                if (id != 0) {
                    val view = LayoutInflater.from(this).inflate(id, container, false)
                    container.addView(view)
                }
            }
            LAUNCH_MODE_NOTHING -> toast("Illegal Launch")
        }

    }

    private fun initCompose(container: FrameLayout) {
        val composeUiTest = transactionObjStore.getAndRemove<UiTest.ComposeUiTest>(TRANSACTION_OBJ_COMPOSE) ?: run {
            intent.getStringExtra(LAUNCH_CLASSNAME)?.let {
                val clz = Class.forName(it)
                (clz.newInstance() as? UiTest.ComposeUiTestWrap)?.data
            }
        }
        container.addView(ComposeView(this).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setContent {
                Box {
                    composeUiTest?.run {
                        content.invoke(this@Box)
                        if (fullscreen) {
                            supportActionBar?.hide()
                        }
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