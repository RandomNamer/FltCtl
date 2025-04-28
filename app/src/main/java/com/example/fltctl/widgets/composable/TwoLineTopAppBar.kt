package com.example.fltctl.widgets.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * Created by zeyu.zyzhang on 4/21/25
 * @author zeyu.zyzhang@bytedance.com
 */

@Composable
@ExperimentalMaterial3Api
fun TwoLineTopAppBar(
    title: @Composable () -> Unit,
    subTitle: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit) = {},
    actions: @Composable (RowScope.() -> Unit) = {},
    collapsedHeight: Dp = TopAppBarDefaults. LargeAppBarCollapsedHeight,
    expandedHeight: Dp = TopAppBarDefaults. LargeAppBarExpandedHeight,
    windowInsets: WindowInsets = TopAppBarDefaults. windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.largeTopAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
) {
    LargeTopAppBar(
        title = {
            Column {
                title()
                AnimatedVisibility(scrollBehavior.state.collapsedFraction < 0.5) {
                    ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                        subTitle()
                    }
                }
            }
        },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        collapsedHeight = collapsedHeight,
        expandedHeight = expandedHeight,
        windowInsets = windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior
    )
}