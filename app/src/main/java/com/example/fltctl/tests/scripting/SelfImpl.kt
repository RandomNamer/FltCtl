@file:JvmName("ScriptRoot")
@file:JvmMultifileClass

package com.example.fltctl.tests.scripting

/**
 * Created by zeyu.zyzhang on 6/4/25
 * @author zeyu.zyzhang@bytedance.com
 */

interface Api

abstract class Component<T: Api> {
    abstract val apiComponent: T
    fun AndroidScriptRunnerScope.some() {
        println("from component: ${apiComponent::class.java.simpleName}")
    }
}
// // not compiling
//public class SomeSelfImpl: SomeSelfImpl.SomeApi, Component<SomeSelfImpl.SomeApi>() {
//    override val apiComponent: SomeApi
//        get() = this
//
//    public interface SomeApi: Api
//}
//
//val selfImpl = script {
//    val component = SomeSelfImpl()
//    with(component) {
//        some()
//    }
//}
