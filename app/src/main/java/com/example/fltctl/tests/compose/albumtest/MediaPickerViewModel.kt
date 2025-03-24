package com.example.fltctl.tests.compose.albumtest

import androidx.lifecycle.ViewModel

/**
 * Created by zeyu.zyzhang on 3/19/25
 * @author zeyu.zyzhang@bytedance.com
 */

data class MediaPickerState(
    val listItems: List<ImageSource>,
    val type: ImageRepository.Type = ImageRepository.Type.COLOR,
    val singleSelect: Boolean = true,
    val selectedImages: List<ImageSource> = emptyList()
)

class MediaPickerViewModel: ViewModel() {
    private lateinit var repo: ImageRepository

}