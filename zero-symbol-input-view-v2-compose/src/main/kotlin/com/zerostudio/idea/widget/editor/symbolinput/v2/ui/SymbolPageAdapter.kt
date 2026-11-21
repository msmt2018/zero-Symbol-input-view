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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DrawerConfig
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings

/**
 * Composition-native replacement for the former RecyclerView/ListAdapter page.
 *
 * There is deliberately no adapter object, ViewHolder, recycling callback or
 * manual bind/unbind API. LazyVerticalGrid owns item composition and disposal.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SymbolPage(
    group: SymbolGroup,
    config: DrawerConfig,
    settings: SymbolDrawerSettings,
    modifier: Modifier = Modifier,
    onSymbolClick: (SymbolEntry) -> Unit,
    onSymbolLongClick: ((SymbolEntry) -> Unit)? = null
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(config.columns.coerceIn(1, 30)),
        modifier = modifier.fillMaxSize().padding(horizontal = 4.dp),
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
            .heightIn(min = config.cellHeightDp.coerceAtLeast(config.minimumTouchTargetDp).dp)
            .clip(MaterialTheme.shapes.small)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (settings.showSymbolLabels) {
            Text(
                text = entry.label,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = settings.symbolLabelTextSizeSp.sp,
                    fontWeight = if (settings.showFullSymbolLabels) FontWeight.Medium else FontWeight.Normal
                ),
                maxLines = if (settings.showFullSymbolLabels) 3 else 2,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Text(
                text = entry.icon ?: entry.label,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
