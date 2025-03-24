package com.example.fltctl

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import com.example.fltctl.configs.SettingsCache
import com.example.fltctl.utils.LogProxy
import com.example.fltctl.utils.MmapLogProxy
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArraySet

class FloatingControlApp: Application() {
    override fun onCreate() {
        super.onCreate()
        AppMonitor.onAppCreate(this)
        MmapLogProxy.initialize(this)
        MmapLogProxy.getInstance().log(LogProxy.INFO, "Application", "onCreate")
        SettingsCache.init(this)
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
}

@SuppressLint("StaticFieldLeak")
object AppMonitor: IAppMonitorService {

    @JvmStatic
    private var applicationRef = WeakReference<Application>(null)

    override val appContext: Context
        get() = applicationRef.get()?.applicationContext ?: throw IllegalStateException("Current application ref is null")

    private val listeners = CopyOnWriteArraySet<AppBackgroundListener>()

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
                if (!isInForeground)
                    listeners.forEach { it.onAppForeground() }
                    isInForeground = true
                _topActivity = activity
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

    override val topActivity: WeakReference<Activity>
        get() = WeakReference(_topActivity)

    @JvmStatic
    var _topActivity: Activity? = null
        private set

    @JvmStatic
    private var isInForeground = false


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
}
