package com.example.fltctl.controls.service.appaware

import com.example.fltctl.configs.commonSettingScope
import com.example.fltctl.controls.service.ActionPerformer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Created by zeyu.zyzhang on 2/16/25
 * @author zeyu.zyzhang@bytedance.com
 */
class MangaAppTriggerWakeLock: AppTrigger {
    override val triggerList: Set<String>
        get() = MangaAppTriggerPageTurn.mangaAppList

    override val tag: String = this::class.simpleName.toString()

    private var isActive = false

    override fun onActive() {
        isActive = true
        commonSettingScope.launch {
            delay(5000L)
            if (this@MangaAppTriggerWakeLock.isActive) ActionPerformer.tryAcquireWakeLockIndefinitely()
        }
    }

    override fun onInactive() {
        isActive = false
        ActionPerformer.releaseWakeLock()
    }
}