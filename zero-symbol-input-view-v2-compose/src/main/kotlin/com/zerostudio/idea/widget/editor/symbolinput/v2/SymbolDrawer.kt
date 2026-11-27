/*
 * This file is part of Zero Symbol Input View.
 *
 * Copyright (C) 2026 android_zero
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, version 3 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see:
 * https://www.gnu.org/licenses/agpl-3.0.html
 *
 * Project additional terms under AGPLv3 Section 7 are provided in
 * ADDITIONAL-TERMS.md in this module.
 *
 * Modified by android_zero on 2026-11-26: license notice updated.
 *
 * Author: android_zero
 * GitHub: https://github.com/msmt2018
 * WeChat: android_zero
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import com.zerostudio.idea.widget.editor.symbolinput.v2.compose.R
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DrawerConfig
import com.zerostudio.idea.widget.drawer.adapter.SymbolDrawerAnchorAdapter
import com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.AdaptiveGridResolver
import com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.WindowAdaptiveInfo
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.manager.SymbolDrawerUsageState
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
import com.zerostudio.idea.widget.editor.symbolinput.v2.ui.SymbolGroupIndicator
import com.zerostudio.idea.widget.editor.symbolinput.v2.ui.SymbolPage
import com.zerostudio.idea.widget.drawer.core.DrawerAnchorInput
import com.zerostudio.idea.widget.drawer.core.DrawerStateHolder
import com.zerostudio.idea.widget.drawer.core.rememberDrawerStateSnapshot
import com.zerostudio.idea.widget.drawer.core.ExistingHeightAnchorStrategy
import com.zerostudio.idea.widget.drawer.core.DrawerNestedScrollConnection

/**
 * Composition-owned state for the symbol drawer.
 *
 * The state contains only presentation values. It does not retain a View,
 * Context, Activity, Lifecycle, adapter, or host callback.
 */
@Stable
class SymbolDrawerState internal constructor(
    initialPage: Int,
    private val initiallyExpanded: Boolean
) {
    /** Zero-based selected group page. Mutations are normalized and owned by this state. */
    var selectedPage by mutableIntStateOf(initialPage.coerceAtLeast(0))
        internal set

    /** The same anchor engine used by the generic drawer container is authoritative. */
    internal val stateHolder = DrawerStateHolder(
        initialInput = DrawerAnchorInput(
            minHeightPx = 0,
            maxHeightPx = 0,
            viewportHeightPx = 0,
            contentHeightPx = 0,
            existingExpandedHeightPx = 0
        ),
        initialStrategy = ExistingHeightAnchorStrategy(),
        initialAnchorId = if (initiallyExpanded) "expanded" else "peek"
    )
    private var geometryInitialized = false
    internal val isGeometryInitialized: Boolean get() = geometryInitialized

    /**
     * Whether the current target anchor represents an expanded presentation.
     * During a drag without a target anchor, this is derived from the current height.
     */
    val expanded: Boolean
        get() {
            val anchors = stateHolder.anchors
            val minimum = anchors.minOf { it.heightPx }
            val maximum = anchors.maxOf { it.heightPx }
            val target = stateHolder.snapshot.targetAnchorId
                ?.let { id -> anchors.firstOrNull { it.id == id } }
            return target?.heightPx?.let { it > minimum } ?: run {
                stateHolder.currentHeightPx > minimum + (maximum - minimum) / 2
            }
        }

    /** Requests a transition to the maximum resolved anchor. */
    fun expand() { stateHolder.expandToMaxAnchor() }

    /** Requests a transition to the minimum resolved anchor. */
    fun collapse() { stateHolder.collapseToMinAnchor() }

    /** Toggles between the minimum and maximum resolved anchors. */
    fun toggle() { if (expanded) collapse() else expand() }

    /** Selects a zero-based group page; negative indices are normalized to zero. */
    fun selectPage(index: Int) { selectedPage = index.coerceAtLeast(0) }

    internal fun synchronizeGeometry(
        collapsedHeightPx: Int,
        expandedHeightPx: Int,
        density: Float
    ) {
        val maximum = expandedHeightPx.coerceAtLeast(collapsedHeightPx)
        stateHolder.updateGeometry(
            DrawerAnchorInput(
                minHeightPx = collapsedHeightPx.coerceAtMost(maximum),
                maxHeightPx = maximum,
                viewportHeightPx = maximum,
                contentHeightPx = maximum,
                existingExpandedHeightPx = maximum,
                density = density
            ),
            preserveExpansionFraction = geometryInitialized
        )
        if (!geometryInitialized) {
            val initial = if (initiallyExpanded) "expanded" else "peek"
            stateHolder.snapToAnchor(initial)
            geometryInitialized = true
        }
    }
}

