@file:JvmName("ScriptRoot")
@file:JvmMultifileClass

package com.example.fltctl.tests.scripting

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

/**
 * Created by zeyu.zyzhang on 5/22/25
 * @author zeyu.zyzhang@bytedance.com
 */

data class ContentReuseResponse(
    @SerializedName("status_code")
    val statusCode: Int?,

    @SerializedName("status_msg")
    val statusMsg: String?,

    @SerializedName("allow_reuse_of_content")
    val allowReuseOfContent: ContentReusePermission?,
)

enum class ContentReusePermission {
    EVERY_ONE,
    FRIENDS,
    NO_ONE,
}

val constructorDefaults = script {
    val jsonString = "{\"status_code\":0,\"status_msg\":\"\",\"allow_reuse_of_content\":1}"
    val gson = Gson()
    var response = gson.fromJson(jsonString, ContentReuseResponse::class.java)
    println(response)
    val gsonWithTA = GsonBuilder()
        .registerTypeAdapter(ContentReusePermission::class.java, object: TypeAdapter<ContentReusePermission>() {
            override fun write(out: JsonWriter?, value: ContentReusePermission?) {
                out?.value(value?.ordinal)
            }

            override fun read(`in`: JsonReader?): ContentReusePermission? {
                `in`?.let {
                    val ordinal = `in`.nextInt()
                    return ContentReusePermission.values()[ordinal]
                }
                return null
            }

        }).create()
    response = gsonWithTA.fromJson(jsonString, ContentReuseResponse::class.java)
    println(response)
}

private data class PrimitiveTypeContainer (
    @SerializedName("float")
    val float: Float? = null,
    @SerializedName("double")
    val double: Double? = null,
    @SerializedName("int")
    val int: Int? = null,
    @SerializedName("long")
    val long: Long? = null,
    @SerializedName("short")
    val short: Short? = null,
    @SerializedName("byte")
    val byte: Byte? = null,
    @SerializedName("char")
    val char: Char? = null,
    @SerializedName("boolean")
    val boolean: Boolean? = null
)

val nanDeserialization = script {
    val gson = Gson()
    val target = PrimitiveTypeContainer(float = Float.NaN)
    val jsonString = runCatching { gson.toJson(target) }.toString()
    println(jsonString)
    val gsonWithNaN = GsonBuilder().serializeSpecialFloatingPointValues().create()
    val jsonStringWithNaN = gsonWithNaN.toJson(target)
    println(jsonStringWithNaN)
    val result = gsonWithNaN.fromJson(jsonStringWithNaN, PrimitiveTypeContainer::class.java)
    println(result)
}