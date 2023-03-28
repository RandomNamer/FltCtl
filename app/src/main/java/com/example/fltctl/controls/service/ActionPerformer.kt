package com.example.fltctl.controls.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent
import android.view.inputmethod.BaseInputConnection
import com.example.fltctl.AppMonitor
import com.example.fltctl.controls.arch.FloatingControlManager
import java.lang.ref.WeakReference

interface IActionsPerformer: AccessibilityServiceSupportedActions {
    fun pressVolumeUp()
    fun pressVolumeDown()
}

interface AccessibilityServiceSupportedActions {
    fun pressHome()
    fun pressBack()
    fun tryTurnPage(forward: Boolean)
}

object ActionPerformer: IActionsPerformer {

    private var accessibilityServiceRef: WeakReference<FloatingControlAccessibilityService> = WeakReference(null)

    fun onServiceConnect(service: FloatingControlAccessibilityService) {
        accessibilityServiceRef = WeakReference(service)
    }

    fun onServiceOff() {
        accessibilityServiceRef.clear()
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


    override fun pressHome() {
        accessibilityServiceRef.get()?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }

    override fun pressBack() {
        accessibilityServiceRef.get()?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }

    override fun tryTurnPage(forward: Boolean) {
        accessibilityServiceRef.get()?.turnPage(forward)
    }

}