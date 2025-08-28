@file:JvmName("ScriptRoot")
@file:JvmMultifileClass
package com.example.fltctl.tests.scripting

/**
 * Created by zeyu.zyzhang on 7/29/25
 * @author zeyu.zyzhang@bytedance.com
 */

val rangeCheckInSublist = script {
    val l = List(8) { it }
    println(l.subList(0, 5))
    println(l.subList(5, l.size))
    println(l.subList(0,8))
    println(l.subList(7,8))
}

val reduce = script {
    val l = List(8) { if(it == 5) true else false }
}