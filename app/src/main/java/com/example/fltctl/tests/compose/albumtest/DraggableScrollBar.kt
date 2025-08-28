package com.example.fltctl.tests.compose.albumtest

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fltctl.tests.UiTest
import com.example.fltctl.ui.theme.FltCtlTheme
import com.example.fltctl.widgets.composable.EInkCompatCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Created by zeyu.zyzhang on 7/7/25
 * @author zeyu.zyzhang@bytedance.com
 */

val draggableScrollBar = UiTest.ComposeUiTest(
    title = "Draggable Scrollbar",
    description = "Test the usage of draggable scrollbar.",
    content = {
        Screen()
    }
).also {
//    AppMonitor.addStartupTestPage(it)
}


private val ImageSource.isNothing: Boolean
    get() = width == -1 && height == -1

private class NothingImageSource(override val id: String) : ImageSource {

    companion object {
        private val sharedEmptyBitmap: Bitmap = createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }

    override val description: String
        get() = "Nothing"

    override suspend fun provideThumbnail(): Bitmap {
        return sharedEmptyBitmap
    }

    override suspend fun provideImage(): Bitmap {
        return sharedEmptyBitmap
    }

    override val width: Int
        get() = -1
    override val height: Int
        get() = -1
}

private class NothingImageRepo(private val count: Int): ImageRepository() {
    override suspend fun load(cursor: Int, count: Int): List<ImageSource> {
        return List(count) {
            NothingImageSource("${cursor + it}")
        }
    }

    override suspend fun count(): Int {
        return count
    }

}

private class ImageSourceWithGroupName(val real: ImageSource): ImageSource by real {
    val groupName: String
        get() = when (real) {
            is SystemAlbumImageSource -> real.dateString
            else -> real.id
        }
}

private data class PageUiState(
    val imageItems: List<ImageSourceWithGroupName>
)

private class PageViewModel(private val repo: ImageRepository): ViewModel() {

    private val _state = MutableStateFlow(PageUiState(emptyList()))

    val state: StateFlow<PageUiState> get() = _state

    val isNothingProvider: Boolean
        get() = repo is NothingImageRepo

    fun loadAll() {
        viewModelScope.launch {
            val count = repo.count()
            val images = repo.load(0, count)
            val imageItems = images.map { ImageSourceWithGroupName(it) }
            _state.value = _state.value.copy(imageItems = imageItems)
        }
    }
}

private class PageVMFactory(private val repo: ImageRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PageViewModel(repo) as T
    }
}

@Composable
private fun Screen() {
    val context = LocalContext.current
    val systemImageVM: PageViewModel = viewModel(key = "systemImageVM", factory = PageVMFactory(SystemAlbumImageRepo(context)))
    val nothingVM: PageViewModel = viewModel(key = "nothing", factory = PageVMFactory(NothingImageRepo(10000)))
    val pagerState = rememberPagerState { 2 }
    val selectedTabIndex by remember { derivedStateOf { pagerState.currentPage } }
    val scope = rememberCoroutineScope()



    FltCtlTheme(darkTheme = false, fontScale = 0.9f) {
        Column {
            TabRow(modifier = Modifier.fillMaxWidth(), selectedTabIndex = selectedTabIndex) {
                listOf("System Images", "Simple UI").forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(index)  }
                        },
                        modifier = Modifier
                         .weight(1f)
                         .padding(horizontal = 8.dp)
                         .wrapContentSize()
                    ) {
                        Text(text = title, modifier = Modifier.padding(vertical = 8.dp))
                    }

                }
            }
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                when (page) {
                    0 -> Page(vm = systemImageVM)
                    1 -> Page(vm = nothingVM)
                }
            }
        }

    }
}

private enum class ScrollMethod {
    BY,
    ITEM
}


