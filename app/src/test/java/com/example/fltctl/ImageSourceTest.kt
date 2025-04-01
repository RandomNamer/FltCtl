package com.example.fltctl

import android.graphics.Bitmap
import com.example.fltctl.tests.compose.albumtest.BlurHashImageRepo
import com.example.fltctl.tests.compose.albumtest.ColorImageSource
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * Created by zeyu.zyzhang on 3/24/25
 * @author zeyu.zyzhang@bytedance.com
 */

class ImageSourceTest {


    @Test
    fun testBlurHashBitmapGen() {
        val repo = BlurHashImageRepo()
        val thumb: Bitmap
        val image: Bitmap
        runBlocking {
            val all = repo.load(0, 5)
            thumb = all[0].provideThumbnail()
            image = all[0].provideImage()
        }
        assertEquals(1,1)
    }

    fun testColorBitmapGen() {
        val image = ColorImageSource.createSources(1)[0]
        val thumb: Bitmap
        val image2: Bitmap
        runBlocking {
            thumb = image.provideThumbnail()
            image2 = image.provideImage()
        }
        assertEquals(1,1)
    }
}