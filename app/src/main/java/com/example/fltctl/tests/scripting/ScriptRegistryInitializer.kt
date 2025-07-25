@file:JvmName("ScriptRoot")
@file:JvmMultifileClass
package com.example.fltctl.tests.scripting

import com.example.fltctl.utils.logs
import java.lang.reflect.Modifier

/**
 * Created by zeyu.zyzhang on 5/14/25
 * @author zeyu.zyzhang@bytedance.com
 */

//Add code from other roots here, and remember keep

private val scriptRoots = listOf(
  "com.example.fltctl.tests.scripting.ScriptRoot"
)

private val log by logs("ScriptRegistryInitializer")


fun initializeScriptRoots() {
    scriptRoots.forEach {
        try {
            val clz = Class.forName(it)
            for (method in clz.declaredMethods) {
                try {
                    if (Modifier.isStatic(method.modifiers)) {
                        val returnType = method.returnType
                        if (returnType == AndroidScriptConfig::class.java) {
                            if (!method.isAccessible) method.isAccessible = true
                            log.v("Initializing script method ${method.name}")
                            val obj = method.invoke(null) as? AndroidScriptConfig
                            obj?.runCatching {
                                if (title.isEmpty() || title == UNNAMED) {
                                    val assignedName = method.name.split("get").last().replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
                                    val titleBackingField = javaClass.getDeclaredField("title")
                                    titleBackingField.isAccessible = true
                                    titleBackingField.set(this@runCatching, assignedName)
                                    log.v(" setting default title: $assignedName")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}


