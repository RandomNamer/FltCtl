package com.example.fltctl

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.annotation.MainThread
import com.bytedance.rheatrace.RheaTrace3
import com.example.fltctl.appselection.ui.AppSelectActivity
import com.example.fltctl.configs.SettingsCache
import com.example.fltctl.tests.TestEntry
import com.example.fltctl.tests.TestsContainerActivity
import com.example.fltctl.tests.UiTest
import com.example.fltctl.tests.compose.AfterCrash
import com.example.fltctl.tests.compose.CrashInfo
import com.example.fltctl.ui.toast
import com.example.fltctl.utils.LogProxy
import com.example.fltctl.utils.MmapLogProxy
import com.example.fltctl.utils.deepStackTraceToString
import com.example.fltctl.utils.isLauncher
import com.example.fltctl.utils.logs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.system.exitProcess

class FloatingControlApp : Application() {

    companion object {
        const val TAG = "Application"
        const val CRASH_RECOVER = "crash_recover"
        const val CRASH_STACKTRACE_STR = "crash_stacktrace"
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        if (BuildConfig.DEBUG) {
            RheaTrace3.init(base)
        }
    }

    override fun onCreate() {
        super.onCreate()
        AppMonitorImpl.onAppCreate(this)
        MmapLogProxy.initialize(this)
        MmapLogProxy.getInstance().log(LogProxy.INFO, TAG, "onCreate")
        SettingsCache.init(this)
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            val stacktraceString = e.deepStackTraceToString()
            val crashInfo = CrashInfo(
                title = "Uncaught exception at thread ${t.name}",
                message = e.message.toString(),
                time = System.currentTimeMillis(),
                stacktraceString = stacktraceString
            )

            MmapLogProxy.getInstance().run {
                logSync(LogProxy.ERROR, TAG, "Thread ${t.name} crashed: ${e.message}")
                logSync(LogProxy.ERROR, TAG, "Stacktrace:")
                stacktraceString.split('\n').forEach {
                    logSync(LogProxy.ERROR, "", it)
                }
                flush()
            }
            e.printStackTrace()
            launchCrashScreen(crashInfo)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        MmapLogProxy.getInstance().flush()
    }

    override fun onTerminate() {
        MmapLogProxy.getInstance().flush()
        super.onTerminate()
    }

    @MainThread
    private fun launchCrashScreen(info: CrashInfo) {
        AppMonitorImpl.topActivity?.run {
            TestsContainerActivity.launch(this, AfterCrash::class) {
                it.putExtra(CRASH_RECOVER, info)
            }
        }
        android.os.Process.killProcess(android.os.Process.myPid())
        exitProcess(10)
    }
}

interface AppBackgroundListener {
    fun onAppBackground() {}
    fun onAppForeground() {}
}

interface IAppMonitorService {
    val appContext: Context
    val topActivity: Activity?
    fun addListener(l: AppBackgroundListener)
    fun removeListener(l: AppBackgroundListener)
    fun addStartupTestPage(test: UiTest)
    fun restartApplication()
}

val AppMonitor: IAppMonitorService
    get() = AppMonitorImpl as IAppMonitorService

@SuppressLint("StaticFieldLeak")
private object AppMonitorImpl : IAppMonitorService {

    private val log by logs("AppMonitorImpl")

    private var applicationRef = WeakReference<Application>(null)

    override val appContext: Context
        get() = applicationRef.get()?.applicationContext ?: throw IllegalStateException("Current application ref is null")

    private val listeners = CopyOnWriteArraySet<AppBackgroundListener>()

    private var appStartupTest: UiTest? = null

    private var coldStarted = false

    private val mainThreadScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var isCrashScreenLaunch = false
        set(value) {
            if (!coldStarted) field = value
        }

    private val alarmManager by lazy { appContext.getSystemService(ALARM_SERVICE) as AlarmManager }

    private val pm by lazy { appContext.packageManager }

    private val activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {

        override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
//            if (!coldStarted) isCrashScreenLaunch = activity.intent?.hasExtra(FloatingControlApp.CRASH_RECOVER) == true
            activityBeforeCreateTasks.removeAll {
                it.invoke(activity)
                true
            }
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            synchronized(this@AppMonitorImpl) {
                aliveActivities.remove(activity)
                aliveActivities.add(activity)
            }
        }

        override fun onActivityStarted(activity: Activity) {
            synchronized(this@AppMonitorImpl) {
                startedActivities.add(activity)
            }
        }

        override fun onActivityResumed(activity: Activity) {
            synchronized(this@AppMonitorImpl) {
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
            synchronized(this@AppMonitorImpl) {
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
            synchronized(this@AppMonitorImpl) {
                aliveActivities.remove(activity)
            }
        }

    }

    internal val aliveActivities = mutableListOf<Activity>()

    private val startedActivities = mutableListOf<Activity>()

    override val topActivity: Activity?
        get() = _topActivity.get()

    private var _topActivity: WeakReference<Activity> = WeakReference(null)

    private var isInForeground = false

    private val activityBeforeCreateTasks = mutableListOf<(Activity) -> Unit>()

    private fun onColdStartComplete() {
        val firstActivity = aliveActivities.first()
        val isEligibleLaunch = isEligibleLaunch(firstActivity)
        log.i("Cold start complete: isCrashScreenLaunch = $isCrashScreenLaunch, topActivity = ${firstActivity}, isEligibleLaunch = $isEligibleLaunch")
        coldStarted = true
        if (isCrashScreenLaunch) return
        alarmManager.cancel(buildRelaunchPendingIntent())
        if (!isEligibleLaunch) {
            with(firstActivity) {
                startActivity(Intent(this, MainActivity::class.java))
                toast("Illegal Launch")
                finish()
            }
        } else {
            TestEntry
            mainThreadScope.launch {
                delay(500L)
                appStartupTest?.let {
                    _topActivity.get()?.let { act ->
                        TestsContainerActivity.launch(act, it)
                    }
                }
                appStartupTest = null
            }
        }

    }

    private fun isEligibleLaunch(firstActivity: Activity): Boolean = when (firstActivity) {
        is MainActivity -> true //stateless
        is TestEntry.TestEntryListActivity -> {
            BuildConfig.DEBUG
        }

        is AppSelectActivity -> {
            firstActivity.callingPackage?.let {
                isLauncher(pm, it)
            } == true || AppSelectActivity.validateExternalIntent(firstActivity.intent) != null
        }

        else -> {
            isCrashScreenLaunch = firstActivity.intent?.extras?.containsKey(FloatingControlApp.CRASH_RECOVER) == true
            firstActivity.isInPictureInPictureMode == true
        }
    }

    fun runBeforeNextActivityLaunch(r: (activity: Activity) -> Unit) {
        activityBeforeCreateTasks.add(r)
    }

    fun onAppCreate(inst: Application) {
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
        if (BuildConfig.DEBUG) {
            appStartupTest = test
        }
    }

    override fun restartApplication() {
        val relaunchIntent = buildRelaunchPendingIntent()
        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 500L, relaunchIntent)
//        android.os.Process.killProcess(android.os.Process.myPid())
        exitProcess(0)
    }

    private fun buildRelaunchPendingIntent() = PendingIntent.getActivity(
        appContext,
        0,
        Intent(appContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        },
        PendingIntent.FLAG_IMMUTABLE
    )
}
