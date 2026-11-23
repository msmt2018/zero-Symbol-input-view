/*
 * This file is part of zero-Symbol-input-view-V2.
 *
 * Copyright (C) 2026 android_zero
 *
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 *
 * License Information:
 *
 * This file is distributed under the following licenses, which apply
 * cumulatively to the same work:
 *
 * 1. GNU Affero General Public License v3.0 only (AGPL-3.0-only):
 *    Grants the rights to use, study, modify, and redistribute this work.
 *    When a modified version is used to provide network services to users,
 *    the corresponding source code must be made available under the
 *    applicable AGPL terms.
 *
 * 2. GNU General Public License v3.0 only (GPL-3.0-only):
 *    Grants the rights to use, study, modify, and redistribute this work.
 *    Distribution of covered binaries requires compliance with the source
 *    code, license notice, attribution, and other applicable GPL terms.
 *
 * 3. Apache License 2.0 (Apache-2.0):
 *    Grants copyright and applicable patent rights to use, modify, and
 *    distribute this work, subject to its license conditions, including
 *    preservation of notices, inclusion of the license, and marking changes.
 *
 * The SPDX operator "AND" means that all listed license terms apply
 * cumulatively. It does not mean that recipients may choose only one
 * of the listed licenses.
 *
 * You must comply with the applicable terms of all three licenses.
 * This notice is a summary and does not replace the full license texts.
 *
 * Full license texts:
 * - GNU AGPL v3.0: https://www.gnu.org/licenses/agpl-3.0.html
 * - GNU GPL v3.0:  https://www.gnu.org/licenses/gpl-3.0.html
 * - Apache 2.0:    https://www.apache.org/licenses/LICENSE-2.0
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
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.zerostudio.idea.widget.editor.symbolinput.v2.R
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DrawerConfig
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
import com.zerostudio.idea.widget.editor.symbolinput.v2.ui.SymbolGroupIndicator
import com.zerostudio.idea.widget.editor.symbolinput.v2.ui.SymbolPage
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core.DrawerAnchorInput
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core.DrawerStateHolder
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core.ExistingHeightAnchorStrategy

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

    fun expand() { stateHolder.expandToMaxAnchor() }
    fun collapse() { stateHolder.collapseToMinAnchor() }
    fun toggle() { if (expanded) collapse() else expand() }
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

/** Creates state whose lifetime is the calling Composition. */
@Composable
fun rememberSymbolDrawerState(
    initialPage: Int = 0,
    initiallyExpanded: Boolean = false
): SymbolDrawerState = remember {
    SymbolDrawerState(initialPage, initiallyExpanded)
}

/**
 * Native Jetpack Compose symbol drawer.
 *
 * Group paging, symbol layout, expansion gestures and state observation are
 * implemented with Compose Foundation and Material 3. The library never
 * creates or embeds a traditional Android View.
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
    onOpenSettings: (() -> Unit)? = null
) {
    val safeGroups = groups
    val density = LocalDensity.current
    val collapsedHeight = (config.collapsedRows * config.cellHeightDp +
        config.indicatorHeightDp + 28).coerceAtLeast(96).dp
    val collapsedHeightPx = with(density) { collapsedHeight.roundToPx() }
    val expandedHeightPx = with(density) {
        config.maxExpandedHeightDp.dp.roundToPx().coerceAtLeast(collapsedHeightPx)
    }
    val holder = state.stateHolder
    val drawerSnapshot = holder.snapshot
    val targetAnchor = drawerSnapshot.targetAnchorId?.let { id ->
        drawerSnapshot.anchors.firstOrNull { it.id == id }
    }
    val targetHeightPx = targetAnchor?.heightPx ?: drawerSnapshot.currentHeightPx
    val animatedHeightPx by animateIntAsState(
        targetValue = if (drawerSnapshot.isDragging) drawerSnapshot.currentHeightPx else targetHeightPx,
        animationSpec = if (!state.isGeometryInitialized || drawerSnapshot.isDragging) snap() else
            tween<Int>(config.animationDurationMs.coerceIn(0L, 1200L).toInt()),
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
    val drawerHeight = with(density) { animatedHeightPx.toDp() }
    val dragState = remember(holder) {
        DraggableState { delta -> holder.dragBy(delta) }
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

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(drawerHeight),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Vertical,
                        onDragStarted = { holder.beginDrag() },
                        onDragStopped = { velocity ->
                            holder.endDrag(
                                fingerVelocityY = velocity,
                                minimumFlingVelocityPxPerSecond = with(density) { 900.dp.toPx() }
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
                )
            }

            SymbolGroupIndicator(
                groups = safeGroups,
                selectedIndex = pagerState.currentPage,
                onGroupSelected = { state.selectedPage = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(config.indicatorHeightDp.coerceAtLeast(0).dp),
                settings = settings,
                onSettingsClick = onOpenSettings,
                onManagerClick = onOpenManager
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
                    key = { page -> safeGroups[page].id }
                ) { page ->
                    val group = safeGroups[page]
                    SymbolPage(
                        group = group,
                        config = config,
                        settings = settings,
                        modifier = Modifier.fillMaxSize(),
                        onSymbolClick = onSymbolClick,
                        onSymbolLongClick = onSymbolLongClick
                    )
                }
            }
        }
    }
}

