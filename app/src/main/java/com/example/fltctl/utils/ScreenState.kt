package com.example.fltctl.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.fltctl.AppMonitor

/**
 * Created by zeyu.zyzhang on 2/5/25
 * @author zeyu.zyzhang@bytedance.com
 */


private class InteractiveStateReceiver(private val listener: (Boolean) -> Unit): BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.run {
            when (action) {
                Intent.ACTION_SCREEN_ON -> listener(true)
                Intent.ACTION_SCREEN_OFF -> listener(false)
            }
        }
    }
}

internal object ScreenStateBroadcastReceiverStore {
    private val listeners = mutableListOf<(Boolean) -> Unit>()
    private val receiver = InteractiveStateReceiver { interactive ->
        listeners.forEach { it(interactive) }
    }
    private val filter = IntentFilter().apply {
        addAction(Intent.ACTION_SCREEN_ON)
        addAction(Intent.ACTION_SCREEN_OFF)
    }
    private val context by lazy { AppMonitor.appContext }
    fun registerListener(l: (Boolean) -> Unit) {
        if (listeners.isEmpty()) {
            context.registerReceiver(receiver, filter)
        }
        listeners.add(l)
    }
    fun unregisterListener(l: (Boolean) -> Unit) {
        listeners.remove(l)
        if (listeners.isEmpty()) {
            context.unregisterReceiver(receiver)
        }
    }
}
fun registerScreenInteractiveStateListener(lifecycle: Lifecycle, l: (interactive: Boolean) -> Unit) {
    lifecycle.addObserver(object: LifecycleEventObserver{
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            when (event) {
                Lifecycle.Event.ON_CREATE -> ScreenStateBroadcastReceiverStore.registerListener(l)
                Lifecycle.Event.ON_DESTROY -> ScreenStateBroadcastReceiverStore.unregisterListener(l)
                else -> {}
            }
        }

    })
}

