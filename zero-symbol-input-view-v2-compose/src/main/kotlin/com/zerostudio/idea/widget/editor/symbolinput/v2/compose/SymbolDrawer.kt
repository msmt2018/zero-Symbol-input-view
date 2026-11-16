/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 * Author: android_zero · https://github.com/msmt2018
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.draggable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

/**
 * Material 3 Compose implementation of the V2 symbol drawer.
 *
 * Vertical expansion is handled by the drag handle; HorizontalPager exclusively
 * owns group paging. The indicator is revealed continuously as the drawer opens.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SymbolDrawer(
    groups: List<SymbolGroup>,
    state: SymbolDrawerState,
    modifier: Modifier = Modifier,
    settings: SymbolDrawerSettings = SymbolDrawerSettings(),
    maxHeight: Dp = settings.maxExpandedHeightDp.dp,
    onSymbolClick: (SymbolEntry) -> Unit,
    onSymbolLongClick: (SymbolEntry) -> Unit = {},
    onGroupChanged: (Int, SymbolGroup?) -> Unit = { _, _ -> },
    onOpenManager: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val pagerState = rememberPagerState(
        initialPage = groups.indexOfFirst { it.id == state.selectedGroupId }.coerceAtLeast(0),
        pageCount = { groups.size.coerceAtLeast(1) }
    )
    val selectedGroup = groups.getOrNull(pagerState.currentPage)
    LaunchedEffect(selectedGroup?.id) {
        selectedGroup?.let { state.selectGroup(it.id) }
        onGroupChanged(pagerState.currentPage, selectedGroup)
    }
    LaunchedEffect(groups.map { it.id }) {
        val retained = groups.indexOfFirst { it.id == state.selectedGroupId }
        if (retained >= 0 && retained != pagerState.currentPage) pagerState.scrollToPage(retained)
        else if (pagerState.currentPage >= groups.size && groups.isNotEmpty()) pagerState.scrollToPage(groups.lastIndex)
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val handleHeight = 24.dp
            val indicatorHeight = settings.indicatorHeightDp.dp
            val cellHeight = settings.cellHeightDp.dp
            val collapsedGridHeight = cellHeight * settings.collapsedRows
            val collapsedHeight = handleHeight + collapsedGridHeight
            val boundedMax = if (constraints.hasBoundedHeight) maxHeight else maxHeight.coerceAtLeast(maxHeight)
            val expandedHeight = min(maxHeight.value.takeIf { constraints.hasBoundedHeight } ?: maxHeight.value, maxHeight.value)
                .let { if (it.isFinite() && it > collapsedHeight.value) it.dp else maxHeight }
                .coerceAtLeast(collapsedHeight)
                .let { min(it, maxHeight.takeIf { constraints.hasBoundedHeight } ?: maxHeight) }
            val totalHeight = collapsedHeight + (expandedHeight - collapsedHeight) * state.expansionFraction
            val visibleIndicatorHeight = indicatorHeight * state.expansionFraction
            val gridHeight = (totalHeight - handleHeight - visibleIndicatorHeight).coerceAtLeast(collapsedGridHeight)

            Column(Modifier.requiredHeight(totalHeight)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(handleHeight)
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta ->
                                val range = with(density) { (expandedHeight - collapsedHeight).toPx().coerceAtLeast(1f) }
                                scope.launch { state.dragBy(-delta / range) }
                            },
                            onDragStopped = { velocity ->
                                val range = with(density) { (expandedHeight - collapsedHeight).toPx().coerceAtLeast(1f) }
                                scope.launch { state.settle(-velocity / range) }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f))
                    )
                }

                if (groups.isNotEmpty()) {
                    Box(Modifier.fillMaxWidth().height(visibleIndicatorHeight)) {
                        if (state.expansionFraction > 0.01f) {
                            ScrollableTabRow(
                                selectedTabIndex = pagerState.currentPage.coerceIn(0, groups.lastIndex),
                                edgePadding = 8.dp,
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ) {
                                groups.forEachIndexed { index, group ->
                                    Tab(
                                        selected = pagerState.currentPage == index,
                                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                        text = {
                                            Text(
                                                text = group.title,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Clip
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth().height(gridHeight),
                        key = { page -> groups.getOrNull(page)?.id ?: "empty-$page" }
                    ) { page ->
                        val group = groups.getOrNull(page)
                        if (group != null) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(settings.columns),
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(settings.cellHorizontalSpacingDp.dp),
                                verticalArrangement = Arrangement.spacedBy(settings.cellVerticalSpacingDp.dp)
                            ) {
                                items(group.symbols, key = { it.id }) { symbol ->
                                    SymbolCell(
                                        symbol = symbol,
                                        settings = settings,
                                        onClick = { onSymbolClick(symbol) },
                                        onLongClick = { onSymbolLongClick(symbol) }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(Modifier.fillMaxWidth().height(gridHeight), contentAlignment = Alignment.Center) {
                        Text("No symbol groups", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SymbolCell(
    symbol: SymbolEntry,
    settings: SymbolDrawerSettings,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val label = if (settings.showSymbolLabels) symbol.label else symbol.icon.orEmpty()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(settings.cellHeightDp.dp)
            .combinedClickable(
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.small
    ) {
        Box(Modifier.padding(horizontal = 2.dp), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = settings.symbolLabelTextSizeSp.sp,
                textAlign = TextAlign.Center,
                maxLines = if (settings.showFullSymbolLabels) Int.MAX_VALUE else 1,
                softWrap = settings.showFullSymbolLabels,
                overflow = if (settings.showFullSymbolLabels) TextOverflow.Visible else TextOverflow.Clip
            )
        }
    }
}
