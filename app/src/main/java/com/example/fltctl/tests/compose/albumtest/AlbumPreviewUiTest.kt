package com.example.fltctl.tests.compose.albumtest

import android.graphics.Bitmap
import android.view.ViewConfiguration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fltctl.tests.UiTest
import com.example.fltctl.ui.theme.FltCtlTheme
import com.example.fltctl.utils.androidLogs
import com.example.fltctl.widgets.composable.EInkCompatCard
import com.example.fltctl.widgets.composable.simplyScrollable
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * Created by zeyu.zyzhang on 3/10/25
 * @author zeyu.zyzhang@bytedance.com
 * Album UI test with image selection and preview functionality
 */
val albumPreviewUiTest = UiTest.ComposeUiTest(
    title = "Album Preview Gestures",
    content = {
        Album()
    }
).also {
//    AppMonitor.addStartupTestPage(it)
}

internal val log by androidLogs("AlbumTest")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BoxScope.Album() {
    log.enable(true)
    val viewModel: MediaPickerViewModel = viewModel()
    val state = viewModel.state
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState { state.tabs.size } // Dynamic tab count based on tabs list
    var showImagePreview by remember { mutableStateOf(false) }
    var currentPreviewThumbRect by remember { mutableStateOf(Rect.Zero) }

//    var selectedPreviewImageIndex by remember { mutableStateOf(0) }

//    val view = LocalView.current
//
//    LaunchedEffect(Unit) {
//        view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
//    }
    
    // Sync tab selection with pager
    LaunchedEffect(pagerState.currentPage) {
        viewModel.onTabSelected(pagerState.currentPage)
    }

    LaunchedEffect(showImagePreview) {
        if (!showImagePreview) viewModel.onPreviewPageChange(-1 )
    }
    
    FltCtlTheme(darkTheme = false, fontScale = 0.8f) {
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
                beyondViewportPageCount = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                ImageGrid(
                    images = state.listItems,
                    isLoading = state.isLoading,
                    errorMsg = state.error,
                    onImageClick = { viewModel.toggleImageSelection(it) },
                    isSelected = { viewModel.isImageSelected(it) }
                )
            }

            // Selected images row
            if (state.selectedImages.isNotEmpty()) {
                SelectedThumbnailRow(
                    selectedImages = state.selectedImages,
                    maxSelectCount = state.maxSelectCount,
                    clearSelection = viewModel::clearSelection,
                    toggleImageSelection = viewModel::toggleImageSelection,
                    onClickThumbnail = {
                        viewModel.onPreviewPageChange(it)
                        showImagePreview = true
                    },
                    previewIndex = state.currentPreviewImageIndex,
                    onPreviewImageBoundSet = {currentPreviewThumbRect = it}
                )
            }
        }

        // Multi-image preview with animation
        if (showImagePreview && state.selectedImages.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize()) {
                MultiImagePreview(
                    images = state.selectedImages,
                    initialPage = state.currentPreviewImageIndex,
                    onDismiss = { showImagePreview = false },
                    currentThumbRect = currentPreviewThumbRect
                )
            }
        }
    }
}

