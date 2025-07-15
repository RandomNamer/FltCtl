@file:JvmName("ScriptRoot")
@file:JvmMultifileClass

package com.example.fltctl.tests.scripting

import android.os.CancellationSignal
import com.example.fltctl.utils.logs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Created by zeyu.zyzhang on 7/2/25
 * @author zeyu.zyzhang@bytedance.com
 */

private val log by logs("CancellationTest")

val cancellableFeatureTest = script {
    val jobScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val job1 = jobScope.launch {
        val result = loadSomething { signal ->
            for (i: Int in 1..50) {
                if (signal.isCanceled) {
                    println("Job1 cancelled at $i")
                    return@loadSomething "Job1 Cancelled"
                }
                    println("[${Thread.currentThread().name}] Job1 compute checkpoint $i")
                Thread.sleep(100)
            }
            "Job1 Result"
        }
        println("job1 got result: $result")
    }

    val job2 = jobScope.launch {
        val result = loadSomething { signal ->
            for (i: Int in 1..50) {
                if (signal.isCanceled) {
                    println("Job2 cancelled at $i")
                    return@loadSomething "Job2 Cancelled"
                }
                    println("[${Thread.currentThread().name}] Job2 compute checkpoint $i")
                Thread.sleep(100)
            }
            "Job2 Result"
        }
        println("job2 got result: $result")
    }

    val job3 = jobScope.launch {
        val result = loadSomething { signal ->
            for (i: Int in 1..15) {
                if (signal.isCanceled) {
                    println("Job3 cancelled at $i")
                    return@loadSomething "Job3 Cancelled"
                }
                println("[${Thread.currentThread().name}] Job3 compute checkpoint $i")
                Thread.sleep(100)
            }
            "Job3 Result"
        }
        println("job3 got result: $result")
    }

    delay(1000)

    // Cancel job1 first
    println("Cancelling job1...")
    job1.cancel()

    delay(300)

    // Cancel the entire scope (which cancels job2)
    println("Cancelling entire scope...")
    jobScope.cancel()

    // Wait for cleanup
    delay(500)
    println("Demo completed")
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun <T> loadSomething(block: (CancellationSignal) -> T) = suspendCancellableCoroutine<T> {
    val signal = CancellationSignal()
    it.invokeOnCancellation { signal.cancel() }
    val result = block(signal)
    it.resume(result)
}