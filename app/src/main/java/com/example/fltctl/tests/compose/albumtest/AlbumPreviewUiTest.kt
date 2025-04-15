package com.example.fltctl.tests.compose.albumtest

import android.graphics.Bitmap
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastSumBy
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fltctl.AppMonitor
import com.example.fltctl.tests.UiTest
import com.example.fltctl.ui.theme.FltCtlTheme
import com.example.fltctl.utils.androidLogs
import com.example.fltctl.widgets.view.takeDp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.absoluteValue

/**
 * Created by zeyu.zyzhang on 3/10/25
 * @author zeyu.zyzhang@bytedance.com
 * Album UI test with image selection and preview functionality
 */
val albumPreviewUiTest = UiTest.ComposeUiTest(
    title = "Album Preview Gestures",
    content = {
        FltCtlTheme {
            Surface {
                Album()
            }
        }
    }
).also {
    AppMonitor.addStartupTestPage(it)
}

private val log by androidLogs("AlbumTest")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BoxScope.Album() {
    val viewModel: MediaPickerViewModel = viewModel()
    val state = viewModel.state
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState { state.tabs.size } // Dynamic tab count based on tabs list
    var showImagePreview by remember { mutableStateOf(false) }
    var selectedPreviewImageIndex by remember { mutableStateOf(0) }

//    val view = LocalView.current
//
//    LaunchedEffect(Unit) {
//        view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
//    }
    
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
    DRAGGING,
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MultiImagePreview(images: List<ImageSource>, initialPage: Int, onDismiss: () -> Unit) {
    val pagerState = rememberPagerState(initialPage = initialPage) { images.size }
    val currentImageInfo = remember { derivedStateOf { images[pagerState.currentPage] } }
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
            delay(300)
            onDismiss()
        }
    }
    // UI
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
            onGestureStateUpdate = { gestureState = it},
            onDismiss = { isDismissing = true },
            onDismissProgressChange = { dismissProgress = it }
        )
    }
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
                onClick = { isDismissing = true },
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
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
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

object PreviewPageDefaults {
    val duration = 250
    fun <T> tween() = tween<T>(
        durationMillis = duration,
        easing = FastOutSlowInEasing
    )
    fun <T> spring() = androidx.compose.animation.core.spring<T>(stiffness = Spring.StiffnessMedium)

    val scaleLimitMin = 0.7f
    val scaleLimitMax = 10f
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransformableImage(
    image: ImageSource, 
    onGestureStateUpdate: (GestureState) -> Unit, 
    onDismiss: () -> Unit,
    onDismissProgressChange: (Float) -> Unit
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scale = remember { Animatable(1f) }
    var pivot = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var translation = remember { Animatable(Offset.Zero, Offset.VectorConverter)}
    var dismissProgress = remember { Animatable(0f) }
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp.value.takeDp()

    var center = remember { Offset.Unspecified }

    val scope = rememberCoroutineScope()

    val dismissThreshold = screenHeight * 0.2f // 20% of screen height for dismiss
    
    // Load full-size image
    LaunchedEffect(image.id) {
        bitmap = image.provideImage()
    }

    fun animateTransformTo(targetScale: Float, targetPivot: Offset, targetTranslation: Offset) {
        scope.launch {
            launch { pivot.animateTo(targetPivot, PreviewPageDefaults.tween()) }
            launch { scale.animateTo(targetScale, PreviewPageDefaults.tween()) }
            launch { translation.animateTo(targetTranslation, PreviewPageDefaults.tween()) }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectZoomAndVerticalDrag(
                    onZoomGesture = { centroid, pan, zoom ->
//                        log.d("double pointer zoom!")
                        if (abs(zoom - 1f) > 0.01f) {
                            scope.launch {
                                val scaleValue = scale.value * zoom
                                pivot.snapTo(centroid)
                                scale.snapTo(
                                    scaleValue.coerceIn(
                                        PreviewPageDefaults.scaleLimitMin,
                                        PreviewPageDefaults.scaleLimitMax
                                    )
                                )
                                translation.snapTo(translation.value + pan)
                            }
                        }
                    },
                    onZoomGestureEnd = {
                        if (scale.value < 1f) {
                            animateTransformTo(1f, center, Offset.Zero)
                        }
                        log.d("zoom ended")
                    },
                    onVerticalDrag = { point, dragChange ->
                        scope.launch {
                            translation.snapTo(translation.value + dragChange)
                            if (scale.value <= 1f) {
                                //no zoom
                                dismissProgress.snapTo(
                                    (translation.value.y.absoluteValue / screenHeight).coerceIn(0f, 1f)
                                )
                                scale.snapTo(1f - dismissProgress.value * 0.5f)
                            }
                        }

                    },
                    onVerticalDragStart = {
                        log.d("drag started")
                        if (scale.value == 1f) {
                            onGestureStateUpdate(GestureState.DRAGGING)
                        }
                    },
                    onVerticalDragEnd = {
                        log.d("drag ended")
                        onGestureStateUpdate(GestureState.IDLE)
                        if (translation.value.y > screenHeight * 0.2) {
                            onDismiss.invoke()
                            log.d("Will dismiss")
                        }
                    }
                )

            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { point ->
                    animateTransformTo(
                        targetPivot = point,
                        targetScale = if (scale.value < 3f) 3f else 1f,
                        targetTranslation = Offset.Zero
                    )
                })
            }
            .graphicsLayer {
//                log.d("${scale.value}, ${pivot.value}, ${translation.value}")
                if (center == Offset.Unspecified) center = Offset(size.width / 2, size.height / 2)
                scaleX = scale.value
                scaleY = scale.value
                transformOrigin = TransformOrigin(
                    pivotFractionX = pivot.value.x / size.width,
                    pivotFractionY = pivot.value.y / size.height
                )
                translationX = translation.value.x
                translationY = translation.value.y
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
            )
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
    }
}

