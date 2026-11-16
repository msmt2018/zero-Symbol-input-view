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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Material 3 Compose implementation of the V2 symbol drawer.
 *
 * Vertical expansion is handled by the drag handle; HorizontalPager exclusively
 * owns group paging. The indicator is revealed continuously as the drawer opens.
 */
/**
 * Converts the configured page cache budget into Compose Pager's symmetric
 * beyond-viewport page count. The configured budget includes the visible page.
 */
internal fun pageBeyondViewportCount(settings: SymbolDrawerSettings): Int {
    if (!settings.prefetchAdjacentGroups || settings.pageCacheLimit <= 1) return 0
    return ((settings.pageCacheLimit - 1) / 2).coerceAtMost(4)
}

/** Converts the configured fling speed (dp/s) to the drawer's normalized range/s. */
internal fun drawerFlingVelocityThreshold(
    settings: SymbolDrawerSettings,
    expandedRangeDp: Float
): Float = settings.flingVelocityDpPerSecond.toFloat() /
    expandedRangeDp.coerceAtLeast(1f)

/** Reduced-motion mode disables settling animation without disabling dragging. */
internal fun drawerAnimationDurationMs(settings: SymbolDrawerSettings): Int =
    if (settings.reduceMotion) 0 else settings.animationDurationMs

/**
 * Pixel consumption helpers for the drawer's nested-scroll contract.
 * Upward pre-scroll expands the drawer; downward post-scroll collapses it only
 * after the child scrollable has reached its own boundary.
 */
internal fun drawerNestedPreScrollConsumption(
    availableY: Float,
    expansionFraction: Float,
    rangePx: Float
): Float {
    if (!availableY.isFinite() || !expansionFraction.isFinite() ||
        !rangePx.isFinite() || rangePx <= 0f || availableY >= 0f
    ) return 0f
    val fraction = expansionFraction.coerceIn(0f, 1f)
    val expandablePx = (1f - fraction) * rangePx
    return -minOf(-availableY, expandablePx)
}

