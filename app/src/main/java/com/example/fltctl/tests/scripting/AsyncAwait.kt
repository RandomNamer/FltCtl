@file:JvmName("ScriptRoot")
@file:JvmMultifileClass
package com.example.fltctl.tests.scripting

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Created by zeyu.zyzhang on 5/29/25
 * @author zeyu.zyzhang@bytedance.com
 */

suspend infix fun CoroutineScope.await(deferred: Deferred<Any>) {
    deferred.await()
}

val awaitIdiom = script {
    val d1 = CompletableDeferred<String>()
    coroutineScope.launch {
        delay(120)
        d1.complete("Hello")
    }
    val d2 = coroutineScope.async(start = CoroutineStart.ATOMIC) {
        delay(150)
        "World"
    }
    println(d1.await())
    d2.cancel()
    println(d2.await())

}