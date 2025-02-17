package com.example.fltctl.controls.service.appaware

import com.example.fltctl.controls.service.ActionPerformer

/**
 * Created by zeyu.zyzhang on 2/16/25
 * @author zeyu.zyzhang@bytedance.com
 */
class MangaAppTriggerWakeLock: AppTrigger {
    override val triggerList: List<String>
        get() = MangaAppTriggerPageTurn.mangaAppList

    override val tag: String = this::class.simpleName.toString()

    override fun onActive() {
        ActionPerformer.tryAcquireWakeLockIndefinitely()
    }

    override fun onInactive() {
        ActionPerformer.releaseWakeLock()
    }
}