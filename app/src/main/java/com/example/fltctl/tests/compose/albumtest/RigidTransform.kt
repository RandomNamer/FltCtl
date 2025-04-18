package com.example.fltctl.tests.compose.albumtest

import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * Created by zeyu.zyzhang on 4/15/25
 * @author zeyu.zyzhang@bytedance.com
 */
data class RigidTransform(
    val scale: Float,
    val pivot: Offset,
    val translation: Offset
) {
    companion object {
        fun getTransform(src: Rect, dst: Rect): RigidTransform {
            val scaleX = dst.width / src.width
            val scaleY = dst.height / src.height
            val scale = minOf(scaleX, scaleY)

            val pivot = Offset(
                src.left + src.width / 2f,
                src.top + src.height / 2f
            )

            val scaledWidth = src.width * scale
            val scaledHeight = src.height * scale
            val translation = Offset(
                dst.left + (dst.width - scaledWidth) / 2f - (src.left * scale - pivot.x * scale + pivot.x),
                dst.top + (dst.height - scaledHeight) / 2f - (src.top * scale - pivot.y * scale + pivot.y)
            )

            return RigidTransform(scale, pivot, translation)
        }

        fun applyTransform(src: Rect, transform: RigidTransform): Rect {
            val resultRect = RectF(src.left, src.top, src.right, src.bottom)
            transform.toMatrix().mapRect(resultRect)
            return androidx.compose.ui.geometry.Rect(
                left = resultRect.left,
                top = resultRect.top,
                right = resultRect.right,
                bottom = resultRect.bottom
            )
        }
    }

    fun toMatrix(): android.graphics.Matrix {
        val matrix = android.graphics.Matrix()
        matrix.postScale(scale, scale, pivot.x, pivot.y)
        matrix.postTranslate(translation.x, translation.y)
        return matrix
    }

    fun fromMatrix(mat: android.graphics.Matrix): RigidTransform {
        val flattened = FloatArray(9)
        mat.getValues(flattened)
        return RigidTransform(
            scale = flattened[0],
            pivot = Offset(flattened[2], flattened[5]),
            translation = Offset(flattened[4], flattened[5])
        )
    }

    fun inverse(): RigidTransform {
        val invMat = android.graphics.Matrix()
        toMatrix().invert(invMat)
        return fromMatrix(invMat)
    }

    fun applyRect(rect: Rect): Rect = applyTransform(rect, this)

}

fun Rect.getTransformTo(dst: Rect): RigidTransform = RigidTransform.getTransform(this, dst)

fun Rect.applyTransform(transform: RigidTransform) = transform.applyRect(this)

fun Rect.inverseTransform(transform: RigidTransform) = transform.inverse().applyRect(this)