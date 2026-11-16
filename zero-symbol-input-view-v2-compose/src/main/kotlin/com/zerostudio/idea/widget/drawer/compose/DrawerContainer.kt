/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 * Author: android_zero · https://github.com/msmt2018
 */

package com.zerostudio.idea.widget.drawer.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.draggable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** A stable named snap point in a bottom drawer. */
data class DrawerAnchor(val id: String, val height: Dp) {
    init { require(id.isNotBlank()); require(height >= 0.dp) }
}

/** Generic Compose drawer configuration independent from symbol-toolbar models. */
data class DrawerConfig(
    val collapsedHeight: Dp = 112.dp,
    val expandedHeight: Dp = 480.dp,
    val dragHandleEnabled: Boolean = true,
    val animationDurationMillis: Int = 220,
    val dismissOnScrimTap: Boolean = false
) {
    init {
        require(collapsedHeight >= 0.dp)
        require(expandedHeight >= collapsedHeight)
        require(animationDurationMillis in 0..1200)
    }
}

/** Saveable state and controller for the generic Compose bottom drawer. */
@Stable
class DrawerState internal constructor(initialFraction: Float, initialPageId: String?) {
    internal val fraction = Animatable(initialFraction.coerceIn(0f, 1f))
    var selectedPageId: String? = initialPageId
        private set
    val expansionFraction: Float get() = fraction.value
    val isExpanded: Boolean get() = fraction.value >= 0.5f

    fun selectPage(id: String) { selectedPageId = id }

    suspend fun expand(durationMillis: Int = 220) {
        fraction.animateTo(1f, tween(durationMillis.coerceIn(0, 1200)))
    }
    suspend fun collapse(durationMillis: Int = 220) {
        fraction.animateTo(0f, tween(durationMillis.coerceIn(0, 1200)))
    }
    internal suspend fun dragBy(delta: Float) {
        fraction.snapTo((fraction.value + delta).coerceIn(0f, 1f))
    }
    internal suspend fun settle(velocity: Float, durationMillis: Int) {
        if (velocity < -0.8f || (velocity.absoluteValue() <= 0.8f && fraction.value >= 0.5f)) {
            expand(durationMillis)
        } else collapse(durationMillis)
    }

    companion object {
        val Saver: Saver<DrawerState, List<Any>> = listSaver(
            save = { listOf(it.fraction.value, it.selectedPageId.orEmpty()) },
            restore = {
                DrawerState(
                    (it.getOrNull(0) as? Float) ?: 0f,
                    (it.getOrNull(1) as? String)?.takeIf(String::isNotEmpty)
                )
            }
        )
    }
}

@Composable
fun rememberDrawerState(
    initiallyExpanded: Boolean = false,
    initialPageId: String? = null
): DrawerState = rememberSaveable(saver = DrawerState.Saver) {
    DrawerState(if (initiallyExpanded) 1f else 0f, initialPageId)
}

/**
 * Generic bottom-anchored Compose drawer. The host supplies its own content;
 * this component owns only sheet geometry, handle and expansion gestures.
 */
@Composable
fun DrawerContainer(
    state: DrawerState,
    modifier: Modifier = Modifier,
    config: DrawerConfig = DrawerConfig(),
    peekContent: @Composable () -> Unit,
    expandedContent: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxSize()) {
        val safeExpanded = config.expandedHeight.coerceAtMost(maxHeight)
        val safeCollapsed = config.collapsedHeight.coerceAtMost(safeExpanded)
        val targetHeight = safeCollapsed + (safeExpanded - safeCollapsed) * state.expansionFraction
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Surface(
                modifier = Modifier.fillMaxWidth().requiredHeight(targetHeight),
                color = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 3.dp,
                shadowElevation = 4.dp
            ) {
                Column {
                    if (config.dragHandleEnabled) {
                        Box(
                            Modifier.fillMaxWidth().height(28.dp).draggable(
                                orientation = Orientation.Vertical,
                                state = rememberDraggableState { delta ->
                                    val range = with(density) { (safeExpanded - safeCollapsed).toPx().coerceAtLeast(1f) }
                                    scope.launch { state.dragBy(-delta / range) }
                                },
                                onDragStopped = { velocity ->
                                    val range = with(density) { (safeExpanded - safeCollapsed).toPx().coerceAtLeast(1f) }
                                    scope.launch { state.settle(velocity / range, config.animationDurationMillis) }
                                }
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                Modifier.size(36.dp, 4.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f))
                            )
                        }
                    }
                    if (state.expansionFraction < 0.5f) peekContent() else expandedContent()
                }
            }
        }
    }
}

private fun Float.absoluteValue(): Float = kotlin.math.abs(this)
