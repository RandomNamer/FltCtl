package com.example.fltctl.tests.compose.albumtest

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * Created by zeyu.zyzhang on 3/19/25
 * @author zeyu.zyzhang@bytedance.com
 */

@Stable
data class MediaPickerState(
    val listItems: List<ImageSource> = emptyList(),
    val type: ImageRepository.Type = ImageRepository.Type.COLOR,
    val singleSelect: Boolean = false,
    val selectedImages: List<ImageSource> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val currentTab: Int = 0,
    val maxSelectCount: Int = 15,
    val tabs: List<UiTabItem> = emptyList(),
    val currentPage: Int = 0,
    val hasMoreItems: Boolean = true,
    val currentPreviewImageIndex: Int = -1,
)

data class TabItem(
    val text: String,
    val type: ImageRepository.Type,
    val repository: ImageRepository
) {
    fun toUiItem() = UiTabItem(title = text, type = type)
}

@Stable
data class UiTabItem(
    val title: String,
    val type: ImageRepository.Type,
)

class MediaPickerViewModel(): ViewModel() {
    private val tabs = listOf(
        TabItem(
            text = "System Album",
            type = ImageRepository.Type.SYSTEM_ALBUM,
            repository = ImageRepository.create(ImageRepository.Type.SYSTEM_ALBUM)
        ),
        TabItem(
            text = "Color Images",
            type = ImageRepository.Type.COLOR,
            repository = ImageRepository.create(ImageRepository.Type.COLOR)
        ),
        TabItem(
            text = "BlurHash Images",
            type = ImageRepository.Type.BLURHASH,
            repository = ImageRepository.create(ImageRepository.Type.BLURHASH)
        ),
        TabItem(
            text = "Checkerboard",
            type = ImageRepository.Type.CHECKERBORAD,
            repository = ImageRepository.create(ImageRepository.Type.CHECKERBORAD)
        ),
    )
    
    var state by mutableStateOf(MediaPickerState(tabs = tabs.map { it.toUiItem() }))
        private set
    
    init {
        loadImages(0)
    }
    
    private fun loadImages(tabIndex: Int) {
        val tab = tabs[tabIndex]
        viewModelScope.launch {
            state = state.copy(
                isLoading = true, 
                error = null, 
                type = tab.type, 
                currentTab = tabIndex,
                currentPage = 0,
                hasMoreItems = true
            )
            try {
                val pageSize = 30
                val images = tab.repository.load(0, pageSize)
                val totalCount = tab.repository.count()
                state = state.copy(
                    listItems = images, 
                    isLoading = false,
                    hasMoreItems = images.size < totalCount
                )
            } catch (e: Exception) {
                state = state.copy(error = e.stackTraceToString(), isLoading = false)
            }
        }
    }
    
    fun loadMore() {
        if (state.isLoading || state.isLoadingMore || !state.hasMoreItems) return
        
        val tab = tabs[state.currentTab]
        val nextPage = state.currentPage + 1
        val pageSize = 30
        
        viewModelScope.launch {
            state = state.copy(isLoadingMore = true)
            try {
                val currentItems = state.listItems
                val newItems = tab.repository.load(nextPage * pageSize, pageSize)
                val totalCount = tab.repository.count()
                
                if (newItems.isNotEmpty()) {
                    state = state.copy(
                        listItems = currentItems + newItems,
                        currentPage = nextPage,
                        isLoadingMore = false,
                        hasMoreItems = (currentItems.size + newItems.size) < totalCount
                    )
                } else {
                    state = state.copy(isLoadingMore = false, hasMoreItems = false)
                }
            } catch (e: Exception) {
                state = state.copy(error = e.stackTraceToString(), isLoadingMore = false)
            }
        }
    }
    
    fun onTabSelected(index: Int) {
        if (state.currentTab != index && index in tabs.indices) {
            loadImages(index)
        }
    }
    
    fun toggleImageSelection(image: ImageSource) {
        val currentSelected = state.selectedImages.toMutableList()
        
        if (currentSelected.contains(image)) {
            // Deselect the image
            currentSelected.remove(image)
        } else {
            // Select the image if under max limit
            if (currentSelected.size < state.maxSelectCount) {
                currentSelected.add(image)
            }
        }
        
        state = state.copy(selectedImages = currentSelected)
    }
    
    fun isImageSelected(image: ImageSource): Boolean {
        return state.selectedImages.contains(image)
    }
    
    fun clearSelection() {
        state = state.copy(selectedImages = emptyList())
    }

    fun onPreviewPageChange(index: Int) {
        state = state.copy(currentPreviewImageIndex = index)
    }
}