@Composable
fun ImageGrid(
    images: List<ImageSource>,
    isLoading: Boolean,
    errorMsg: String? = null,
    onImageClick: (ImageSource) -> Unit,
    isSelected: (ImageSource) -> Boolean,
    viewModel: MediaPickerViewModel = viewModel()
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (errorMsg != null) {
            EInkCompatCard(
                isInEInkMode = false,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            ) {
                Text(text = errorMsg, modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxSize()
                    .padding(16.dp)
                    .simplyScrollable())
            }
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
fun ImageItem(modifier: Modifier = Modifier, image: ImageSource, onClick: () -> Unit, isSelected: Boolean) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(Unit) {
        bitmap = image.provideThumbnail()
        if ((bitmap?.height ?: 0) > 1000)
            log.w("possible too large bitmap: ${bitmap!!.width} ${bitmap!!.height} ${bitmap!!.byteCount}")
    }
    
    Box(
        modifier = modifier
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
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun SelectedThumbnailRow(
    selectedImages: List<ImageSource>,
    maxSelectCount: Int,
    clearSelection: () -> Unit,
    toggleImageSelection: (ImageSource) -> Unit,
    onClickThumbnail: (Int) -> Unit,
    previewIndex: Int = -1,
    onPreviewImageBoundSet: (Rect) -> Unit = {}
) {
    val listState = rememberLazyListState()

    LaunchedEffect(previewIndex) {
        if (previewIndex >= 0 && previewIndex < selectedImages.count() ) {
            listState.scrollToRevealItem(previewIndex)
        }
    }

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
                text = "Selected (${selectedImages.size}/${maxSelectCount})",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Clear",
                modifier = Modifier.clickable { clearSelection() },
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium
            )
        }

        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(80.dp)
        ) {
            items(selectedImages) { image ->
                val index = selectedImages.indexOf(image)
                val isInPreview = index == previewIndex
                SelectedImageThumbnail(
                    image = image,
                    highlight = isInPreview,
                    onRemove = { toggleImageSelection(image) },
                    onClick = {
                        onClickThumbnail(index)
                    },
                    modifier = if (isInPreview) Modifier.onGloballyPositioned {
                        onPreviewImageBoundSet(it.boundsInWindow())
                    } else Modifier
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun SelectedImageThumbnail(image: ImageSource, modifier: Modifier = Modifier, highlight: Boolean = false, onRemove: () -> Unit, onClick: () -> Unit = {}) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(image.id) {
        bitmap = image.provideThumbnail()
    }

    val baseModifier = modifier
        .size(70.dp)
        .clip(RoundedCornerShape(4.dp))

    Box(
        modifier = (if (highlight) baseModifier.border(3.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(4.dp))
                    else baseModifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)))
            .clickable { onClick() }
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = image.description,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        } ?: Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        
        // Remove button
        Box(Modifier
            .size(24.dp)
            .padding(2.dp)
            .align(Alignment.TopEnd)
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable(onClick = onRemove),) {
            Icon(
                Icons.Filled.Close, null,
                Modifier,
                MaterialTheme.colorScheme.inverseOnSurface
            )
        }

    }
}



enum class GestureState {
    IDLE,
    SCALING,
    DRAGGING,
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MultiImagePreview(images: List<ImageSource>, initialPage: Int, currentThumbRect: Rect, onDismiss: () -> Unit) {
    val pagerState = rememberPagerState(initialPage = initialPage) { images.size }
    val currentImageInfo = remember { derivedStateOf { images[pagerState.currentPage] } }
    var gestureState by remember { mutableStateOf(GestureState.IDLE) }
    var dismissProgress by remember { mutableFloatStateOf(0f) }
    var isDismissing by remember { mutableStateOf(false) }

    val shouldShowUi by remember { derivedStateOf { gestureState == GestureState.IDLE } }

    val vm = viewModel<MediaPickerViewModel>()

    val scope = rememberCoroutineScope()

    
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isDismissing) 0f else 1f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "alpha"
    )
    

    LaunchedEffect(pagerState.currentPage) {
        vm.onPreviewPageChange(pagerState.currentPage)
    }

    // Background mask
    Box(Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = (1f - dismissProgress) * animatedAlpha)))
    // Image pager
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        pageSpacing = 12.dp,
        beyondViewportPageCount = 3,
        userScrollEnabled = gestureState == GestureState.IDLE
    ) { page ->
        TransformableImage(
            image = images[page],
            onGestureStateUpdate = { gestureState = it },
            onDismiss = { onDismiss.invoke() },
            onDismissProgressChange = { dismissProgress = it },
            srcRect = currentThumbRect,
            containerVisible = pagerState.currentPage == page
        )
    }
    // UIs
    if (shouldShowUi) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .alpha(animatedAlpha)
        ) {
            Row(Modifier
                .fillMaxWidth(0.8f)
                .align(Alignment.TopStart)) {

                IconButton(
                    onClick = {  },
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(16.dp)
                        .size(48.dp)
                        .graphicsLayer { alpha = animatedAlpha }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "${pagerState.currentPage}/${pagerState.pageCount}: ${currentImageInfo.value.description}",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }

            // Page indicator
            if (images.size > 1) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
//                        .padding(bottom = 16.dp)
                ) {
                    SelectedThumbnailRow(
                        selectedImages = images,
                        maxSelectCount = -1,
                        clearSelection = vm::clearSelection,
                        toggleImageSelection = vm::toggleImageSelection,
                        onClickThumbnail = { idx ->
                            if (idx != pagerState.currentPage && idx < pagerState.pageCount)
                                scope.launch {
                                    pagerState.animateScrollToPage(idx)
                                }
                        },
                        previewIndex = pagerState.currentPage,
                    )

                }
            }
        }
    }
}

