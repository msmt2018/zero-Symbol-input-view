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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
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
 * Material 3 scrollable tabs with an interruptible, edge-driven liquid capsule.
 *
 * The leading edge accelerates first while the trailing edge follows after a
 * short delay. The capsule then contracts past its resting width and settles
 * with a damped spring. Animatable preserves the live value and velocity when
 * a new selection interrupts an in-flight transition.
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
                val position = positions.getOrNull(selected)
                if (position != null) {
                    val leftEdge = remember { Animatable(position.left.value) }
                    val rightEdge = remember {
                        Animatable(position.left.value + position.width.value)
                    }
                    var previousSelected by remember { mutableIntStateOf(selected) }

                    LaunchedEffect(selected, positions.size) {
                        val previous = previousSelected
                        previousSelected = selected
                        if (previous == selected) {
                            leftEdge.snapTo(position.left.value)
                            rightEdge.snapTo(position.left.value + position.width.value)
                            return@LaunchedEffect
                        }

                        val targetLeft = position.left.value
                        val targetRight = targetLeft + position.width.value
                        val targetWidth = (targetRight - targetLeft).coerceAtLeast(1f)
                        val maxStretchWidth = targetWidth * 2.2f
                        val overshoot = 5f
                        val movingRight = selected > previous

                        // Stage 1 (0–140ms): the leading edge reaches ahead;
                        // the trailing edge starts 55ms later and is width-clamped.
                        val sourceLeft = leftEdge.value
                        val sourceRight = rightEdge.value
                        val stretchLeading = if (movingRight) {
                            targetRight + overshoot
                        } else {
                            targetLeft - overshoot
                        }
                        val stretchTrailing = if (movingRight) {
                            (stretchLeading - maxStretchWidth).coerceAtLeast(sourceLeft)
                        } else {
                            (stretchLeading + maxStretchWidth).coerceAtMost(sourceRight)
                        }

                        coroutineScope {
                            val leading = launch {
                                if (movingRight) {
                                    rightEdge.animateTo(
                                        stretchLeading,
                                        animationSpec = tween(140)
                                    )
                                } else {
                                    leftEdge.animateTo(
                                        stretchLeading,
                                        animationSpec = tween(140)
                                    )
                                }
                            }
                            val trailing = launch {
                                delay(55)
                                if (movingRight) {
                                    leftEdge.animateTo(
                                        stretchTrailing,
                                        animationSpec = tween(85)
                                    )
                                } else {
                                    rightEdge.animateTo(
                                        stretchTrailing,
                                        animationSpec = tween(85)
                                    )
                                }
                            }
                            joinAll(leading, trailing)
                        }

                        // Stage 2 (140–280ms): contract to a slightly compressed
                        // capsule while retaining a small directional overshoot.
                        val compressedWidth = targetWidth * 0.95f
                        val contractedLeft = if (movingRight) {
                            stretchLeading - compressedWidth
                        } else {
                            stretchLeading
                        }
                        val contractedRight = if (movingRight) {
                            stretchLeading
                        } else {
                            stretchLeading + compressedWidth
                        }
                        coroutineScope {
                            val left = launch {
                                leftEdge.animateTo(
                                    contractedLeft,
                                    animationSpec = tween(140)
                                )
                            }
                            val right = launch {
                                rightEdge.animateTo(
                                    contractedRight,
                                    animationSpec = tween(140)
                                )
                            }
                            joinAll(left, right)
                        }

                        // Stage 3: one restrained under-damped settle to the
                        // exact target bounds. A new click cancels this effect
                        // and continues from these live edge values/velocities.
                        coroutineScope {
                            val settle = spring<Float>(
                                dampingRatio = 0.68f,
                                stiffness = 420f,
                                visibilityThreshold = 0.35f
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
                        val top = 5.dp.toPx()
                        val bottom = size.height - top
                        val height = (bottom - top).coerceAtLeast(0f)
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
