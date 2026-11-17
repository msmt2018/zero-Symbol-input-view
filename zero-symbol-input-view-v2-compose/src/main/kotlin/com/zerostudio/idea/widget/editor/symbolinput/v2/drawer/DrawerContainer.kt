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

package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draggable
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.config.DrawerConfig
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.config.DrawerPresentationMode
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerPage
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerPageHost
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerNavigationPlacement
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.panel.DrawerPanelHost
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.panel.DrawerPanelSlot

/** Composition-owned expansion state for a generic drawer container. */
@Stable
class DrawerContainerState internal constructor(initiallyExpanded: Boolean) {
    var expanded by mutableStateOf(initiallyExpanded)
        internal set
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
    val maxExpandedHeight = dimensions.expandedMaxHeightDp.dp
    val targetHeight = if (config.presentationMode == DrawerPresentationMode.FULL_SCREEN) {
        maxExpandedHeight
    } else if (state.expanded) {
        maxExpandedHeight
    } else {
        collapsedHeight
    }
    val dragThreshold = with(density) { 32.dp.toPx() }
    var dragDistance by remember { mutableFloatStateOf(0f) }
    val dragState = remember(state) {
        DraggableState { delta ->
            dragDistance += delta
            if (dragDistance < -dragThreshold) {
                state.expanded = true
                dragDistance = 0f
            } else if (dragDistance > dragThreshold) {
                state.expanded = false
                dragDistance = 0f
            }
        }
    }
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = if (config.style.motion.enableSpring) spring() else
            tween(config.style.motion.settleDurationMs.coerceIn(0L, 1200L).toInt()),
        label = "drawer-container-height"
    )

    BackHandler(enabled = state.expanded && config.dismissOnBack) {
        state.expanded = false
        onDismiss()
    }

    Box(modifier.fillMaxSize()) {
        content()

        if (state.expanded && config.style.drawScrim) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        config.style.colors.scrim?.let { Color(it) }
                            ?: Color.Black.copy(alpha = 0.32f)
                    )
                    .clickable(enabled = config.dismissOnScrimTap) {
                        state.expanded = false
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
                .height(animatedHeight)
                .draggable(
                    state = dragState,
                    orientation = Orientation.Vertical,
                    enabled = config.enableDrag,
                    onDragStarted = { dragDistance = 0f },
                    onDragStopped = { velocity ->
                        if (kotlin.math.abs(velocity) >= config.style.motion.minimumFlingVelocityDp * density.density) {
                            state.expanded = velocity < 0f
                        }
                        dragDistance = 0f
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
                } else if (state.expanded) {
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
