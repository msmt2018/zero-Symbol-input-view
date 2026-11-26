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

package com.zerostudio.idea.widget.editor.symbolinput.v2.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import com.zerostudio.idea.widget.editor.symbolinput.v2.compose.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.GroupIndicatorStyle
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Compose-native group navigation indicator.
 *
 * All 40 persisted V2 indicator style identifiers remain stable. They are
 * rendered with Compose Canvas primitives and never allocate Android Views.
 */
@Composable
fun SymbolGroupIndicator(
    groups: List<SymbolGroup>,
    selectedIndex: Int,
    onGroupSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    settings: SymbolDrawerSettings = SymbolDrawerSettings(),
    onSettingsClick: (() -> Unit)? = null,
    onManagerClick: (() -> Unit)? = null
) {
    val selected = selectedIndex.coerceIn(0, (groups.size - 1).coerceAtLeast(0))
    val primaryColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f)
    val textMode = settings.indicatorStyle == GroupIndicatorStyle.TEXT ||
        settings.indicatorStyle == GroupIndicatorStyle.TEXT_TABS
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (textMode) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(groups, key = { _, group -> group.id }) { index, group ->
                    Text(
                        text = group.title,
                        modifier = Modifier.clickable { onGroupSelected(index) },
                        color = if (index == selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(groups.size) {
                        detectTapGestures { point ->
                            if (groups.isNotEmpty() && size.width > 0) {
                                val index = (point.x / size.width * groups.size)
                                    .toInt().coerceIn(0, groups.lastIndex)
                                onGroupSelected(index)
                            }
                        }
                    }
            ) {
                val count = groups.size
                if (count > 0) {
                    val primary = primaryColor
                    val inactive = inactiveColor
                    val centerY = size.height / 2f
                    val step = size.width / count
                    val style = settings.indicatorStyle
                    val selectedX = step * (selected + 0.5f)
                    val lineStart = Offset(step * 0.5f, centerY)
                    val lineEnd = Offset(size.width - step * 0.5f, centerY)

                    when (style) {
                        GroupIndicatorStyle.UNDERLINE,
                        GroupIndicatorStyle.PILL,
                        GroupIndicatorStyle.SEGMENTED_TRACK,
                        GroupIndicatorStyle.PROGRESS_LINE,
                        GroupIndicatorStyle.DOUBLE_TRACK,
                        GroupIndicatorStyle.SPRING_SLIDER,
                        GroupIndicatorStyle.MORPHING_BAR -> {
                            drawLine(inactive.copy(alpha = 0.25f), lineStart, lineEnd, 3.dp.toPx(), StrokeCap.Round)
                            val width = (step * 0.72f).coerceAtLeast(4.dp.toPx())
                            drawRoundRect(
                                primary,
                                topLeft = Offset(selectedX - width / 2f, centerY - 3.dp.toPx()),
                                size = Size(width, 6.dp.toPx()),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                            )
                            if (style == GroupIndicatorStyle.DOUBLE_TRACK) {
                                drawLine(primary.copy(alpha = 0.45f), Offset(step * 0.5f, centerY + 7.dp.toPx()), Offset(size.width - step * 0.5f, centerY + 7.dp.toPx()), 1.dp.toPx())
                            }
                        }
                        GroupIndicatorStyle.RADIAL_RING,
                        GroupIndicatorStyle.HUD_ARC,
                        GroupIndicatorStyle.RADAR_SWEEP,
                        GroupIndicatorStyle.CAMERA_DIAL,
                        GroupIndicatorStyle.WHEEL_ROLLER -> {
                            val radius = (size.height * 0.34f).coerceAtLeast(3.dp.toPx())
                            for (i in 0 until count) {
                                val angle = -90f + 360f * i / count
                                val radians = angle * PI.toFloat() / 180f
                                val x = selectedX + cos(radians) * radius
                                val y = centerY + sin(radians) * radius
                                drawCircle(if (i == selected) primary else inactive, if (i == selected) 3.dp.toPx() else 1.8.dp.toPx(), Offset(x, y))
                            }
                            drawArc(
                                color = primary.copy(alpha = 0.75f),
                                startAngle = -90f,
                                sweepAngle = 360f * (selected + 1) / count,
                                useCenter = false,
                                topLeft = Offset(selectedX - radius - 4.dp.toPx(), centerY - radius - 4.dp.toPx()),
                                size = Size((radius + 4.dp.toPx()) * 2, (radius + 4.dp.toPx()) * 2),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                        GroupIndicatorStyle.VERTICAL_RAIL -> {
                            for (i in 0 until count) {
                                val y = size.height * (i + 0.5f) / count
                                drawLine(if (i == selected) primary else inactive, Offset(size.width * 0.5f, y - 2.dp.toPx()), Offset(size.width * 0.5f, y + 2.dp.toPx()), if (i == selected) 4.dp.toPx() else 2.dp.toPx(), StrokeCap.Round)
                            }
                        }
                        else -> {
                            drawLine(inactive.copy(alpha = 0.22f), lineStart, lineEnd, 1.dp.toPx(), StrokeCap.Round)
                            for (i in 0 until count) {
                                val x = step * (i + 0.5f)
                                val active = i == selected
                                val radius = when (style) {
                                    GroupIndicatorStyle.GLOWING_NODES, GroupIndicatorStyle.PULSE_DOTS,
                                    GroupIndicatorStyle.ORBIT_DOTS, GroupIndicatorStyle.ARC_NODES -> if (active) 5.dp.toPx() else 2.dp.toPx()
                                    GroupIndicatorStyle.NUMBER_BADGES, GroupIndicatorStyle.DIGITAL_COUNTER,
                                    GroupIndicatorStyle.STEPPER -> if (active) 5.dp.toPx() else 3.dp.toPx()
                                    GroupIndicatorStyle.HEXAGON_NODES, GroupIndicatorStyle.DIAMOND_MARKERS,
                                    GroupIndicatorStyle.SQUARE_TICKS -> if (active) 4.dp.toPx() else 2.5.dp.toPx()
                                    else -> if (active) 4.5.dp.toPx() else 2.dp.toPx()
                                }
                                val color = if (active) primary else inactive
                                when (style) {
                                    GroupIndicatorStyle.MINIMAL_DASHES,
                                    GroupIndicatorStyle.VERNIER_TICKS,
                                    GroupIndicatorStyle.MECHANICAL_RULER -> drawLine(color, Offset(x, centerY - radius * 1.7f), Offset(x, centerY + radius * 1.7f), if (active) 2.dp.toPx() else 1.dp.toPx(), StrokeCap.Round)
                                    GroupIndicatorStyle.SQUARE_TICKS,
                                    GroupIndicatorStyle.STACKED_CARDS,
                                    GroupIndicatorStyle.THUMBNAIL_STRIP,
                                    GroupIndicatorStyle.MINI_PREVIEW -> drawRect(color, Offset(x - radius, centerY - radius), Size(radius * 2, radius * 2))
                                    GroupIndicatorStyle.DIAMOND_MARKERS -> {
                                        val path = Path().apply { moveTo(x, centerY-radius); lineTo(x+radius,centerY); lineTo(x,centerY+radius); lineTo(x-radius,centerY); close() }
                                        drawPath(path, color)
                                    }
                                    GroupIndicatorStyle.TRIANGLE_POINTER -> {
                                        val path = Path().apply { moveTo(x, centerY-radius); lineTo(x+radius,centerY+radius); lineTo(x-radius,centerY+radius); close() }
                                        drawPath(path, color)
                                    }
                                    GroupIndicatorStyle.HEXAGON_NODES -> {
                                        val path = Path()
                                        for (corner in 0..5) {
                                            val a = (PI / 3.0 * corner - PI / 6.0).toFloat()
                                            val px = x + cos(a) * radius
                                            val py = centerY + sin(a) * radius
                                            if (corner == 0) path.moveTo(px, py) else path.lineTo(px, py)
                                        }
                                        path.close()
                                        drawPath(path, color)
                                    }
                                    GroupIndicatorStyle.ELASTIC_WORM,
                                    GroupIndicatorStyle.SPRING_BLOB,
                                    GroupIndicatorStyle.LIQUID_MERGE,
                                    GroupIndicatorStyle.GOOEY_METABALL,
                                    GroupIndicatorStyle.ELASTIC_WAVE,
                                    GroupIndicatorStyle.SINE_WAVE,
                                    GroupIndicatorStyle.BOUNCE_TRACK,
                                    GroupIndicatorStyle.FLUID_TRAIL -> {
                                        val distance = (i - selected).toFloat()
                                        val y = centerY + sin(distance * 0.7f) * if (active) 0f else 3.dp.toPx()
                                        drawCircle(color, radius * if (active) 1.2f else 0.8f, Offset(x, y))
                                    }
                                    else -> drawCircle(color, radius, Offset(x, centerY))
                                }
                            }
                        }
                    }
                }
            }
        }
        if (onManagerClick != null) {
            TextButton(onClick = onManagerClick) {
                Text(stringResource(R.string.compose_manager_title))
            }
        }
        if (onSettingsClick != null) {
            Text(
                text = "⚙",
                modifier = Modifier.clickable(onClick = onSettingsClick),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
