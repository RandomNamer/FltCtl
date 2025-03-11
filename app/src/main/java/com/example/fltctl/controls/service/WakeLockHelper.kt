package com.example.fltctl.controls.service

import android.annotation.SuppressLint
import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import androidx.annotation.UiThread
import com.example.fltctl.configs.WakeLockStrategy
import com.example.fltctl.configs.localWakeLockStrategy
import com.example.fltctl.ui.toast
import com.example.fltctl.utils.logs
import java.lang.ref.WeakReference

/**
 * Created by zeyu.zyzhang on 2/5/25
 * @author zeyu.zyzhang@bytedance.com
 */
class WakeLockHelper(private val context: Context): WakeLockActions {
    companion object {
        private const val TAG = "FltCtl:WakeLockHelper"

        private const val INDEFINITE_TIMEOUT = 1000 * 60 * 60 * 10L //10h
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private val powerManager: PowerManager by lazy {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }
    private val strategy: WakeLockStrategy by lazy {
        localWakeLockStrategy
    }
    private var requestedViewRef = WeakReference<View>(null)

    private val resetViewFlagsRunnable = Runnable {
        requestedViewRef.get()?.let {
            setWakelockFlags(it, false)
            requestedViewRef.clear()
        }
    }

    private val windowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private val log by logs(TAG)

    private var originalScreenTimeout: Long = 0L

    private var lastRequestedTimeout: Long = -1L

    override fun tryAcquireWakeLockIndefinitely(with: View?) {
        tryAcquireWakeLock(-1L, with)
    }

    override fun tryAcquireWakeLock(timeout: Long, with: View?) {
        log.i( "Wakelock req: $timeout, $strategy")
       if (!strategy.writeSettings || !acquireWakeLockByChangeSettings(if (timeout < 0L) INDEFINITE_TIMEOUT else timeout)) {
           if (!strategy.useView || with == null) {
               acquireWakeLockInternal(strategy.wakeLockType, timeout)
           } else {
               applyWakeLockOnView(with, timeout)
           }
       }
        when {
            strategy.writeSettings -> acquireWakeLockByChangeSettings(INDEFINITE_TIMEOUT)
            strategy.useView && with != null -> applyWakeLockOnView(with, timeout)
            else -> acquireWakeLockInternal(strategy.wakeLockType, timeout)
        }
    }

    private fun acquireWakeLockByChangeSettings(timeout: Long): Boolean {
        if (timeout > 10000L) {
            val curSettingValue = getScreenTimeout()
            if (curSettingValue > 0L && curSettingValue != lastRequestedTimeout) {
                originalScreenTimeout = curSettingValue
                lastRequestedTimeout = timeout
                return modifyScreenTimeout(INDEFINITE_TIMEOUT)
            }
        }
        return false
    }

    private fun applyWakeLockOnView(view: View, timeout: Long = -1L) {
        if (requestedViewRef.get() != null) {
            removeWakeLockOnView()
        }
        view.post {
            if (setWakelockFlags(view, true)) {
                requestedViewRef = WeakReference(view)
                if (timeout > 0L) {
                    view.postDelayed(resetViewFlagsRunnable, timeout)
                }
            }
        }
    }

    private fun removeWakeLockOnView() {
        requestedViewRef.get()?.let {
            setWakelockFlags(it, false)
            requestedViewRef.clear()
        }
    }

    @UiThread
    private fun setWakelockFlags(view: View, add: Boolean): Boolean {
       val topLevelView = if (view.layoutParams is WindowManager.LayoutParams) view
            else if ((view.parent as? View)?.layoutParams is WindowManager.LayoutParams) (view.parent as View)
            else return false
        val lp = topLevelView.layoutParams as WindowManager.LayoutParams
        if (add) {
            lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        } else {
            lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.inv()
        }
        try {
            windowManager.updateViewLayout(topLevelView, lp)
        } catch (e: Exception) {
            log.e( "Failed to set view flags, is it attached?")
            return false
        }
        return true
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLockInternal(type: Int = PowerManager.PARTIAL_WAKE_LOCK, timeout: Long = -1L) {
        try {
            if (wakeLock == null) {
                wakeLock = powerManager.newWakeLock(type, TAG)
            }
            if (timeout > 0L) wakeLock?.acquire(timeout)
            else wakeLock?.acquire()
        } catch (e: Exception) {
            context.toast("Failed to acquire wake lock by: ${e.message}")
        }
    }

    private fun getScreenTimeout(): Long {
        if (Settings.System.canWrite(context)) {
            try {
                return Settings.System.getLong(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return -1L
    }

    private fun modifyScreenTimeout(value: Long): Boolean {
        log.d("Modify timeout real call: to=$value, original=$originalScreenTimeout")
        if(Settings.System.canWrite(context)) {
            try {
                Settings.System.putLong(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, value)
                return true
            } catch (e: Exception) {
                context.toast("Failed to modify screen timeout by: ${e.message}")
                e.printStackTrace()
            }
        }
        return false
    }

    private fun restoreScreenTimeout() {
        originalScreenTimeout.takeIf { it > 100 * 1000L && it != INDEFINITE_TIMEOUT }?.let {
            modifyScreenTimeout(it)
        }
    }

    override fun releaseWakeLock() {
        log.i( "Wakelock release")
        removeWakeLockOnView()
        restoreScreenTimeout()
        wakeLock?.release()
        wakeLock = null
    }

}