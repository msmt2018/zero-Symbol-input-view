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
 * The SPDX operator "AND" means that all listed licenses apply
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

package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.config.DrawerConfig
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.config.DrawerPresentationMode
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core.DefaultDrawerController
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core.DrawerAnchorInput
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core.DrawerAnchorStrategy
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core.DrawerStateHolder
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerPage
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerPageHost
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerNavigationPlacement
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.panel.DrawerPanelHost
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.panel.DrawerPanelSlot

/**
 * Composition-owned facade over the host-facing controller and its single
 * shared anchor state engine. Geometry, drag and animation state are never
 * duplicated in this container.
 */
@Stable
class DrawerContainerState internal constructor(
    private val initiallyExpanded: Boolean
) {
    private var latestAnchorInput = DrawerAnchorInput(
        minHeightPx = 0,
        maxHeightPx = 0,
        viewportHeightPx = 0,
        contentHeightPx = 0,
        existingExpandedHeightPx = 0
    )

    val controller: DefaultDrawerController
    internal val stateHolder: DrawerStateHolder
        get() = controller.stateHolder

    private var geometryInitialized = false
    internal val isGeometryInitialized: Boolean get() = geometryInitialized

    init {
        stateHolder = DrawerStateHolder(
            initialInput = latestAnchorInput,
            initialAnchorId = if (initiallyExpanded) "expanded" else "peek"
        )
        controller = DefaultDrawerController(stateHolder) { latestAnchorInput }
    }

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

    fun expand() { controller.expand() }
    fun collapse() { controller.collapse() }
    fun toggle() { if (expanded) controller.collapse() else controller.expand() }

    internal fun synchronizeGeometry(
        input: DrawerAnchorInput,
        strategy: DrawerAnchorStrategy
    ) {
        latestAnchorInput = input
        controller.setAnchorStrategy(strategy)
        controller.refreshAnchors()
        if (!geometryInitialized) {
            val initial = if (initiallyExpanded) stateHolder.anchors.maxByOrNull { it.heightPx }
                else stateHolder.anchors.minByOrNull { it.heightPx }
            initial?.let { controller.moveToAnchor(it.id, animated = false) }
            geometryInitialized = true
        }
    }
}

@Composable
fun rememberDrawerContainerState(initiallyExpanded: Boolean = false): DrawerContainerState =
    remember { DrawerContainerState(initiallyExpanded) }

/**
 * Reusable Compose drawer container.
 *
 * The host, peek slot and expanded slot are composable content lambdas. Gesture
 * jobs and animation state are owned by Composition; there are no View,
 * nested-scrolling, attach/detach or manual disposal contracts.
 */
