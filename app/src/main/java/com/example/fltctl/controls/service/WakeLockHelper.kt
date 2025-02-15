package com.example.fltctl.controls.service

import android.annotation.SuppressLint
import android.content.Context
import android.os.PowerManager
import android.util.Log
import com.example.fltctl.ui.toast

/**
 * Created by zeyu.zyzhang on 2/5/25
 * @author zeyu.zyzhang@bytedance.com
 */
class WakeLockHelper(private val context: Context): WakeLockActions {
    companion object {
        private const val TAG = "FltCtl:WakeLockHelper"
        private const val TAG_SPECIAL = "FltCtl:WakeLockHelper:Special"

        private const val TIMEOUT = 1000 * 60 * 10L
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private val powerManager: PowerManager by lazy {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    override fun tryAcquireWakeLockIndefinitely() {
       acquireWakeLockInternal()
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLockInternal(timeout: Long = -1L) {
        Log.i(TAG, "Wakelock req: $timeout")
        try {
            if (wakeLock == null) {
                wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG)
            }
            if (timeout > 0L) wakeLock?.acquire(timeout)
            else wakeLock?.acquire()
        } catch (e: Exception) {
            context.toast("Failed to acquire wake lock by: ${e.message}")
        }
    }

    override fun tryAcquireWakeLock(timeout: Long) {
        acquireWakeLockInternal(timeout)
    }

    override fun releaseWakeLock() {
        Log.i(TAG, "Wakelock release")
        wakeLock?.release()
        wakeLock = null
    }

}