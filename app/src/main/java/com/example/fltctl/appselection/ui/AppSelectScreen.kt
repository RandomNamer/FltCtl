package com.example.fltctl.appselection.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fltctl.R
import com.example.fltctl.appselection.model.openAppSettingsPage
import com.example.fltctl.appselection.model.startActivityByName
import com.example.fltctl.widgets.composable.BackdropScaffold
import com.example.fltctl.widgets.composable.DualStateListDialog
import com.example.fltctl.widgets.composable.borderBottom


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppSelectScreen(vm: AppSelectPageVM, onSelectEnd: () -> Unit, onBackPressedDelegate: () -> Unit) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val selectionList by remember { derivedStateOf { state.appList } }
    var searchBarExpanded by remember { mutableStateOf(false) }
    var searchInput by remember { mutableStateOf("") }
    val fastSearch = vm.fastSearch
    val ctx = LocalContext.current

    if (state.activityDialog.show) {
        val (_, currentAppInfo, itemList, exportedCount) = state.activityDialog
        val activityCount = state.activityDialog.list.size
        DualStateListDialog(
            items = itemList,
            title = stringResource(R.string.app_selection_select_activity),
            onItemSelected = {
                //Always from enabled item (exported)
               it ?: return@DualStateListDialog
                ctx.startActivityByName(currentAppInfo.packageName, it.className)
            },
            mainAction = stringResource(R.string.app_selection_goto_settings) to {
                ctx.openAppSettingsPage(currentAppInfo.packageName)
            },
            footer = {
                Box(Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.app_selection_activity_counts, exportedCount, activityCount),
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            },
            onDismiss = {
                vm.onDismissActivityListDialog()
            }
        )

    }
    BackdropScaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (!searchBarExpanded) Text(
                        stringResource(id = R.string.app_selection_pageTitle),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    if (!searchBarExpanded) IconButton(onClick = onBackPressedDelegate) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    AnimatedVisibility(
                        visible = searchBarExpanded,
                        enter = fadeIn() + slideIn { IntOffset(x = it.width, y = 0) },
                        exit = fadeOut() + slideOut { IntOffset(x = it.width, y = 0) }
                    ) {
                        OutlinedTextField(
                            value = searchInput,
                            onValueChange = {
                                searchInput = it
                                if (fastSearch) vm.onSearchFilterChange(searchInput)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .borderBottom(
                                    width = 0.5.dp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    padding = (-1145).dp
                                )
                                .padding(start = 8.dp)
                                .wrapContentHeight(),
                            textStyle = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            leadingIcon = {
                                Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = {
                                    searchBarExpanded = false
                                    vm.onSearchFilterChange(null)
                                }) {
                                    Icon(Icons.Rounded.Close, null)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                errorBorderColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    vm.onSearchFilterChange(searchInput)
                                }
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                        )
                    }
                    AnimatedVisibility(
                        visible = !searchBarExpanded,
                        enter = fadeIn(initialAlpha = 0.5f),
                        exit = fadeOut()
                    ) {
                        IconButton(onClick = {
                            searchBarExpanded = true
                        }) {
                            Icon(Icons.Rounded.Search, null)
                        }
                    }
                },
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                   Text(
                       text = stringResource(R.string.app_selection_currentlySelected, state.counts.selected, state.counts.filtered),
                       style = MaterialTheme.typography.bodyMedium,
                       maxLines = 2,
                       modifier = Modifier
                           .fillMaxWidth(0.6f)
                           .padding(start = 10.dp)
                   )
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = onSelectEnd,
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                    ) {
                        Row {
                            Icon(Icons.Filled.Send, null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(id = R.string.app_selection_endAction),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            )
        },
        expandInitially = false,
        enableGesture = true,
        backLayerContent = { pv ->
            Box(modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(pv)) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                ) {
                    DefaultFilterChip(
                        selected = state.filter.includeSystem,
                        onClick = {
                            vm.onFilterChange(state.filter.copy(includeSystem = state.filter.includeSystem.not()))
                        },
                        labelText = stringResource(id = R.string.app_selection_filter_sys)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    DefaultFilterChip(
                        selected = !state.filter.includeNoExportedActivity,
                        onClick = { vm.onFilterChange(state.filter.copy(includeNoExportedActivity = state.filter.includeNoExportedActivity.not())) },
                        labelText = stringResource(id = R.string.app_selection_filter_exported)
                    )
                }
            }
        }
    ) {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                verticalArrangement = Arrangement.Top
            ) {
                items(count = selectionList.size) { idx ->
                    val item = selectionList[idx]
                    if (idx == 0) Spacer(modifier = Modifier.height(10.dp))
                    AppItemCardWithSelection(
                        info = item,
                        onSelect = { vm.onSelect(it.packageName) },
                        onUnselect = { vm.onUnselect(it.packageName)}
                    ) {
                        vm.onClickItemCard(item.value)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    labelText: String
) {
    FilterChip(
        selected = selected, onClick = onClick,
        label = {
            Text(text = labelText)
        },
        leadingIcon = { if (selected) { Icon(Icons.Filled.Done, null)  } else null }
    )
}