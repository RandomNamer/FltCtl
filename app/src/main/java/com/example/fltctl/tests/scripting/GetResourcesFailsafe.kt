@file:JvmName("ScriptRoot")
@file:JvmMultifileClass
package com.example.fltctl.tests.scripting

/**
 * Created by zeyu.zyzhang on 8/21/25
 * @author zeyu.zyzhang@bytedance.com
 */

val getResourcesCrash = script(enableErrorCatching = true) {
    val resources = androidContext.resources
    try {
        val invalidString = resources.getString(-1)
    } catch (e: Exception) {
        println(e.toString())
    }
    resources.getString(-1)
}