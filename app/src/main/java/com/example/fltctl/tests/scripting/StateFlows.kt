@file:JvmName("ScriptRoot")
@file:JvmMultifileClass

package com.example.fltctl.tests.scripting

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Created by zeyu.zyzhang on 6/4/25
 * @author zeyu.zyzhang@bytedance.com
 */

val stateFlowReplay = script(enableLocalLogging = false) {
    val started = SystemClock.elapsedRealtime()
    val stateFlow = MutableStateFlow(-1)
    val sharedFlow1 = MutableSharedFlow<Int>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val sharedFlow1StateIn = sharedFlow1.stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        -1
    )
    val emitJob = coroutineScope.launch(Dispatchers.IO) {
        repeat(3) {
            stateFlow.value = it+1
            delay(100)
            repeat(3) { it2 ->
                sharedFlow1.emit((it+1) * 100 + it2+1)
            }

            delay(100)

        }
    }
    coroutineScope.launch {
        stateFlow.collect {
            println("${SystemClock.elapsedRealtime() - started}: stateFlow: $it")
        }
    }
//    delay(200)
    coroutineScope.launch {
        sharedFlow1.collect {
            println("${SystemClock.elapsedRealtime() - started}: sharedFlow1: $it")
            delay(100)
        }
    }
    delay(200)
    coroutineScope.launch {
        sharedFlow1StateIn.collect {
            println("${SystemClock.elapsedRealtime() - started}: sharedFlow1StateIn: $it")
            log.i("actual get value $it")
            delay(300)
        }
    }
    emitJob.join()
}