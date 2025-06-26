@file:JvmName("ScriptRoot")
@file:JvmMultifileClass

package com.example.fltctl.tests.scripting

/**
 * Created by zeyu.zyzhang on 6/18/25
 * @author zeyu.zyzhang@bytedance.com
 */

@JvmInline
value class VC(
    val a: Int,
)

//@JvmInline
//value class MultiVC(
//    val a: Int,
//    val b: String,
//    val c: VC,
//    val d: List<VC>,
//    val e: Map<String, VC>,
//    val f: Set<VC>,
//    val g: VC?,
//)

data class DC(
    val a: Int,
)

data class MultiDC(
    val a: Int,
    val b: String,
    val c: VC,
    val d: List<VC>,
    val e: Map<String, VC>,
    val f: Set<VC>,
    val g: VC?,
)

val valueVsDataViaReflection = script {
    val vc = VC(114514)
    val dc = DC(114514)
    val vcMethodsDta = vc.javaClass.declaredMethods
    val dcMethodsDta = dc.javaClass.declaredMethods
    val vcFieldsDta = vc.javaClass.declaredFields
    val dcFieldsDta = dc.javaClass.declaredFields
    println(vcMethodsDta.map { it.name  })
    println(dcMethodsDta.map { it.name  })
    println(vcFieldsDta.map { it.name  })
    println(dcFieldsDta.map { it.name  })

    val multiDC = MultiDC(
        114514,
        "114514",
        vc,
        listOf(vc),
        mapOf("114514" to vc),
        setOf(vc),
        vc,
    )
    println(multiDC.javaClass.declaredFields.map { it.name  })
    println(multiDC.javaClass.declaredMethods.map { it.name  })
}