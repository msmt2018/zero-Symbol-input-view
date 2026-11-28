/*
 * This file is part of Zero Symbol Input View V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.joinAll

/**
 * Material 3 scrollable tabs with a connected, water-like liquid capsule.
 *
 * A selection change is deliberately not implemented as a translated fixed
 * indicator. The leading edge flows to the destination first, the trailing
 * edge follows and stretches the capsule across the source and destination.
 * The capsule then contracts at the destination and settles with a restrained
 * spring rebound. Animatable keeps the live edge values and velocities when a
 * new selection interrupts an in-flight transition.
 *
 * @param titles ordered tab labels.
 * @param selectedIndex selected zero-based tab index; out-of-range values are clamped.
 * @param onTabSelected invoked with the selected zero-based index.
 * @param modifier modifier applied to the tab row and its separator.
 */
@Composable
fun LiquidScrollableTabRow(
    titles: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (titles.isEmpty()) return

    val selected = selectedIndex.coerceIn(titles.indices)
    val colorScheme = MaterialTheme.colorScheme

    Column(modifier) {
        ScrollableTabRow(
            selectedTabIndex = selected,
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            contentColor = colorScheme.primary,
            divider = {},
            indicator = { positions ->
                val target = positions.getOrNull(selected)
                if (target != null) {
                    val leftEdge = remember {
                        Animatable(target.left.value)
                    }
                    val rightEdge = remember {
                        Animatable(target.left.value + target.width.value)
                    }
                    var previousSelected by remember { mutableIntStateOf(selected) }

                    LaunchedEffect(selected, positions.size) {
                        val previous = previousSelected
                        previousSelected = selected

                        val targetLeft = target.left.value
                        val targetRight = targetLeft + target.width.value
                        if (previous == selected) {
                            leftEdge.snapTo(targetLeft)
                            rightEdge.snapTo(targetRight)
                            return@LaunchedEffect
                        }

                        val movingRight = selected > previous
                        val sourceLeft = leftEdge.value
                        val sourceRight = rightEdge.value

                        // A tiny overshoot gives the advancing water front a
                        // rounded, energetic arrival without changing tab bounds.
                        val leadingOvershoot = 3.5f

                        /*
                         * Phase 1 — FLOW / CONNECT (about 170 ms)
                         *
                         * Move the leading edge to the far side of the target.
                         * The trailing edge follows shortly afterwards, so the
                         * capsule becomes one continuous body spanning both tabs
                         * instead of teleporting or drawing two separate pills.
                         */
                        coroutineScope {
                            val leading = launch {
                                if (movingRight) {
                                    rightEdge.animateTo(
                                        targetRight + leadingOvershoot,
                                        animationSpec = tween(
                                            durationMillis = 165,
                                            easing = androidx.compose.animation.core.FastOutSlowInEasing
                                        )
                                    )
                                } else {
                                    leftEdge.animateTo(
                                        targetLeft - leadingOvershoot,
                                        animationSpec = tween(
                                            durationMillis = 165,
                                            easing = androidx.compose.animation.core.FastOutSlowInEasing
                                        )
                                    )
                                }
                            }

                            val trailing = launch {
                                delay(48)
                                if (movingRight) {
                                    leftEdge.animateTo(
                                        sourceLeft,
                                        animationSpec = tween(
                                            durationMillis = 118,
                                            easing = androidx.compose.animation.core.LinearOutSlowInEasing
                                        )
                                    )
                                } else {
                                    rightEdge.animateTo(
                                        sourceRight,
                                        animationSpec = tween(
                                            durationMillis = 118,
                                            easing = androidx.compose.animation.core.LinearOutSlowInEasing
                                        )
                                    )
                                }
                            }
                            joinAll(leading, trailing)
                        }

                        /*
                         * Phase 2 — CONTRACT / LAND (about 145 ms)
                         *
                         * Keep the leading edge at the destination while the
                         * trailing edge catches up. This is the key liquid
                         * contraction: the stretched bridge becomes the target
                         * capsule from its advancing end, not a fixed-width slide.
                         */
                        coroutineScope {
                            if (movingRight) {
                                leftEdge.animateTo(
                                    targetLeft,
                                    animationSpec = tween(
                                        durationMillis = 145,
                                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                                    )
                                )
                            } else {
                                rightEdge.animateTo(
                                    targetRight,
                                    animationSpec = tween(
                                        durationMillis = 145,
                                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                                    )
                                )
                            }
                        }

                        /*
                         * Phase 3 — REBOUND / SETTLE
                         *
                         * The spring only resolves the small leading-edge
                         * overshoot. Its damping keeps the final rebound subtle
                         * and close to the approximately 400 ms reference motion.
                         */
                        coroutineScope {
                            val settle = spring<Float>(
                                dampingRatio = 0.78f,
                                stiffness = 520f,
                                visibilityThreshold = 0.25f
                            )
                            val left = launch {
                                leftEdge.animateTo(targetLeft, animationSpec = settle)
                            }
                            val right = launch {
                                rightEdge.animateTo(targetRight, animationSpec = settle)
                            }
                            joinAll(left, right)
                        }
                    }

                    Canvas(Modifier.fillMaxSize()) {
                        val left = leftEdge.value.dp.toPx()
                        val right = rightEdge.value.dp.toPx()
                        val verticalInset = 5.dp.toPx()
                        val top = verticalInset
                        val height = (size.height - verticalInset * 2f).coerceAtLeast(0f)
                        val width = (right - left).coerceAtLeast(0f)

                        if (width > 0f && height > 0f) {
                            drawRoundRect(
                                color = colorScheme.primary.copy(alpha = 0.14f),
                                topLeft = Offset(left, top),
                                size = Size(width, height),
                                cornerRadius = CornerRadius(height / 2f)
                            )
                        }
                    }
                }
            }
        ) {
            titles.forEachIndexed { index, title ->
                val isSelected = selected == index
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) colorScheme.primary
                    else colorScheme.onSurfaceVariant,
                    animationSpec = tween(
                        durationMillis = 150,
                        delayMillis = if (isSelected) 105 else 0
                    ),
                    label = "liquid-tab-content-color"
                )
                Tab(
                    selected = isSelected,
                    onClick = { onTabSelected(index) },
                    text = {
                        Text(
                            text = title,
                            maxLines = 1,
                            color = contentColor
                        )
                    }
                )
            }
        }

        HorizontalDivider(
            thickness = 0.5.dp,
            color = colorScheme.outlineVariant.copy(alpha = 0.72f)
        )
    }
}
