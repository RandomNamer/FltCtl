package com.example.fltctl.controls.service

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent
import android.view.inputmethod.BaseInputConnection
import com.example.fltctl.AppMonitor
import com.example.fltctl.controls.arch.FloatingControlManager
import java.lang.ref.WeakReference

interface CommonActions {
    fun pressVolumeUp()
    fun pressVolumeDown()
}

interface WakeLockActions {
    fun tryAcquireWakeLockIndefinitely()
    fun tryAcquireWakeLock(timeout: Long)
    fun releaseWakeLock()
}

interface RequireAccessibilityServiceActions {
    fun pressHome()
    fun pressBack()
    fun tryTurnPage(forward: Boolean)
    fun tryTurnPageVertically(forward: Boolean)
    fun queryFocusedApp(): String?
}

private interface AccessibilityServiceActionWrapper: RequireAccessibilityServiceActions {
    fun onServiceConnect(service: RequireAccessibilityServiceActions)

    fun onServiceDisconnect()
}

object ActionPerformer:
    CommonActions,
    WakeLockActions by WakeLockHelper(AppMonitor.appContext),
    AccessibilityServiceActionWrapper by AccessibilityServiceActionWrapperImpl {

    private object AccessibilityServiceActionWrapperImpl: AccessibilityServiceActionWrapper {

        private var accessibilityServiceRef: WeakReference<RequireAccessibilityServiceActions> = WeakReference(null)
        override fun onServiceConnect(service: RequireAccessibilityServiceActions) {
            accessibilityServiceRef = WeakReference(service)
        }

        override fun onServiceDisconnect() {
            accessibilityServiceRef.clear()
        }

        override fun pressHome() {
            accessibilityServiceRef.get()?.pressHome()
        }

        override fun pressBack() {
            accessibilityServiceRef.get()?.pressBack()
        }

        override fun tryTurnPage(forward: Boolean) {
            accessibilityServiceRef.get()?.tryTurnPage(forward)
        }

        override fun tryTurnPageVertically(forward: Boolean) {
            accessibilityServiceRef.get()?.tryTurnPageVertically(forward)
        }

        override fun queryFocusedApp(): String? = accessibilityServiceRef.get()?.queryFocusedApp()

    }

    private val audioManager by lazy {
        AppMonitor.appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private fun simulateInput(keyCode: Int, delay: Long = 0L) {
        FloatingControlManager.getCurrentViewOnly()?.let {
            val ic = BaseInputConnection(it, true)
            val down = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            val up = KeyEvent(KeyEvent.ACTION_UP, keyCode)
            ic.sendKeyEvent(down)
            it.postDelayed({
                ic.sendKeyEvent(up)
            }, delay)

        }
    }

    override fun pressVolumeUp() {
        audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
    }

    override fun pressVolumeDown() {
//        simulateInput(KeyEvent.KEYCODE_VOLUME_DOWN)
        audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
    }


}