/**
 * Enable only drag down to start
 */
suspend fun PointerInputScope.detectZoomAndVerticalDrag(
    onZoomGesture: (centroid: Offset, pan: Offset, zoom: Float) -> Unit,
    onZoomGestureEnd: () -> Unit = {},
    onVerticalDrag: (point: Offset, delta: Offset) -> Unit,
    onVerticalDragStart: () -> Unit = {},
    onVerticalDragEnd: () -> Unit = {}
) {
    awaitEachGesture {
        var zoom = 1f
        var pan = Offset.Zero
        var passedZoomTouchSlop = false
        var passedDragTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop

        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.fastAny { it.isConsumed }
            val touchCount = event.changes.fastSumBy { if (it.pressed) 1 else 0 }
            if (!canceled) {
                if (touchCount == 1) {
                    val dragChange = event.calculatePan()
                    if (!passedDragTouchSlop) {
                        if (dragChange.getDistance() > touchSlop && dragChange.y > dragChange.x.absoluteValue) {
                            passedDragTouchSlop = true
                            onVerticalDragStart()
                        }
                    }
                    if (passedDragTouchSlop) {
                        onVerticalDrag(event.changes[0].position, dragChange)
                        event.changes[0].consume()
                    }
                } else if (touchCount > 1) {
                    val zoomChange = event.calculateZoom()
                    val panChange = event.calculatePan()

                    if (!passedZoomTouchSlop) {
                        zoom *= zoomChange
                        pan += panChange

                        val centroidSize = event.calculateCentroidSize(useCurrent = false)
                        val zoomMotion = abs(1 - zoom) * centroidSize
                        val panMotion = pan.getDistance()

                        if (zoomMotion > touchSlop ||
                            panMotion > touchSlop
                        ) {
                            passedZoomTouchSlop = true
                        }
                    }

                    if (passedZoomTouchSlop) {
                        val centroid = event.calculateCentroid(useCurrent = false)
                        if (zoomChange != 1f ||
                            panChange != Offset.Zero
                        ) {
                            onZoomGesture(centroid, panChange, zoomChange)
                        }
                        event.changes.fastForEach {
                            if (it.positionChanged()) {
                                it.consume()
                            }
                        }
                    }
                }

            }
        } while (!canceled && touchCount > 0)

        if (passedDragTouchSlop) {
            onVerticalDragEnd()
        } else if (passedZoomTouchSlop) {
            onZoomGestureEnd()
        }

    }
}