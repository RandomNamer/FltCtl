@file:JvmName("ScriptRoot")
@file:JvmMultifileClass
@file:Keep

package com.example.fltctl.tests.scripting

import androidx.annotation.Keep


/**
 * Sample scripts to demonstrate the scripting functionality
 */


// Simple hello world script
val helloWorldScript = script(
    name = "Hello World",
    enableErrorCatching = true
) {
    println("Hello from Android Script!")
    log.i("Hello World script executed")
    toast("this is a toast")
}

// Script with error handling demonstration
val errorDemoScript = script(
    name = "Error Demo",
    enableErrorCatching = true
) {
    println("Starting error demo script")
    println("This will work fine")
    log.i("About to throw an exception")

    // This will throw an exception that will be caught by the runner
    throw RuntimeException("This is a demonstration error")

    // This code will not be executed
    println("This line will not be printed")
}

// Script with a loop to demonstrate longer running scripts
val loopDemoScript = script(
    name = "Loop Demo",
    enableErrorCatching = true
) {
    println("Starting loop demo")
    for (i in 1..5) {
        println("Loop iteration $i")
        log.i("Loop iteration $i")
        // Simulate some work
        Thread.sleep(500)
    }
    println("Loop completed")
    kotlinx.coroutines.runBlocking { toast("Loop demo completed!") }
}

// Script with local logging enabled
val loggingDemoScript = script(
    name = "Logging Demo",
    enableErrorCatching = true,
    enableLocalLogging = true
) {
    log.v("This is a verbose log message")
    log.d("This is a debug log message")
    log.i("This is an info log message")
    log.w("This is a warning log message")
    log.e("This is an error log message")

    println("Check the logs for different log levels")
    kotlinx.coroutines.runBlocking { toast("Logging demo completed!") }
}