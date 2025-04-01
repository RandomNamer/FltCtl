package com.example.fltctl.tests.compose.albumtest

import android.graphics.Bitmap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fltctl.tests.UiTest
import com.example.fltctl.ui.theme.FltCtlTheme
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Created by zeyu.zyzhang on 3/10/25
 * @author zeyu.zyzhang@bytedance.com
 * Album UI test with image selection and preview functionality
 */
val albumPreviewUiTest = UiTest.ComposeUiTest(
    title = "Album Preview Gestures",
    content = {
        FltCtlTheme {
            androidx.compose.material.Surface {
                Album()
            }
        }
    }
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BoxScope.Album() {
    val viewModel: MediaPickerViewModel = viewModel()
    val state = viewModel.state
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState { state.tabs.size } // Dynamic tab count based on tabs list
    var showImagePreview by remember { mutableStateOf(false) }
    var selectedPreviewImageIndex by remember { mutableStateOf(0) }
    
    // Sync tab selection with pager
    LaunchedEffect(pagerState.currentPage) {
        viewModel.onTabSelected(pagerState.currentPage)
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Tab layout
        TabRow(
            selectedTabIndex = state.currentTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            state.tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = state.currentTab == index,
                    onClick = { 
                        scope.launch { pagerState.animateScrollToPage(index) }
                    },
                    text = { Text(tab.title) }
                )
            }
        }
        
        // ViewPager with image grids
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            ImageGrid(
                images = state.listItems,
                isLoading = state.isLoading,
                onImageClick = { viewModel.toggleImageSelection(it) },
                isSelected = { viewModel.isImageSelected(it) }
            )
        }
        
        // Selected images row
        if (state.selectedImages.isNotEmpty()) {
            HorizontalDivider()
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Selected (${state.selectedImages.size}/${state.maxSelectCount})",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Clear",
                        modifier = Modifier.clickable { viewModel.clearSelection() },
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(80.dp)
                ) {
                    items(state.selectedImages) { image ->
                        val index = state.selectedImages.indexOf(image)
                        SelectedImageThumbnail(
                            image = image,
                            onRemove = { viewModel.toggleImageSelection(image) },
                            onClick = {
                                selectedPreviewImageIndex = index
                                showImagePreview = true
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
    
    // Multi-image preview with animation
    if (showImagePreview && state.selectedImages.isNotEmpty()) {
        Box(modifier = Modifier.fillMaxSize()) {
            MultiImagePreview(
                images = state.selectedImages,
                initialPage = selectedPreviewImageIndex,
                onDismiss = { showImagePreview = false }
            )
        }
    }
}

@Composable
fun ImageGrid(
    images: List<ImageSource>,
    isLoading: Boolean,
    onImageClick: (ImageSource) -> Unit,
    isSelected: (ImageSource) -> Boolean,
    viewModel: MediaPickerViewModel = viewModel()
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(images) { image ->
                    ImageItem(
                        image = image,
                        onClick = { onImageClick(image) },
                        isSelected = isSelected(image)
                    )
                }
                
                // Add loading indicator at the bottom when loading more
                item(span = { GridItemSpan(maxLineSpan) }) {
                    if (viewModel.state.isLoadingMore) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    } else if (viewModel.state.hasMoreItems) {
                        // Invisible spacer that triggers loading more when it becomes visible
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .onGloballyPositioned {
                                    viewModel.loadMore()
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImageItem(image: ImageSource, onClick: () -> Unit, isSelected: Boolean) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(image.id) {
        bitmap = image.provideThumbnail()
    }
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = image.description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } ?: Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        
        if (isSelected) {
            // Selection indicator
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
            
            Badge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun SelectedImageThumbnail(image: ImageSource, onRemove: () -> Unit, onClick: () -> Unit = {}) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(image.id) {
        bitmap = image.provideThumbnail()
    }
    
    Box(
        modifier = Modifier
            .size(70.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            .clickable { onClick() }
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = image.description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } ?: Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        
        // Remove button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "×",
                color = MaterialTheme.colorScheme.onError,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
enum class GestureState {
    IDLE,
    SCALING,
    SWIPING_TO_DISMISS,
    PAGING
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MultiImagePreview(images: List<ImageSource>, initialPage: Int, onDismiss: () -> Unit) {
    val pagerState = rememberPagerState(initialPage = initialPage) { images.size }
    var gestureState by remember { mutableStateOf(GestureState.IDLE) }
    var dismissProgress by remember { mutableFloatStateOf(0f) }
    var isDismissing by remember { mutableStateOf(false) }
    
    // Animation values
    val animatedScale by animateFloatAsState(
        targetValue = if (isDismissing) 0.5f else 1f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "scale"
    )
    
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isDismissing) 0f else 1f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "alpha"
    )
    
    // Trigger dismiss animation
    LaunchedEffect(isDismissing) {
        if (isDismissing) {
            // Wait for animation to complete before calling onDismiss
            kotlinx.coroutines.delay(300)
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = (1f - dismissProgress) * animatedAlpha))
            .graphicsLayer {
                // Apply scale animation when dismissing
                scaleX = animatedScale
                scaleY = animatedScale
            }
    ) {
        // Close button
        IconButton(
            onClick = { isDismissing = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(48.dp)
                .graphicsLayer { alpha = animatedAlpha }
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Image pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = gestureState == GestureState.IDLE || gestureState == GestureState.PAGING
        ) { page ->
            TransformableImage(
                image = images[page], 
                onGestureStateUpdate = { gestureState = it}, 
                onDismiss = { isDismissing = true },
                onDismissProgressChange = { dismissProgress = it }
            )
        }

        // Page indicator
        if (images.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .graphicsLayer { alpha = animatedAlpha },
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(images.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (isSelected) 1f else 0.5f))
                    )
                }
            }
        }
    }
}

@Composable
fun TransformableImage(
    image: ImageSource, 
    onGestureStateUpdate: (GestureState) -> Unit, 
    onDismiss: () -> Unit,
    onDismissProgressChange: (Float) -> Unit
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var dismissProgress by remember { mutableFloatStateOf(0f) }
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp.value
    val dismissThreshold = screenHeight * 0.2f // 20% of screen height for dismiss
    
    // Load full-size image
    LaunchedEffect(image.id) {
        bitmap = image.provideImage()
    }
    
    // Update parent with dismiss progress
    LaunchedEffect(dismissProgress) {
        onDismissProgressChange(dismissProgress.coerceIn(0f, 1f))
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, rotation ->
                    if (abs(zoom -1f) > 0.01f) {
                        // Handle scaling
                        onGestureStateUpdate(GestureState.SCALING)
                        scale *= zoom
                        scale = scale.coerceIn(1f, 3f)
                        offset += pan
                        return@detectTransformGestures
                    }
                    
                    if (abs(pan.getDistance()) > 10f) {
                        if (abs(pan.y) > abs(pan.x)) {
                            // Handle vertical swipe for dismissal
                            onGestureStateUpdate(GestureState.SWIPING_TO_DISMISS)
                            offset += pan
                            
                            // Calculate dismiss progress based on vertical drag
                            dismissProgress = (offset.y / screenHeight).coerceIn(0f, 1f)
                            
                            // Trigger dismiss if threshold exceeded
                            if (offset.y > dismissThreshold) {
                                onDismiss()
                            }
                            return@detectTransformGestures
                        } else {
                            // Handle horizontal swipe for paging
                            onGestureStateUpdate(GestureState.PAGING)
                            return@detectTransformGestures
                        }
                    }
                }
            }
    ) {
        // Image with transformations
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = image.description,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Apply scale and translation
                        scaleX = scale * (1f - dismissProgress * 0.5f) // Scale down as we drag
                        scaleY = scale * (1f - dismissProgress * 0.5f)
                        translationX = offset.x
                        translationY = offset.y
                        alpha = 1f - dismissProgress * 0.7f // Fade out as we drag
                    }
            )
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
    }
}