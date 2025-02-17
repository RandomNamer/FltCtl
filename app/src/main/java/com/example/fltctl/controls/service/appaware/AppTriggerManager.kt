package com.example.fltctl.controls.service.appaware

import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.fltctl.controls.service.ActionPerformer
import com.example.fltctl.configs.PackageNames
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Created by zeyu.zyzhang on 2/15/25
 * @author zeyu.zyzhang@bytedance.com
 */

interface AppTrigger {
    val triggerList: List<String>
    val tag: String
    fun onActive()
    fun onInactive()
    fun onAppFocusChanged(packageName: String) {}
    fun onEvent(ev: AppTriggerManager.LocalAccessibilityEvent) {}
    fun onActivityFocusChanged(from: String, to: String) {}
}

internal val appTriggerSharedScope = CoroutineScope(Dispatchers.Default)


object AppTriggerManager {

    data class LocalAccessibilityEvent(
        val eventType: Int,
        val eventTime: Long,
        val packageName: String?,
        val contentChangeTypes: Int,
        val windowChangeTypes: Int,
        val className: String?,
    ) {
        companion object {
            fun of(event: AccessibilityEvent) = LocalAccessibilityEvent(
                eventType = event.eventType,
                eventTime = event.eventTime,
                packageName = event.packageName?.toString(),
                contentChangeTypes = event.contentChangeTypes,
                windowChangeTypes = event.windowChanges,
                className = event.className?.toString(),
            )
        }
    }

    private fun AccessibilityEvent.localCopy() = LocalAccessibilityEvent.of(this)

    private const val TAG = "AppTriggerMan"

    private val workerScope = CoroutineScope(Dispatchers.IO)

    private val _eventChannel = Channel<LocalAccessibilityEvent>(Channel.UNLIMITED)

    private val eventFlow = _eventChannel.receiveAsFlow().shareIn(workerScope, SharingStarted.Eagerly, replay = 10)

    private val windowStateChangedFlow = eventFlow.filter { it.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED }
    private val windowContentChangedFlow = eventFlow.filter { it.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED  }

    private val triggers = CopyOnWriteArraySet<AppTrigger>()

    private val activeTriggers = CopyOnWriteArraySet<AppTrigger>()

    private var focusedApp: String = ""

    private var focusedActivity: String = ""

    private val excludedPackages = setOf(PackageNames.SYSTEMUI, PackageNames.SETTINGS)

    init {
        preloadedTriggers.forEach { registerTrigger(it) }
        workerScope.launch(Dispatchers.IO) {
            windowStateChangedFlow.collect {
//                delay(300L)
                resolveTriggers(it)
            }
        }
        workerScope.launch(Dispatchers.IO) {
            windowContentChangedFlow.collectLatest {
                processInAppEvents(it)
            }
        }
    }

    fun onAccessibilityEvent(event: AccessibilityEvent) {
        val copy = event.localCopy()
//        Log.d(TAG, "onAccessibilityEvent: $copy")
        workerScope.launch { _eventChannel.send(copy) }
    }

    private fun onFocusedAppChanged(packageName: String) {
        Log.i(TAG, "Focused app changed: $focusedApp -> $packageName")
        if (excludedPackages.contains(packageName)) {
            focusedApp = packageName
            return
        }
        val newActiveTriggers = triggers.filter {
            it.triggerList.contains(packageName)
        }.onEach { trigger ->
           if (!activeTriggers.contains(trigger)) {
               trigger.onActive()
           }
        }
        activeTriggers.forEach {
            if (!newActiveTriggers.contains(it)) {
                it.onInactive()
            }
        }
        activeTriggers.clear()
        activeTriggers.addAll(newActiveTriggers)
        focusedApp = packageName
    }

    private fun resolveTriggers(event: LocalAccessibilityEvent) {
        ActionPerformer.queryFocusedApp()?.let {
            if (focusedApp != it) {
                //app change
                onFocusedAppChanged(it)
            } else {
                processInAppEvents(event)
            }
            if (!event.className.isNullOrEmpty() && focusedActivity != event.className) {
                //activity change
                Log.d(TAG, "Activity changed: $focusedActivity -> ${event.className}")
                activeTriggers.forEach { trigger ->
                    trigger.onActivityFocusChanged(focusedActivity, event.className)
                }
                focusedActivity = event.className
            }
        }
    }

    private fun processInAppEvents(event: LocalAccessibilityEvent) {
        activeTriggers.forEach {
            it.onEvent(event)
        }
    }

    fun registerTrigger(trigger: AppTrigger) {
        triggers.add(trigger)
        if (trigger.triggerList.contains(focusedApp)) {
            trigger.onActive()
        }
    }

    fun unregisterTrigger(trigger: AppTrigger) {
        triggers.remove(trigger)
        trigger.onInactive()
    }

    fun unregisterTriggersByPackage(packageName: String) {
        triggers.filter { it.triggerList.contains(packageName) }.forEach {
            unregisterTrigger(it)
        }
    }
}