object PreviewPageDefaults {
    const val duration = 250
    fun <T> tween() = tween<T>(
        durationMillis = duration,
        easing = FastOutSlowInEasing
    )
    fun <T> spring() = androidx.compose.animation.core.spring<T>(stiffness = Spring.StiffnessMedium)

    const val scaleLimitMin = 0.7f
    const val scaleLimitMax = 10f
    const val doubleTapScale = 3f

    fun dismissThreshold(containerHeight: Float) = containerHeight * 0.2f // 20% of screen height for dismiss
}

private class BoundsStateHolder(
    var imageBoundsInParent: Rect = Rect.Zero,
    var originalImageBoundsInParent: Rect = Rect.Zero,
    var originalImageBoundsInWindow: Rect = Rect.Zero,
    var containerBounds: Rect = Rect.Zero
) {
    init {
        log.d("BoundsStateHolder init")
    }
    override fun toString(): String {
        return "BoundsStateHolder(imageBoundsInParent=$imageBoundsInParent, originalImageBoundsInParent=$originalImageBoundsInParent, imageBoundsInWindow=$originalImageBoundsInWindow, containerBounds=$containerBounds)"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransformableImage(
    image: ImageSource, 
    onGestureStateUpdate: (GestureState) -> Unit, 
    onDismiss: () -> Unit,
    onDismissProgressChange: (Float) -> Unit,
    srcRect: Rect = Rect.Zero,
    containerVisible: Boolean
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scale = remember { Animatable(1f) }
//    var pivot = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var translation = remember { Animatable(Offset.Zero, Offset.VectorConverter)}
    var dismissProgress = remember { Animatable(0f) }

    val boundsHolder = remember { BoundsStateHolder() }

    val scope = rememberCoroutineScope()

    val viewConfiguration: android.view.ViewConfiguration = ViewConfiguration.get(LocalContext.current)

    val animatedClipRect = remember { Animatable(Rect(0f, 0f, 1f, 1f), Rect.VectorConverter) }

    var enableClip by remember { mutableStateOf(false) }

    val srcBounds by rememberUpdatedState<Rect>(srcRect)


    // Load full-size image
    LaunchedEffect(image.id) {
        bitmap = image.provideImage()
    }

    LaunchedEffect(dismissProgress.value) {
        onDismissProgressChange(dismissProgress.value)
    }

    fun isInZoom(): Boolean = scale.value > 1f

    fun animateImageTransformTo(targetScale: Float, targetTranslation: Offset, onEnd: () -> Unit = {}) {
        scope.launch {
            launch { scale.animateTo(targetScale, PreviewPageDefaults.tween()) }
            launch { translation.animateTo(targetTranslation, PreviewPageDefaults.tween()); onEnd.invoke() }
        }
    }

    fun animatePackedTransformTo(packed: RigidTransform) {
        val (scale, _, translation) = packed
        animateImageTransformTo(scale, translation)
    }

    fun startDismiss() {
        log.d("Will dismiss $boundsHolder")
        boundsHolder.takeIf { it.originalImageBoundsInWindow != Rect.Zero }?.let { it.originalImageBoundsInWindow }?.let {
            val scale = it.fit(image.size).let {
                srcBounds.width / minOf(it.width, it.height)
            }
            val finalClipRect = it.fit(image.size).fit(Size(1f, 1f))
            val (_, _, translation) = it.getTransformTo(srcBounds, scale)
            enableClip = true
            scope.launch {
                animatedClipRect.snapTo(Rect(0f, 0f, 1f, 1f))
                launch { dismissProgress.animateTo(1f, PreviewPageDefaults.tween()) }
                launch {
                    animatedClipRect.animateTo(Rect(
                        Offset(
                            x = (finalClipRect.left - it.left) / it.width,
                            y = (finalClipRect.top - it.top) / it.height
                        ),
                        Size(
                            width = finalClipRect.width / it.width,
                            height = finalClipRect.height / it.height
                        )

                    ))
                }
            }
            animateImageTransformTo(scale, translation) {
                onDismiss.invoke()
                enableClip = false
            }
        }
    }

    BackHandler(
        enabled = containerVisible,
        onBack = {
            startDismiss()
        }
    )

    fun reset() {
        scope.launch {
            scale.snapTo(1f)
            translation.snapTo(Offset.Zero)
        }
    }

    fun onDragOrFling(dragChange: Offset) = scope.launch {
        val imageBounds = boundsHolder.imageBoundsInParent
        val containerBounds = boundsHolder.containerBounds
        if (isInZoom()) {
            val validOffset = calculateDragAmountForZoom(imageBounds, containerBounds, dragChange)
            log.d("drag: $dragChange -> $validOffset. final image bound=${imageBounds}, screenBound=$containerBounds")
            translation.snapTo(translation.value + validOffset)
        } else {
            translation.snapTo(translation.value + dragChange)
            //no zoom
            dismissProgress.snapTo((translation.value.y.absoluteValue / containerBounds.height).coerceIn(0f, 1f))
            scale.snapTo(1f - dismissProgress.value * 0.5f)
        }
    }

    var flingWhenZoomJob: Job? = remember { null }



    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                with(it) {
                    boundsHolder.containerBounds = Rect(Offset.Zero, size.toSize())
                }
            }
            .pointerInput(Unit) {
//                detectVerticalDragGestures { change, dragAmount ->  }

                detectZoomAndDrag(
                    detectVerticalOnly = { !isInZoom() },
                    onZoomGesture = { centroid, pan, zoom ->
                        // centroid is always from last frame
                        log.d("double pointer zoom: raw value: $zoom")
                        val imageBounds = boundsHolder.imageBoundsInParent
                        val containerBounds = boundsHolder.containerBounds
                        scope.launch {
                            val targetScale = (scale.value * zoom).coerceIn(
                                PreviewPageDefaults.scaleLimitMin,
                                PreviewPageDefaults.scaleLimitMax
                            )
                            val pivot = centroid.pivotIn(containerBounds.fit(image.size))
                            val scaleFactor = targetScale / scale.value
                            val translationDelta = Offset(
                                x = (1f - scaleFactor) * (pivot.x - imageBounds.left) + pan.x,
                                y = (1f - scaleFactor) * (pivot.y - imageBounds.top) + pan.y
                            )
                            scale.snapTo(targetScale)
                            translation.snapTo(translation.value + translationDelta)
                        }
                    },
                    onZoomStart = {
                        scope.coroutineContext.cancelChildren()
                        onGestureStateUpdate(GestureState.SCALING)
                    },
                    onZoomEnd = {
                        if (scale.value < 1f) {
                            onGestureStateUpdate(GestureState.IDLE)
                            animateImageTransformTo(1f, Offset.Zero)
                        } else if (scale.value > 1f) {
                            val imageBounds = boundsHolder.imageBoundsInParent
                            val containerBounds = boundsHolder.containerBounds
                            scope.launch {
                                val translationFix = calculateTranslationFix(imageBounds.fit(image.size), containerBounds)
                                log.d("zoom end fix: $translationFix, input: ${imageBounds.fit(image.size)} $containerBounds")
                                translation.animateTo(translation.value + translationFix, PreviewPageDefaults.tween())
                            }
                        }
                    },
                    onDrag = { point, dragChange ->
                        onDragOrFling(dragChange)
                    },
                    onDragStart = {
                        scope.coroutineContext.cancelChildren()
                        if (scale.value == 1f) {
                            onGestureStateUpdate(GestureState.DRAGGING)
                        }
                    },
                    onDragEnd = { v ->
                        val containerBounds = boundsHolder.containerBounds
                        if (!isInZoom()) {
                            if (translation.value.y > PreviewPageDefaults.dismissThreshold(containerBounds.height)) {
                                startDismiss()
                            } else {
                                onGestureStateUpdate(GestureState.IDLE)
                                log.d("Not dismissing")
                                animateImageTransformTo(
                                    targetScale = 1f,
                                    targetTranslation = Offset.Zero
                                )
                                scope.launch {
                                    dismissProgress.animateTo(0f, PreviewPageDefaults.spring())
                                }
                            }
                        } else {
                            // Android default fling
                            val decay = splineBasedDecay<Offset>(this@pointerInput)
                            var prevValue = Offset.Zero
                            if (v.total > viewConfiguration.scaledMinimumFlingVelocity && v.total < viewConfiguration.scaledMaximumFlingVelocity) {
                                scope.launch {
                                    AnimationState(Offset.VectorConverter, Offset.Zero, Offset(v.x, v.y)).animateDecay(decay) {
                                        val delta = value - prevValue
                                        prevValue = value
//                                    log.d("emit fling $delta")
                                        onDragOrFling(delta)
                                    }
                                }
                            }
                        }
                    }
                )

            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { point ->
                    scope.coroutineContext.cancelChildren()
                    val scaleUp = scale.value < PreviewPageDefaults.doubleTapScale
                    if (scaleUp) {
                        val imageBounds = boundsHolder.imageBoundsInParent
                        val originalRect = boundsHolder.originalImageBoundsInParent
                        val containerBounds = boundsHolder.containerBounds
                        val scaleFactor = PreviewPageDefaults.doubleTapScale / scale.value
                        val translationAfterScale = -Offset(
                            x = (scaleFactor - 1) * (point.x - imageBounds.left),
                            y = (scaleFactor - 1) * (point.y - imageBounds.top)
                        )
                        RigidTransform(
                            scale = PreviewPageDefaults.doubleTapScale,
                            pivot = originalRect.topLeft,
                            translation = translationAfterScale
                        ).let {
                            val dstRectBeforeFix = originalRect.applyTransform(it)
                            val translationFix = calculateTranslationFix(dstRectBeforeFix.fit(image.size), containerBounds)

                            log.d("double tap translation $translationAfterScale fix $translationFix， , input: ${dstRectBeforeFix.fit(image.size)} $containerBounds")

                            animatePackedTransformTo(
                                it.copy(
                                    translation = translationAfterScale + translationFix
                                )
                            )
                        }
                    } else {
                        animateImageTransformTo(
                            targetScale = 1f,
                            targetTranslation = Offset.Zero
                        )
                    }

                    onGestureStateUpdate(if (scaleUp) GestureState.SCALING else GestureState.IDLE)
                })
            }

    ) {
        // Image with transformations
        bitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = image.description,
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned {
                        boundsHolder.originalImageBoundsInParent = it.boundsInParent()
                        boundsHolder.originalImageBoundsInWindow = it.boundsInWindow()
                    }
                    .graphicsLayer {
                        log.d("${scale.value}, ${translation.value}, $enableClip, ${animatedClipRect.value}")
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = scale.value
                        scaleY = scale.value
                        translationX = translation.value.x
                        translationY = translation.value.y
                        clip = enableClip
                        shape = RelativeRectCutShape(animatedClipRect.value)
                    }
                    .onGloballyPositioned { coord ->
                        with(coord) {
//                            if (center == Offset.Unspecified) center = Offset(size.width / 2f, size.height / 2f)
//                            imageBounds = originalImageBounds.applyTransform(RigidTransform(
//                                scale = scale.value,
//                                pivot = originalImageBounds.topLeft,
//                                translation = translation.value
//                            ))
                            boundsHolder.imageBoundsInParent = coord.boundsInParent()
                            log.i("after: $boundsHolder")
                        }
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

class RelativeRectCutShape(private val fracRect: Rect): Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Rectangle(Rect(
            Offset(
                x = fracRect.left * size.width,
                y = fracRect.top * size.height
            ),
            Size(
                width = fracRect.width * size.width,
                height = fracRect.height * size.height
            )
        ))
    }

}
