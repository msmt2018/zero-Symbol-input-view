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
import androidx.compose.foundation.gestures.draggable
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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostudio.idea.widget.editor.symbolinput.v2.R
import androidx.compose.ui.res.stringResource
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.ThemeMode
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.HapticMode
import kotlinx.coroutines.launch

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
    extensions: ComposeSymbolDrawerExtensions = ComposeSymbolDrawerExtensions(),
    extensionRegistry: ComposeSymbolDrawerExtensionRegistry? = null,
    onSymbolClick: (SymbolEntry) -> Unit,
    onSymbolLongClick: (SymbolEntry) -> Unit = {},
    onGroupChanged: (Int, SymbolGroup?) -> Unit = { _, _ -> },
    onOpenManager: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val registeredExtensions = extensionRegistry?.let { rememberComposeSymbolDrawerExtensions(it) } ?: extensions
    val density = LocalDensity.current
    val providerGroupsState = remember(registeredExtensions.groupProviders) {
        mutableStateOf<List<SymbolGroup>?>(null)
    }
    LaunchedEffect(groups, registeredExtensions.groupProviders) {
        val merged = LinkedHashMap<String, SymbolGroup>()
        // Explicit host data remains authoritative; providers fill missing IDs.
        groups.forEach { merged[it.id] = it }
        registeredExtensions.groupProviders.forEach { provider ->
            runCatching { provider.loadGroups() }.getOrNull()?.forEach { group ->
                merged.putIfAbsent(group.id, group)
            }
        }
        providerGroupsState.value = merged.values.toList()
    }
    val contentGroups = providerGroupsState.value ?: groups
    val hostColorScheme = MaterialTheme.colorScheme
    val systemDark = isSystemInDarkTheme()
    val modeColorScheme = remember(settings.themeMode, systemDark, hostColorScheme) {
        when (settings.themeMode) {
            ThemeMode.SYSTEM -> hostColorScheme
            ThemeMode.LIGHT -> lightColorScheme()
            ThemeMode.DARK -> darkColorScheme()
        }
    }
    // Explicit registered theme extensions take precedence over the built-in
    // mode so a host can provide its own complete design system.
    val resolvedColorScheme = registeredExtensions.themes.firstNotNullOfOrNull { extension ->
        runCatching { extension.resolve().colorScheme }.getOrNull()
    } ?: modeColorScheme
    val pagerState = rememberPagerState(
        initialPage = contentGroups.indexOfFirst { it.id == state.selectedGroupId }.coerceAtLeast(0),
        pageCount = { contentGroups.size.coerceAtLeast(1) }
    )
    val selectedGroup = contentGroups.getOrNull(pagerState.currentPage)
    val lifecycleExtensions = registeredExtensions.lifecycle

    DisposableEffect(lifecycleExtensions) {
        lifecycleExtensions.forEach { extension -> runCatching { extension.onAttached() } }
        onDispose {
            lifecycleExtensions.asReversed().forEach { extension ->
                runCatching { extension.onDetached() }
            }
        }
    }

    LaunchedEffect(settings, lifecycleExtensions) {
        lifecycleExtensions.forEach { extension ->
            runCatching { extension.onSettingsChanged(settings) }
        }
    }
    LaunchedEffect(contentGroups, lifecycleExtensions) {
        lifecycleExtensions.forEach { extension ->
            runCatching { extension.onGroupsChanged(contentGroups) }
        }
    }
    LaunchedEffect(selectedGroup?.id, pagerState.currentPage, lifecycleExtensions) {
        selectedGroup?.let { state.selectGroup(it.id) }
        onGroupChanged(pagerState.currentPage, selectedGroup)
        lifecycleExtensions.forEach { extension ->
            runCatching { extension.onGroupSelected(pagerState.currentPage, selectedGroup) }
        }
    }
    LaunchedEffect(state.expansionFraction, lifecycleExtensions) {
        lifecycleExtensions.forEach { extension ->
            runCatching { extension.onExpansionChanged(state.expansionFraction) }
        }
    }

    fun invokeSymbol(symbol: SymbolEntry, longPress: Boolean) {
        val shouldHaptic = settings.hapticMode == HapticMode.ALL ||
            (settings.hapticMode == HapticMode.SELECTION && !longPress)
        if (shouldHaptic) {
            hapticFeedback.performHapticFeedback(
                if (longPress) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
            )
        }
        val action = if (longPress) symbol.longAction ?: symbol.shortAction else symbol.shortAction
        val handled = registeredExtensions.actions.any { extension ->
            runCatching { extension.handler.onAction(action, symbol) }.getOrDefault(false)
        }
        if (!handled) {
            if (longPress) onSymbolLongClick(symbol) else onSymbolClick(symbol)
        }
        lifecycleExtensions.forEach { extension ->
            runCatching { extension.onSymbolInvoked(symbol, longPress) }
        }
    }
    LaunchedEffect(contentGroups.map { it.id }) {
        val retained = contentGroups.indexOfFirst { it.id == state.selectedGroupId }
        if (retained >= 0 && retained != pagerState.currentPage) pagerState.scrollToPage(retained)
        else if (pagerState.currentPage >= contentGroups.size && contentGroups.isNotEmpty()) pagerState.scrollToPage(contentGroups.lastIndex)
    }

    MaterialTheme(colorScheme = resolvedColorScheme) {
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
            val effectiveCellHeight = maxOf(cellHeight, settings.minimumTouchTargetDp.dp)
            val collapsedGridHeight = effectiveCellHeight * settings.collapsedRows
            val collapsedHeight = handleHeight + collapsedGridHeight
            val availableHeight = if (constraints.hasBoundedHeight) this.maxHeight else maxHeight
            val expandedHeight = availableHeight.coerceAtMost(maxHeight).coerceAtLeast(collapsedHeight)
            val totalHeight = collapsedHeight + (expandedHeight - collapsedHeight) * state.expansionFraction
            val visibleIndicatorHeight = indicatorHeight * state.expansionFraction
            val gridHeight = (totalHeight - handleHeight - visibleIndicatorHeight).coerceAtLeast(collapsedGridHeight)

            Column(Modifier.requiredHeight(totalHeight)) {
                Box(Modifier.fillMaxWidth().height(handleHeight)) {
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
                                    scope.launch { state.settle(velocity / range, settings.animationDurationMs) }
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
                    Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onOpenManager) { Text(stringResource(R.string.compose_action_manage)) }
                        TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.compose_action_settings)) }
                    }
                }

                if (contentGroups.isNotEmpty()) {
                    Box(Modifier.fillMaxWidth().height(visibleIndicatorHeight)) {
                        if (state.expansionFraction > 0.01f) {
                            val customIndicator = registeredExtensions.groupIndicators
                                .sortedWith(compareByDescending<SymbolDrawerGroupIndicator> { it.priority }.thenBy { it.id })
                                .firstOrNull()
                            if (customIndicator != null) {
                                customIndicator.Content(
                                    groups = contentGroups,
                                    selectedIndex = pagerState.currentPage,
                                    onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                BuiltInGroupIndicator(
                                    groups = contentGroups,
                                    selectedIndex = pagerState.currentPage,
                                    settings = settings,
                                    onSelect = { index ->
                                        if (index in contentGroups.indices) {
                                            scope.launch { pagerState.animateScrollToPage(index) }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth().height(gridHeight),
                        key = { page -> contentGroups.getOrNull(page)?.id ?: "empty-$page" }
                    ) { page ->
                        val group = contentGroups.getOrNull(page)
                        if (group != null) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(settings.columns),
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(settings.cellHorizontalSpacingDp.dp),
                                verticalArrangement = Arrangement.spacedBy(settings.cellVerticalSpacingDp.dp)
                            ) {
                                items(group.symbols, key = { it.id }) { symbol ->
                                    val renderer = registeredExtensions.itemRenderers
                                        .asSequence()
                                        .filter { it.accepts(symbol) }
                                        .sortedWith(compareByDescending<SymbolDrawerItemRenderer> { it.priority }.thenBy { it.id })
                                        .firstOrNull()
                                    if (renderer != null) {
                                        renderer.Content(
                                            symbol = symbol,
                                            selected = false,
                                            modifier = Modifier.fillMaxWidth().height(effectiveCellHeight),
                                            onClick = { invokeSymbol(symbol, false) },
                                            onLongClick = { invokeSymbol(symbol, true) }
                                        )
                                    } else {
                                        SymbolCell(
                                            symbol = symbol,
                                            settings = settings,
                                            onClick = { invokeSymbol(symbol, false) },
                                            onLongClick = { invokeSymbol(symbol, true) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(Modifier.fillMaxWidth().height(gridHeight), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.compose_no_groups), style = MaterialTheme.typography.bodyMedium)
                    }
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
            .height(maxOf(settings.cellHeightDp, settings.minimumTouchTargetDp).dp)
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