/**
 * Creates state whose lifetime is the calling Composition.
 *
 * @param initialPage zero-based initial page; negative values are normalized to zero.
 * @param initiallyExpanded whether the first resolved drawer geometry starts expanded.
 * @return a state object remembered at this Composition call site.
 */
@Composable
fun rememberSymbolDrawerState(
    initialPage: Int = 0,
    initiallyExpanded: Boolean = false
): SymbolDrawerState = remember {
    SymbolDrawerState(initialPage, initiallyExpanded)
}

/**
 * Displays a native Jetpack Compose symbol drawer.
 *
 * Group paging, symbol layout, expansion gestures and state observation use
 * Compose Foundation and Material 3. The library does not create or embed a
 * traditional Android View. Data loading, persistence and navigation remain
 * responsibilities of the host application.
 *
 * @param groups ordered symbol-group snapshot; group and entry IDs should remain stable.
 * @param modifier modifier applied to the drawer's outer layout.
 * @param state Composition-owned drawer state; defaults to a remembered state.
 * @param settings immutable presentation/settings snapshot.
 * @param config resolved symbol-drawer configuration; defaults from settings.
 * @param onSymbolClick required callback for a short-clicked symbol.
 * @param onSymbolLongClick optional callback for a long-clicked symbol.
 * @param onOpenManager optional callback requesting manager navigation.
 * @param onOpenSettings optional callback requesting settings navigation.
 * @param usageState optional usage snapshot used for usage-aware presentation.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SymbolDrawer(
    groups: List<SymbolGroup>,
    modifier: Modifier = Modifier,
    state: SymbolDrawerState = rememberSymbolDrawerState(),
    settings: SymbolDrawerSettings = SymbolDrawerSettings(),
    config: DrawerConfig = settings.toDrawerConfig(),
    onSymbolClick: (SymbolEntry) -> Unit,
    onSymbolLongClick: ((SymbolEntry) -> Unit)? = null,
    onOpenManager: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    usageState: SymbolDrawerUsageState? = null
) {
    val safeGroups = groups
    val density = LocalDensity.current

    // Resolve every dimension from the constraints actually assigned by the
    // parent. This remains correct in landscape, split-screen and resizable
    // windows; it does not depend on physical display metrics.
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val safeWidthDp = if (maxWidth == androidx.compose.ui.unit.Dp.Infinity) {
            (config.columns * maxOf(36, settings.minimumTouchTargetDp)).toFloat()
        } else {
            maxWidth.value
        }
        val safeHeightDp = if (maxHeight == androidx.compose.ui.unit.Dp.Infinity) {
            config.maxExpandedHeightDp.toFloat()
        } else {
            maxHeight.value
        }
        val windowInfo = remember(safeWidthDp, safeHeightDp, density.density) {
            WindowAdaptiveInfo(
                widthDp = safeWidthDp,
                heightDp = safeHeightDp,
                density = density.density
            )
        }
        val gridSpec = remember(windowInfo, config, settings) {
            AdaptiveGridResolver.resolve(
                window = windowInfo,
                preferredColumns = config.columns,
                minimumCellWidthDp = maxOf(36f, settings.minimumTouchTargetDp.toFloat()),
                baseHorizontalSpacingDp = config.cellHorizontalSpacingDp,
                baseVerticalSpacingDp = config.cellVerticalSpacingDp,
                adaptive = true,
                lockColumnCount = !settings.showFullSymbolLabels
            )
        }
        val layoutConfig = remember(config, gridSpec) {
            config.copy(
                columns = gridSpec.columnCount,
                cellHorizontalSpacingDp = gridSpec.horizontalSpacingDp,
                cellVerticalSpacingDp = gridSpec.verticalSpacingDp
            )
        }

        // V2 anchor geometry remains the canonical symbol-drawer specification,
        // but is resolved only after adaptive grid columns have been selected.
        val anchors = remember(safeGroups, layoutConfig, density.density) {
            SymbolDrawerAnchorAdapter.resolve(
                groups = safeGroups,
                config = layoutConfig,
                density = density.density
            )
        }
        val viewportHeightPx = with(density) {
            if (maxHeight == androidx.compose.ui.unit.Dp.Infinity) {
                layoutConfig.maxExpandedHeightDp.dp.toPx()
            } else {
                maxHeight.toPx()
            }
        }.toInt().coerceAtLeast(1)
        val collapsedHeightPx = anchors.collapsedHeightPx.coerceAtMost(viewportHeightPx)
        val expandedHeightPx = anchors.expandedHeightPx
            .coerceAtMost(viewportHeightPx)
            .coerceAtLeast(collapsedHeightPx)

        val holder = state.stateHolder
        val drawerSnapshot = rememberDrawerStateSnapshot(holder)
        val targetAnchor = drawerSnapshot.targetAnchorId?.let { id ->
            drawerSnapshot.anchors.firstOrNull { it.id == id }
        }
        val targetHeightPx = targetAnchor?.heightPx ?: drawerSnapshot.currentHeightPx
        val animatedHeightPx by animateIntAsState(
            targetValue = if (drawerSnapshot.isDragging) drawerSnapshot.currentHeightPx else targetHeightPx,
            animationSpec = if (!state.isGeometryInitialized || drawerSnapshot.isDragging) snap() else
                tween<Int>(layoutConfig.animationDurationMs.coerceIn(0L, 1200L).toInt()),
            label = "symbol-drawer-height"
        )
        val animationGeneration = holder.activeAnimationGeneration

        LaunchedEffect(state, collapsedHeightPx, expandedHeightPx, density.density) {
            state.synchronizeGeometry(collapsedHeightPx, expandedHeightPx, density.density)
        }
        LaunchedEffect(holder, animatedHeightPx, animationGeneration, targetAnchor?.id) {
            val generation = animationGeneration ?: return@LaunchedEffect
            if (!holder.updateAnimatedHeight(animatedHeightPx, generation)) return@LaunchedEffect
            val target = targetAnchor ?: return@LaunchedEffect
            if (animatedHeightPx == target.heightPx) {
                holder.finishAnimation(target.id, generation)
            }
        }

        // Render the state-holder's exact pixel position while a finger owns
        // the gesture. Feeding every pointer delta through animateIntAsState
        // adds a frame of lag and lets the indicator/grid threshold oscillate
        // around the expanded anchor during fast drags.
        val renderedHeightPx = if (drawerSnapshot.isDragging) {
            drawerSnapshot.currentHeightPx
        } else {
            animatedHeightPx
        }
        val drawerHeight = with(density) { renderedHeightPx.toDp() }
        val expansionFraction = ((renderedHeightPx - collapsedHeightPx).toFloat() /
            (expandedHeightPx - collapsedHeightPx).coerceAtLeast(1)).coerceIn(0f, 1f)
        val revealedIndicatorHeight =
            (layoutConfig.indicatorHeightDp * expansionFraction).coerceAtLeast(0f).dp
        val minimumFlingVelocity = with(density) {
            layoutConfig.flingVelocityDpPerSecond.dp.toPx()
        }
        val nestedScrollConnection = remember(holder, density.density, minimumFlingVelocity) {
            DrawerNestedScrollConnection(
                stateHolder = holder,
                minimumFlingVelocityPxPerSecond = minimumFlingVelocity,
                // The symbol grid owns scrolling only when a gesture begins at
                // the fully expanded anchor. At every intermediate height the
                // direct drawer recognizer is the sole vertical-drag owner.
                canStartNestedScroll = {
                    holder.currentHeightPx >= holder.anchors.maxOf { it.heightPx }
                }
            )
        }

        val pagerState = rememberPagerState(
            initialPage = state.selectedPage.coerceIn(0, (safeGroups.size - 1).coerceAtLeast(0)),
            pageCount = { safeGroups.size }
        )

        LaunchedEffect(state.selectedPage, safeGroups.size) {
            if (safeGroups.isNotEmpty()) {
                val target = state.selectedPage.coerceIn(0, safeGroups.lastIndex)
                if (pagerState.currentPage != target) pagerState.animateScrollToPage(target)
            }
        }
        LaunchedEffect(pagerState.currentPage) {
            state.selectedPage = pagerState.currentPage
        }
        LaunchedEffect(safeGroups.size) {
            if (safeGroups.isEmpty()) state.selectedPage = 0
            else state.selectedPage = state.selectedPage.coerceIn(0, safeGroups.lastIndex)
        }

        // Keep the nested-scroll node stable for the whole gesture. The
        // symbol grid dispatches nested scroll only while fully expanded;
        // while collapsed/partially expanded it is non-scrollable and the
        // sheet's direct draggable owns the gesture.
        //
        // At the expanded anchor the direct draggable is disabled, so the grid
        // consumes its own scroll first and returns downward remainder through
        // this parent connection when it reaches the first row.
        val fullyExpanded = expansionFraction >= 0.999f
        val expandedAtGestureStart = rememberUpdatedState(fullyExpanded)
        val indicatorDragBoundaryPx = with(density) {
            layoutConfig.indicatorHeightDp.dp.toPx()
        }
        val indicatorBoundaryAtGestureStart = rememberUpdatedState(indicatorDragBoundaryPx)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(drawerHeight)
                .nestedScroll(nestedScrollConnection)
                .pointerInput(holder, minimumFlingVelocity) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val pointerId = down.id
                        val isExpandedAtDown = expandedAtGestureStart.value
                        val dragFromIndicator = down.position.y <= indicatorBoundaryAtGestureStart.value
                        val velocityTracker = VelocityTracker()
                        velocityTracker.addPosition(down.uptimeMillis, down.position)
                        var lastPosition = down.position
                        var axis = 0 // 0 = undecided, 1 = horizontal, 2 = vertical
                        var ownsDirectDrag = false
                        var completed = false

                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId }
                                    ?: break
                                velocityTracker.addPosition(change.uptimeMillis, change.position)

                                val delta = change.position - lastPosition
                                val total = change.position - down.position
                                if (axis == 0 && total.getDistance() > viewConfiguration.touchSlop) {
                                    axis = when {
                                        kotlin.math.abs(total.x) >
                                            kotlin.math.abs(total.y) * 1.15f -> 1
                                        kotlin.math.abs(total.y) >
                                            kotlin.math.abs(total.x) * 1.15f -> 2
                                        else -> 0
                                    }
                                    // Snapshot expansion only when the gesture starts.
                                    // Reaching the expanded anchor mid-drag must not disable
                                    // this pointer pipeline before UP and strand isDragging.
                                    val shouldOwnDirectDrag = axis == 2 && when {
                                        dragFromIndicator -> total.y > 0f
                                        !isExpandedAtDown -> total.y < 0f
                                        else -> false
                                    }
                                    if (shouldOwnDirectDrag) {
                                        holder.beginDrag()
                                        ownsDirectDrag = true
                                    }
                                }

                                if (ownsDirectDrag && axis == 2 && delta.y != 0f) {
                                    holder.dragBy(delta.y)
                                    change.consume()
                                }
                                lastPosition = change.position

                                if (!change.pressed) {
                                    if (ownsDirectDrag && holder.isDragging) {
                                        holder.endDrag(
                                            fingerVelocityY = velocityTracker.calculateVelocity().y,
                                            minimumFlingVelocityPxPerSecond = minimumFlingVelocity
                                        )
                                    }
                                    completed = true
                                    break
                                }
                            }
                        } finally {
                            if (!completed && ownsDirectDrag && holder.isDragging) {
                                holder.cancelDrag()
                            }
                        }
                    }
                },
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 2.dp,
            shape = MaterialTheme.shapes.large
        ) {
            Column(Modifier.fillMaxSize()) {
                SymbolGroupIndicator(
                    groups = safeGroups,
                    selectedIndex = pagerState.currentPage,
                    onGroupSelected = { state.selectedPage = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(revealedIndicatorHeight)
                        .graphicsLayer {
                            alpha = expansionFraction
                            translationY = -(
                                (1f - expansionFraction) *
                                    layoutConfig.indicatorHeightDp *
                                    density.density * 0.2f
                                )
                        },
                    settings = settings
                )

                if (safeGroups.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.compose_symbol_drawer_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        flingBehavior = PagerDefaults.flingBehavior(
                            state = pagerState,
                            pagerSnapDistance = PagerSnapDistance.atMost(1)
                        ),
                        key = { page -> safeGroups[page].id }
                    ) { page ->
                        val group = safeGroups[page]
                        SymbolPage(
                            group = group,
                            config = layoutConfig,
                            settings = settings,
                            userScrollEnabled = expansionFraction >= 0.999f && !drawerSnapshot.isDragging,
                            modifier = Modifier.fillMaxSize(),
                            onSymbolClick = { entry ->
                                usageState?.recordRecentlyUsed(entry.id)
                                onSymbolClick(entry)
                            },
                            onSymbolLongClick = onSymbolLongClick,
                        )
                    }
                }
            }
        }
    }
}