@Composable
fun DrawerContainer(
    config: DrawerConfig = DrawerConfig(),
    modifier: Modifier = Modifier,
    state: DrawerContainerState = rememberDrawerContainerState(),
    title: String? = null,
    pages: List<DrawerPage> = emptyList(),
    selectedPageId: String? = null,
    onPageSelected: (DrawerPage) -> Unit = {},
    onDismiss: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val dimensions = config.style.dimensions
    val collapsedHeight = (dimensions.peekHeightDp + dimensions.handleHeightDp + 24f).dp
    val collapsedHeightPx = with(density) { collapsedHeight.roundToPx() }
    val maxExpandedHeightPx = with(density) { dimensions.expandedMaxHeightDp.dp.roundToPx() }
    val resolvedExpandedHeightPx = maxExpandedHeightPx.coerceAtLeast(collapsedHeightPx)
    val resolvedExpandedHeight = with(density) { resolvedExpandedHeightPx.toDp() }
    val animationSpec: androidx.compose.animation.core.AnimationSpec<Int> =
        if (config.style.motion.enableSpring) spring() else
            androidx.compose.animation.core.tween<Int>(
                durationMillis = config.style.motion.settleDurationMs.coerceIn(0L, 1200L).toInt()
            )

    val controller = state.controller
    val holder = controller.stateHolder
    val snapshot = holder.snapshot
    val targetAnchor = snapshot.targetAnchorId?.let { id ->
        snapshot.anchors.firstOrNull { it.id == id }
    }
    val targetHeightPx = targetAnchor?.heightPx ?: snapshot.currentHeightPx
    val animatedHeightPx by androidx.compose.animation.core.animateIntAsState(
        targetValue = if (snapshot.isDragging) snapshot.currentHeightPx else targetHeightPx,
        animationSpec = if (!state.isGeometryInitialized || snapshot.isDragging) snap() else animationSpec,
        label = "drawer-container-height"
    )
    val generation = controller.activeAnimationGeneration()

    LaunchedEffect(state, collapsedHeightPx, resolvedExpandedHeightPx, config.anchorStrategy, density.density) {
        val viewportHeightPx = resolvedExpandedHeightPx
        state.synchronizeGeometry(
            input = DrawerAnchorInput(
                minHeightPx = collapsedHeightPx.coerceAtMost(viewportHeightPx),
                maxHeightPx = viewportHeightPx,
                viewportHeightPx = viewportHeightPx,
                contentHeightPx = viewportHeightPx,
                existingExpandedHeightPx = viewportHeightPx,
                density = density.density
            ),
            strategy = config.anchorStrategy
        )
    }

    LaunchedEffect(holder, animatedHeightPx, generation, targetAnchor?.id) {
        val activeGeneration = generation ?: return@LaunchedEffect
        if (!holder.updateAnimatedHeight(animatedHeightPx, activeGeneration)) return@LaunchedEffect
        val activeTarget = targetAnchor ?: return@LaunchedEffect
        if (animatedHeightPx == activeTarget.heightPx) {
            holder.finishAnimation(activeTarget.id, activeGeneration)
        }
    }

    val dragState = remember(holder) {
        DraggableState { delta -> holder.dragBy(delta) }
    }
    val nestedScrollConnection = remember(
        holder,
        config.style.motion.minimumFlingVelocityDp,
        density.density
    ) {
        com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core.DrawerNestedScrollConnection(
            stateHolder = holder,
            minimumFlingVelocityPxPerSecond =
                config.style.motion.minimumFlingVelocityDp * density.density
        )
    }
    val expanded = state.expanded || config.presentationMode == DrawerPresentationMode.FULL_SCREEN
    val displayHeight = with(density) { animatedHeightPx.toDp() }

    // System-back dispatch is intentionally host-owned. The library does not
    // register lifecycle-bound back callbacks; the host can collapse this
    // composition-owned state from its own platform integration.

    Box(modifier.fillMaxSize()) {
        content()

        if ((expanded || config.presentationMode == DrawerPresentationMode.FULL_SCREEN) && config.style.drawScrim) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        config.style.colors.scrim?.let { Color(it) }
                            ?: Color.Black.copy(alpha = 0.32f)
                    )
                    .clickable(enabled = config.dismissOnScrimTap) {
                        controller.collapse()
                        onDismiss()
                    }
            )
        }

        val surfaceColor = config.style.colors.surface?.let(::Color)
            ?: MaterialTheme.colorScheme.surface
        val corner = config.style.shape
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(if (config.presentationMode == DrawerPresentationMode.FULL_SCREEN) resolvedExpandedHeight else displayHeight)
                .then(
                    if (config.enableNestedScroll) Modifier.nestedScroll(nestedScrollConnection)
                    else Modifier
                )
                .draggable(
                    state = dragState,
                    orientation = Orientation.Vertical,
                    enabled = config.enableDrag,
                    onDragStarted = { holder.beginDrag() },
                    onDragStopped = { velocity ->
                        holder.endDrag(
                            fingerVelocityY = velocity,
                            minimumFlingVelocityPxPerSecond = config.style.motion.minimumFlingVelocityDp * density.density
                        )
                    }
                ),
            color = surfaceColor,
            contentColor = config.style.colors.primaryText?.let(::Color)
                ?: MaterialTheme.colorScheme.onSurface,
            shadowElevation = config.style.elevationDp.dp,
            shape = RoundedCornerShape(
                topStart = corner.topStartRadiusDp.dp,
                topEnd = corner.topEndRadiusDp.dp,
                bottomEnd = corner.bottomEndRadiusDp.dp,
                bottomStart = corner.bottomStartRadiusDp.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = dimensions.horizontalPaddingDp.dp,
                        vertical = dimensions.verticalPaddingDp.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(dimensions.panelSpacingDp.dp)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height((dimensions.handleHeightDp + 12f).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(width = dimensions.handleWidthDp.dp, height = dimensions.handleHeightDp.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
                    )
                }

                if (title != null) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                }

                if (pages.isNotEmpty()) {
                    if (config.navigationConfig.enabled &&
                        config.navigationConfig.placement != DrawerNavigationPlacement.HIDDEN
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pages.forEach { page ->
                                Text(
                                    text = page.title,
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.small)
                                        .clickable { onPageSelected(page) }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    color = if (page.id == selectedPageId) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    DrawerPageHost(
                        pages = pages,
                        selectedPageId = selectedPageId,
                        onPageSelected = onPageSelected,
                        modifier = Modifier.weight(1f)
                    )
                } else if (expanded) {
                    DrawerPanelHost(
                        content = config.expandedContent ?: config.peekContent,
                        modifier = Modifier.weight(1f),
                        slot = DrawerPanelSlot.EXPANDED
                    )
                } else {
                    DrawerPanelHost(
                        content = config.peekContent,
                        modifier = Modifier.weight(1f),
                        slot = DrawerPanelSlot.PEEK
                    )
                }
            }
        }
    }
}
