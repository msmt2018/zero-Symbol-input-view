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
 * 1. GNU Affero General Public License v3.0 only (AGPL-3.0-only).
 * 2. GNU General Public License v3.0 only (GPL-3.0-only).
 * 3. Apache License 2.0 (Apache-2.0).
 *
 * See the corresponding license files in this module for the complete terms.
 *
 * Author: android_zero
 * GitHub: https://github.com/msmt2018
 * WeChat: android_zero
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DrawerConfig
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
import kotlin.math.abs

/**
 * Composition-native replacement for the former RecyclerView/ListAdapter page.
 *
 * The grid owns its scroll state. While grid scrolling is enabled, downward
 * gestures remain with the grid until it reaches its first item. When grid
 * scrolling is disabled by a collapsed/partial sheet anchor, downward gestures
 * are handed directly to the sheet even if the grid retains an old scroll
 * offset. Upward gestures remain with the outer drawer while it is partial and with the grid when fully expanded.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SymbolPage(
    group: SymbolGroup,
    config: DrawerConfig,
    settings: SymbolDrawerSettings,
    modifier: Modifier = Modifier,
    userScrollEnabled: Boolean = false,
    onSymbolClick: (SymbolEntry) -> Unit,
    onSymbolLongClick: ((SymbolEntry) -> Unit)? = null,
    onExpandedSheetDragStart: (() -> Unit)? = null,
    onExpandedSheetDrag: ((Float) -> Unit)? = null,
    onExpandedSheetDragEnd: ((Float) -> Unit)? = null
) {
    val gridState = rememberLazyGridState()
    val latestUserScrollEnabled = rememberUpdatedState(userScrollEnabled)
    val dragStart = onExpandedSheetDragStart
    val drag = onExpandedSheetDrag
    val dragEnd = onExpandedSheetDragEnd
    val sheetDragEnabled = dragStart != null && drag != null && dragEnd != null

    val dragArbitrationModifier = if (sheetDragEnabled) {
        Modifier.pointerInput(gridState, dragStart, drag, dragEnd) {
            val startDrag = requireNotNull(dragStart)
            val dragBy = requireNotNull(drag)
            val endDrag = requireNotNull(dragEnd)
            awaitEachGesture {
                val down = awaitFirstDown(
                    requireUnconsumed = false,
                    pass = PointerEventPass.Initial
                )
                val pointerId = down.id
                val velocityTracker = VelocityTracker().apply {
                    addPosition(down.uptimeMillis, down.position)
                }
                var previous = down.position
                var accumulatedX = 0f
                var accumulatedY = 0f
                var ownsGesture = false

                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                    if (!change.pressed) break

                    val delta = change.position - previous
                    previous = change.position

                    if (!ownsGesture) {
                        accumulatedX += delta.x
                        accumulatedY += delta.y
                        val crossedSlop = abs(accumulatedX) > viewConfiguration.touchSlop ||
                            abs(accumulatedY) > viewConfiguration.touchSlop
                        if (!crossedSlop) continue

                        // Horizontal paging and upward grid scrolling always
                        // remain with descendants. The sheet claims downward
                        // motion when the grid is disabled or has reached its top.
                        if (abs(accumulatedX) >= abs(accumulatedY) ||
                            accumulatedY <= 0f ||
                            (latestUserScrollEnabled.value && gridState.canScrollBackward)
                        ) {
                            break
                        }

                        ownsGesture = true
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        startDrag()
                        dragBy(accumulatedY)
                        change.consume()
                    } else {
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        dragBy(delta.y)
                        change.consume()
                    }
                }

                if (ownsGesture) {
                    endDrag(velocityTracker.calculateVelocity().y)
                }
            }
        }
    } else {
        Modifier
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(config.columns.coerceIn(1, 30)),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp)
            .then(dragArbitrationModifier),
        userScrollEnabled = userScrollEnabled,
        verticalArrangement = Arrangement.spacedBy(config.cellVerticalSpacingDp.dp),
        horizontalArrangement = Arrangement.spacedBy(config.cellHorizontalSpacingDp.dp)
    ) {
        items(group.symbols, key = { it.id }) { entry ->
            SymbolCell(
                entry = entry,
                config = config,
                settings = settings,
                onClick = { onSymbolClick(entry) },
                onLongClick = onSymbolLongClick?.let { callback -> { callback(entry) } }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SymbolCell(
    entry: SymbolEntry,
    config: DrawerConfig,
    settings: SymbolDrawerSettings,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(config.cellHeightDp.coerceAtLeast(config.minimumTouchTargetDp).dp)
            .clip(MaterialTheme.shapes.small)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (settings.showSymbolLabels) {
            if (settings.showFullSymbolLabels) {
                FullSymbolLabel(entry.label, settings.symbolLabelTextSizeSp)
            } else {
                Text(
                    text = entry.label,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = settings.symbolLabelTextSizeSp.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Text(
                text = entry.icon ?: entry.label,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Full-label mode keeps the complete symbol text on one line. The font is
 * reduced against the cell's actual measured width rather than truncating the
 * label or allowing it to wrap into the next row.
 */
@Composable
private fun FullSymbolLabel(
    label: String,
    configuredTextSizeSp: Int
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val baseFontSize = configuredTextSizeSp.coerceAtLeast(MIN_FULL_LABEL_FONT_SIZE_SP).sp
        val characterCount = label.codePointCount(0, label.length).coerceAtLeast(1)
        val widthBoundFontSize = with(density) {
            (maxWidth / characterCount.toFloat() * FULL_LABEL_WIDTH_FACTOR).toSp()
        }
        // TextUnit is not a Comparable value in the Compose API. Compare
        // scalar sp values explicitly, then reconstruct the fitted TextUnit.
        val minimumFontSize = MIN_FULL_LABEL_FONT_SIZE_SP.toFloat()
        val fittedFontSize = minOf(
            baseFontSize.value,
            widthBoundFontSize.value.coerceAtLeast(minimumFontSize)
        ).sp

        Text(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = fittedFontSize,
                fontWeight = FontWeight.Medium
            ),
            softWrap = false,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

private const val MIN_FULL_LABEL_FONT_SIZE_SP = 8
private const val FULL_LABEL_WIDTH_FACTOR = 0.92f
