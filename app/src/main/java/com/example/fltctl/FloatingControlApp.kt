package com.example.fltctl

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import com.example.fltctl.configs.SettingsCache
import com.example.fltctl.controls.service.appaware.appTriggerSharedScope
import com.example.fltctl.tests.TestEntry
import com.example.fltctl.tests.UiTest
import com.example.fltctl.tests.ViewBasedTestsContainerActivity
import com.example.fltctl.utils.LogProxy
import com.example.fltctl.utils.MmapLogProxy
import com.example.fltctl.utils.stackTraceAsString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.system.exitProcess

class FloatingControlApp: Application() {

    companion object {
        const val TAG = "Application"
        const val CRASH_RECOVER = "crash_recover"
        const val CRASH_STACKTRACE_STR = "crash_stacktrace"
    }
    override fun onCreate() {
        super.onCreate()
        AppMonitor.onAppCreate(this)
        MmapLogProxy.initialize(this)
        MmapLogProxy.getInstance().log(LogProxy.INFO, TAG, "onCreate")
        SettingsCache.init(this)
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            val stacktraceString = e.stackTraceAsString()
            MmapLogProxy.getInstance().run {
                logSync(LogProxy.ERROR, TAG, "Thread ${t.name} crashed: ${e.message}")
                logSync(LogProxy.ERROR, TAG, "Stacktrace:")
                stacktraceString.split('\n').forEach {
                    logSync(LogProxy.ERROR, "", it)
                }
                flush()
            }
            val relaunchIntent = PendingIntent.getActivity(
                applicationContext,
                0,
                Intent(applicationContext, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(CRASH_RECOVER, true)
                    putExtra(CRASH_STACKTRACE_STR, stacktraceString)
                },
                PendingIntent.FLAG_IMMUTABLE
            )
            (getSystemService(ALARM_SERVICE) as AlarmManager).set(AlarmManager.RTC, System.currentTimeMillis(), relaunchIntent)
            exitProcess(10)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        MmapLogProxy.getInstance().flush()
    }

    override fun onTerminate() {
        super.onTerminate()
        MmapLogProxy.getInstance().flush()
    }
}

interface AppBackgroundListener {
    fun onAppBackground() {}
    fun onAppForeground() {}
}

interface IAppMonitorService {
    val appContext: Context
    val topActivity: WeakReference<Activity>
    fun addListener(l: AppBackgroundListener)
    fun removeListener(l: AppBackgroundListener)
    fun addStartupTestPage(test: UiTest)
}

@SuppressLint("StaticFieldLeak")
object AppMonitor: IAppMonitorService {

    @JvmStatic
    private var applicationRef = WeakReference<Application>(null)

    override val appContext: Context
        get() = applicationRef.get()?.applicationContext ?: throw IllegalStateException("Current application ref is null")

    private val listeners = CopyOnWriteArraySet<AppBackgroundListener>()

    private var appStartupTest: UiTest? = null

    private var coldStarted = false

    private val mainThreadScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val activityLifecycleCallbacks = object: Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            synchronized(this@AppMonitor) {
                aliveActivities.remove(activity)
                aliveActivities.add(activity)
            }
        }

        override fun onActivityStarted(activity: Activity) {
            synchronized(this@AppMonitor) {
                startedActivities.add(activity)
            }
        }

        override fun onActivityResumed(activity: Activity) {
            synchronized(this@AppMonitor) {
                if (!isInForeground) listeners.forEach { it.onAppForeground() }
                isInForeground = true
                if (!coldStarted && _topActivity.get() == null) {
                    onColdStartComplete()
                }
                _topActivity = WeakReference(activity)
            }
        }

        override fun onActivityPaused(activity: Activity) {
        }

        override fun onActivityStopped(activity: Activity) {
            synchronized(this@AppMonitor) {
                startedActivities.remove(activity)
                if (isInForeground && startedActivities.isEmpty()) {
                    listeners.forEach { it.onAppBackground() }
                    isInForeground = false
                }
            }
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

        }

        override fun onActivityDestroyed(activity: Activity) {
            synchronized(this@AppMonitor) {
                aliveActivities.remove(activity)
                aliveActivities.add(activity)
            }
        }

    }

    @JvmStatic
    private val aliveActivities = mutableListOf<Activity>()

    @JvmStatic
    private val startedActivities = mutableListOf<Activity>()

    override val topActivity:WeakReference<Activity>
        get() = _topActivity

    @JvmStatic
    private var _topActivity: WeakReference<Activity> = WeakReference(null)

    @JvmStatic
    private var isInForeground = false

    private fun onColdStartComplete() {
        coldStarted = true
        TestEntry
        mainThreadScope.launch {
            delay(500L)
            appStartupTest?.let {
                _topActivity.get()?.let { act ->
                    ViewBasedTestsContainerActivity.launch(act, it)
                }
            }
            appStartupTest = null
        }
    }


    internal fun onAppCreate(inst: Application) {
        inst.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
        applicationRef = WeakReference(inst)
    }

    override fun addListener(l: AppBackgroundListener) {
        listeners.add(l)
    }

    override fun removeListener(l: AppBackgroundListener) {
        listeners.remove(l)
    }

    override fun addStartupTestPage(test: UiTest) {
        appStartupTest = test
    }
}