@Composable
private fun Page(
    vm: PageViewModel
) {
    val state = vm.state.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()
    val useAbsolutelyMostSimpleItem = vm.isNothingProvider
    var scrollMethod by remember { mutableStateOf(ScrollMethod.ITEM) }


    LaunchedEffect(Unit) {
        vm.loadAll()
    }

    Column(Modifier.fillMaxSize()) {

        EInkCompatCard(modifier = Modifier.padding(16.dp)) { pv ->
            Column (modifier = Modifier
                .fillMaxWidth()
                .padding(pv)) {

                Text(
                    text = "Tweaks",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.size(8.dp))

                Row(modifier = Modifier.selectableGroup()) {
                    Text("Scroll method: ")
                    Row(modifier = Modifier
                        .padding(start = 8.dp)
                        .selectable(
                            selected = scrollMethod == ScrollMethod.ITEM,
                            onClick = {
                                scrollMethod = ScrollMethod.ITEM
                            }
                        )) {
                        RadioButton(selected = scrollMethod == ScrollMethod.ITEM, onClick = null)
                        Text("ToItem")
                    }
                    Row(modifier = Modifier
                        .padding(start = 8.dp)
                        .selectable(
                            selected = scrollMethod == ScrollMethod.BY,
                            onClick = {
                                scrollMethod = ScrollMethod.BY
                            }
                        )) {
                        RadioButton(selected = scrollMethod == ScrollMethod.BY, onClick = null)
                        Text("By")
                    }
                }



            }
        }

        ScrollBarOverlay(
            columnCount = 3,
            gridState = gridState,
            titleCallback = { index ->
                val imageItems = state.value.imageItems
                if (index >= imageItems.size) {
                    ""
                } else {
                    imageItems[index].groupName
                }
            },
            scrollMethod = scrollMethod,
        ) { modifier ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = gridState,
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                userScrollEnabled = false
            ) {
                items(items = state.value.imageItems, key = { it.id }) {
                    if (useAbsolutelyMostSimpleItem) {
                        Box(modifier = Modifier
                            .aspectRatio(1f)
                            .fillMaxSize()
                            .background(Color.DarkGray))
                    } else {
                        ImageItem(image = it.real, onClick = {}, isSelected = false)
                    }
                }
            }
        }
    }
}



@Composable
private fun ScrollBarOverlay(
    modifier: Modifier = Modifier,
    columnCount: Int,
    gridState: LazyGridState,
    titleCallback: (index: Int) -> String,
    scrollMethod: ScrollMethod,
    content: @Composable (Modifier) -> Unit,
) {
    var gripTitle by remember { mutableStateOf("Drag to start") }
    //Accumulated, [0, listheight]
    val dragPosition = remember { Animatable(0f) }
    val listHeight = remember { Ref<Int>() }
    val scope = rememberCoroutineScope()
    val scrollMethodLatest by rememberUpdatedState(scrollMethod)

    Layout(
        content = {
            content(Modifier.layoutId("content"))
            Text(
                text = gripTitle,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .layoutId("grip")
                    .wrapContentWidth()
                    .padding(end = 20.dp)
                    .background(Color.DarkGray, RoundedCornerShape(15.dp))
                    .shadow(2.dp, RoundedCornerShape(15.dp))
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, amount ->
                                val viewHeight = listHeight.value?.takeIf { it > 0 } ?: return@detectVerticalDragGestures
                                val itemHeight = gridState.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.height ?: return@detectVerticalDragGestures
                                var totalColumns = gridState.layoutInfo.totalItemsCount / columnCount
                                if (gridState.layoutInfo.totalItemsCount % columnCount > 0) {
                                    totalColumns += 1
                                }
                                val listTotalHeight = itemHeight * totalColumns
                                scope.launch {
                                    dragPosition.snapTo((dragPosition.value + amount).coerceIn(0f, viewHeight.toFloat()))
                                    val columnIndex = ((dragPosition.value / viewHeight.toFloat()) * totalColumns).toInt()
                                    val index = columnIndex * columnCount
                                    val toItemRemaining = ((dragPosition.value / viewHeight.toFloat()) * listTotalHeight) % itemHeight
                                    val title = titleCallback(index)
                                    gripTitle = title
                                    val dragAmountScaled = (amount / viewHeight.toFloat()) * listTotalHeight
                                    when (scrollMethodLatest) {
                                        ScrollMethod.ITEM -> gridState.scrollToItem(index, toItemRemaining.roundToInt())
                                        ScrollMethod.BY -> gridState.scrollBy(dragAmountScaled)
                                    }
                                    log.d("Dragging to $index by $scrollMethodLatest, title: $title yoffset: ${dragPosition.value} $toItemRemaining")
                                }

                            }
                        )
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                )
        },
        modifier = modifier,
        measurePolicy = object: MeasurePolicy {
            override fun MeasureScope.measure(
                measurables: List<Measurable>,
                constraints: Constraints
            ): MeasureResult {
                val content = measurables.first { it.layoutId == "content" }
                val grip = measurables.first { it.layoutId == "grip" }
                val contentPlaceable = content.measure(constraints)
                val gripPlaceable = grip.measure(constraints)
                listHeight.value = constraints.maxHeight
                return layout(constraints.maxWidth, constraints.maxHeight) {
                    contentPlaceable.place(0, 0)
                    gripPlaceable.place(constraints.maxWidth - gripPlaceable.width, dragPosition.value.roundToInt())
                }

            }

        }
    )
}