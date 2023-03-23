package com.example.fltctl

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet

class FloatingControlApp: Application() {
    override fun onCreate() {
        super.onCreate()
        AppMonitor.onAppCreate(this)
    }
}

@SuppressLint("StaticFieldLeak")
object AppMonitor {

    interface AppBackgroundListener {
        fun onAppBackground() {}
        fun onAppForeground() {}
    }

    @JvmStatic
    private var applicationRef = WeakReference<Application>(null)

    val appContext: Context
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
                topActivity = activity
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

    @JvmStatic
    var topActivity: Activity? = null
        private set

    @JvmStatic
    private var isInForeground = false

    private val currentActivity: Activity?
        get() = topActivity

    fun onAppCreate(inst: Application) {
        inst.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
        applicationRef = WeakReference(inst)
    }

    fun addListener(l: AppBackgroundListener) {
        listeners.add(l)
    }

    fun removeListener(l: AppBackgroundListener) {
        listeners.remove(l)
    }
}
