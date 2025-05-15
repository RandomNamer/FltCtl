@file:JvmName("ScriptRoot")
@file:JvmMultifileClass
package com.example.fltctl.tests.scripting


/**
 * Created by zeyu.zyzhang on 5/14/25
 * @author zeyu.zyzhang@bytedance.com
 */

val a = script(
    name = "ConcurrentModification",
    enableErrorCatching = false
) {
    val m = mutableMapOf(
        "ads" to 1,
        "dsads" to 1323
    )
    m.forEach {
        if (it.value == 1) m.remove(it.key)
        println(it.key)
    }
}