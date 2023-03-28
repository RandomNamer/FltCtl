package com.example.fltctl.widgets.window

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.example.fltctl.AppMonitor


internal object ConfigurationHelper {
    private val changeReceiver by lazy {
        ConfigurationChangeReceiver {
            dispatchOnConfigChanged()
        }
    }

    private val changeListeners = mutableListOf<() -> Unit>()

    fun addOnChangeListener(l: () -> Unit) {
        changeListeners.add(l)
        if (changeListeners.size == 1) {
            registerOnChangeReceiver()
        }
    }

    fun removeOnChangeListener(l: () -> Unit) {
        changeListeners.remove(l)
        if (changeListeners.size == 0) {
            unregisterOnChangeReceiver()
        }
    }

    fun removeAllOnChangeListeners() {
        changeListeners.clear()
        unregisterOnChangeReceiver()
    }

    private fun dispatchOnConfigChanged() {
        changeListeners.forEach { it.invoke() }
    }

    private fun registerOnChangeReceiver() {
        AppMonitor.appContext.registerReceiver(changeReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_CONFIGURATION_CHANGED)
        })
    }

    private fun unregisterOnChangeReceiver() {
        AppMonitor.appContext.unregisterReceiver(changeReceiver)
    }
}

private class ConfigurationChangeReceiver(private val onChanged: () -> Unit): BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_CONFIGURATION_CHANGED) {
            onChanged.invoke()
        }
    }
}