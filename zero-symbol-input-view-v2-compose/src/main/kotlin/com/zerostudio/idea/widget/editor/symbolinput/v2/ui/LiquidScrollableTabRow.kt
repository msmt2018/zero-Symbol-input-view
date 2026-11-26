/*
 * This file is part of Zero Symbol Input View V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Material 3 scrollable tabs with a shared spring-driven liquid capsule indicator.
 *
 * The indicator animation and the sub-1dp separator are part of this component,
 * so consumers do not need to reimplement or adapt them at each call site.
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
                    val left by animateDpAsState(
                        targetValue = position.left,
                        animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
                        label = "liquid-tab-left"
                    )
                    val width by animateDpAsState(
                        targetValue = position.width,
                        animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
                        label = "liquid-tab-width"
                    )
                    Box(Modifier.fillMaxSize()) {
                        Box(
                            Modifier
                                .offset(x = left)
                                .width(width)
                                .fillMaxHeight()
                                .padding(horizontal = 7.dp, vertical = 5.dp)
                                .clip(CircleShape)
                                .background(colorScheme.primary.copy(alpha = 0.14f))
                                .align(Alignment.CenterStart)
                        )
                    }
                }
            }
        ) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = selected == index,
                    onClick = { onTabSelected(index) },
                    text = {
                        Text(
                            text = title,
                            maxLines = 1,
                            color = if (selected == index) colorScheme.primary
                            else colorScheme.onSurfaceVariant
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