internal fun drawerNestedPostScrollConsumption(
    availableY: Float,
    expansionFraction: Float,
    rangePx: Float
): Float {
    if (!availableY.isFinite() || !expansionFraction.isFinite() ||
        !rangePx.isFinite() || rangePx <= 0f || availableY <= 0f
    ) return 0f
    val fraction = expansionFraction.coerceIn(0f, 1f)
    val collapsiblePx = fraction * rangePx
    return minOf(availableY, collapsiblePx)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SymbolDrawer(
    groups: List<SymbolGroup>,
    state: SymbolDrawerState = remember { SymbolDrawerState(false, null) },
    modifier: Modifier = Modifier,
    settings: SymbolDrawerSettings = SymbolDrawerSettings(),
    maxHeight: Dp = settings.maxExpandedHeightDp.dp,
    extensions: ComposeSymbolDrawerExtensions = ComposeSymbolDrawerExtensions(),
    extensionRegistry: ComposeSymbolDrawerExtensionRegistry? = null,
    onSymbolClick: (SymbolEntry) -> Unit,
    onSymbolLongClick: (SymbolEntry) -> Unit = {},
    onGroupChanged: (Int, SymbolGroup?) -> Unit = { _, _ -> },
    onOpenManager: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    detentState: SymbolDrawerDetentState? = null
) {
    val scope = rememberCoroutineScope()
    // Animatable.snapTo cancels an in-flight mutation. Serialize pointer deltas
    // and settling so high-frequency callbacks cannot cancel one another.
    val drawerMutationMutex = remember { Mutex() }
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
            val providedGroups = try {
                provider.loadGroups()
            } catch (cancelled: CancellationException) {
                // Never turn a cancelled composition/effect into a successful load.
                throw cancelled
            } catch (_: Throwable) {
                // One optional provider must not prevent other providers or host data.
                emptyList()
            }
            providedGroups.forEach { group ->
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
    val expansionFraction = detentState?.expansionFraction ?: state.expansionFraction
    val selectedGroupId = detentState?.selectedGroupId ?: state.selectedGroupId
    val pagerState = rememberPagerState(
        initialPage = contentGroups.indexOfFirst { it.id == selectedGroupId }.coerceAtLeast(0),
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
        selectedGroup?.let {
            state.selectGroup(it.id)
            detentState?.selectGroup(it.id)
        }
        onGroupChanged(pagerState.currentPage, selectedGroup)
        lifecycleExtensions.forEach { extension ->
            runCatching { extension.onGroupSelected(pagerState.currentPage, selectedGroup) }
        }
    }
    LaunchedEffect(expansionFraction, lifecycleExtensions) {
        lifecycleExtensions.forEach { extension ->
            runCatching { extension.onExpansionChanged(expansionFraction) }
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
    LaunchedEffect(contentGroups.map { it.id }, selectedGroupId) {
        val retained = contentGroups.indexOfFirst { it.id == selectedGroupId }
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
            // Respect bounded parent constraints even when the configured
            // collapsed grid cannot fit in the available viewport.
            val expandedHeight = availableHeight.coerceAtMost(maxHeight).coerceAtLeast(0.dp)
            val effectiveCollapsedHeight = collapsedHeight.coerceAtMost(expandedHeight)
            val effectiveHandleHeight = handleHeight.coerceAtMost(effectiveCollapsedHeight)
            val dragRange = (expandedHeight - effectiveCollapsedHeight).coerceAtLeast(0.dp)
            val totalHeight = effectiveCollapsedHeight + dragRange * expansionFraction
            val visibleIndicatorHeight = indicatorHeight * expansionFraction
            val gridHeight = (totalHeight - effectiveHandleHeight - visibleIndicatorHeight).coerceAtLeast(0.dp)
            val rangePx = with(density) { dragRange.toPx() }
            val nestedScrollCoordinator = remember(detentState, state) {
                DrawerNestedScrollCoordinator(expansionFraction, rangePx)
            }
            // One worker mirrors projected deltas into state; pointer events never spawn an unbounded job queue.
            val mutationDrainJob = remember(nestedScrollCoordinator) { arrayOfNulls<Job>(1) }
            fun enqueueDrawerMutation(deltaFraction: Float) {
                val leaseId = nestedScrollCoordinator.enqueueMutation(deltaFraction) ?: return
                mutationDrainJob[0] = scope.launch {
                    try {
                        while (true) {
                            val nextDelta = nestedScrollCoordinator.takePendingMutation(leaseId) ?: break
                            drawerMutationMutex.withLock {
                                if (detentState != null) detentState.dragBy(nextDelta)
                                else state.dragBy(nextDelta)
                            }
                        }
                    } finally {
                        // Cancellation/failure must not strand the coordinator's
                        // lease. The generation check prevents an old worker from
                        // releasing a lease acquired by a newer worker.
                        nestedScrollCoordinator.releaseMutationDrain(leaseId)
                    }
                }
            }
            // Keep normalized projection across viewport/range changes, including
            // while accepted deltas are waiting to be applied to Animatable.
            SideEffect { nestedScrollCoordinator.updateRange(rangePx) }
            val nestedScrollConnection = remember(detentState, state, rangePx, settings, nestedScrollCoordinator) {
                object : NestedScrollConnection {
                    override fun onPreScroll(
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        val consumption = nestedScrollCoordinator.consumePreScroll(available.y)
                        enqueueDrawerMutation(consumption.deltaFraction)
                        return Offset(0f, consumption.consumedY)
                    }

                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        val consumption = nestedScrollCoordinator.consumePostScroll(available.y)
                        enqueueDrawerMutation(consumption.deltaFraction)
                        return Offset(0f, consumption.consumedY)
                    }

                    override suspend fun onPostFling(
                        consumed: Velocity,
                        available: Velocity
                    ): Velocity {
                        val fraction = nestedScrollCoordinator.projectedFraction()
                        val canSettle = (available.y < 0f && fraction < 1f) ||
                            (available.y > 0f && fraction > 0f)
                        if (!canSettle || rangePx <= 0f) return Velocity.Zero

                        val rangeDp = with(density) { rangePx.toDp().value }
                        val velocityThreshold = drawerFlingVelocityThreshold(settings, rangeDp)
                        val duration = drawerAnimationDurationMs(settings)
                        mutationDrainJob[0]?.join()
                        drawerMutationMutex.withLock {
                            if (detentState != null) {
                                detentState.settle(
                                    velocityFraction = -available.y / rangePx,
                                    velocityThreshold = velocityThreshold,
                                    animationDurationMillis = duration
                                )
                                nestedScrollCoordinator.reconcile(detentState.expansionFraction)
                            } else {
                                state.settle(
                                    velocityFraction = available.y / rangePx,
                                    animationDurationMillis = duration,
                                    velocityThreshold = velocityThreshold
                                )
                                nestedScrollCoordinator.reconcile(state.expansionFraction)
                            }
                        }
                        return Velocity(0f, available.y)
                    }
                }
            }

            Column(
                Modifier
                    .requiredHeight(totalHeight)
                    .nestedScroll(nestedScrollConnection)
            ) {
                Box(Modifier.fillMaxWidth().height(effectiveHandleHeight)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(effectiveHandleHeight)
                            .draggable(
                                orientation = Orientation.Vertical,
                                state = rememberDraggableState { delta ->
                                    val deltaFraction = nestedScrollCoordinator.consumeDragDelta(delta)
                                    enqueueDrawerMutation(deltaFraction)
                                },
                                onDragStopped = { velocity ->
                                    val range = with(density) { dragRange.toPx().coerceAtLeast(1f) }
                                    val rangeDp = with(density) { range.toDp().value }
                                    val velocityThreshold = drawerFlingVelocityThreshold(settings, rangeDp)
                                    val animationDuration = drawerAnimationDurationMs(settings)
                                    scope.launch {
                                        mutationDrainJob[0]?.join()
                                        drawerMutationMutex.withLock {
                                            if (detentState != null) {
                                                detentState.settle(
                                                    velocityFraction = -velocity / range,
                                                    velocityThreshold = velocityThreshold,
                                                    animationDurationMillis = animationDuration
                                                )
                                                nestedScrollCoordinator.reconcile(detentState.expansionFraction)
                                            } else {
                                                state.settle(
                                                    velocityFraction = velocity / range,
                                                    animationDurationMillis = animationDuration,
                                                    velocityThreshold = velocityThreshold
                                                )
                                                nestedScrollCoordinator.reconcile(state.expansionFraction)
                                            }
                                        }
                                    }
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
                        if (expansionFraction > 0.01f) {
                            // Registry snapshots are already priority/ID ordered.
                            val customIndicator = registeredExtensions.groupIndicators.firstOrNull()
                            if (customIndicator != null) {
                                customIndicator.Content(
                                    groups = contentGroups,
                                    selectedIndex = pagerState.currentPage,
                                    onSelect = { index ->
                                        if (index in contentGroups.indices) {
                                            scope.launch { pagerState.animateScrollToPage(index) }
                                        }
                                    },
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
                        beyondViewportPageCount = pageBeyondViewportCount(settings),
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
                                    // The immutable registry snapshot is already ordered; avoid
                                    // allocating and sorting a sequence for every visible symbol.
                                    val renderer = registeredExtensions.itemRenderers
                                        .firstOrNull { it.accepts(symbol) }
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

/**
 * Remembers a saveable multi-anchor drawer state. Keep detent IDs stable when
 * changing anchor fractions so restoration can resolve the intended position.
 */
@Composable
fun rememberSymbolDrawerDetentState(
    detents: List<SymbolDrawerDetent> = SymbolDrawerDetent.Defaults,
    initialDetentId: String = SymbolDrawerDetent.Collapsed.id,
    selectedGroupId: String? = null
): SymbolDrawerDetentState = rememberSaveable(
    detents,
    saver = SymbolDrawerDetentState.saver(detents)
) {
    SymbolDrawerDetentState(
        detents = detents,
        initialDetentId = initialDetentId,
        selectedGroupId = selectedGroupId
    )
}
