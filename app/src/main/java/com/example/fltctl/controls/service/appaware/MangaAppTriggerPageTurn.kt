package com.example.fltctl.controls.service.appaware

import android.os.Handler
import android.os.Looper
import com.example.fltctl.AppMonitor
import com.example.fltctl.configs.PackageNames
import com.example.fltctl.configs.SettingKeys
import com.example.fltctl.configs.settings
import com.example.fltctl.controls.DefaultTurnPageControl
import com.example.fltctl.controls.arch.FloatingControlInfo
import com.example.fltctl.controls.arch.FloatingControlManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Created by zeyu.zyzhang on 2/15/25
 * @author zeyu.zyzhang@bytedance.com
 */
class MangaAppTriggerPageTurn: AppTrigger {
    companion object {

        val mangaAppList = setOf(
            PackageNames.DMZJ,
            PackageNames.DMZJ_ALT,
            PackageNames.BCOMIC,
            PackageNames.DMZJ_FLUTTER,
            PackageNames.MIHON,
            PackageNames.MIHON_D,
            PackageNames.COPY,
            PackageNames.MR,
            PackageNames.KOTASU,
            PackageNames.EH_1,
            PackageNames.EH_2,
            PackageNames.KINOKO,
            PackageNames.QS,
        )
    }
    override val triggerList: Set<String>
        get() = mangaAppList
    override val tag: String = this::class.simpleName.toString()

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }


    override fun onActive() {
        val klass = DefaultTurnPageControl::class
       mainHandler.post {
           FloatingControlManager.temporarySelectControl(FloatingControlInfo("", "", klass).key)
//           FloatingControlManager.tryShowWindowWithCurrentControl()
       }
    }

    override fun onInactive() {
//        FloatingControlManager.closeWindow()
        appTriggerSharedScope.launch {
            AppMonitor.appContext.settings.run {
                val prevControlKey = data.first()[SettingKeys.CURRENT_CONTROL] ?: ""
                mainHandler.postAtFrontOfQueue {
                    FloatingControlManager.temporarySelectControl(prevControlKey)
                }
            }
        }
    }
}