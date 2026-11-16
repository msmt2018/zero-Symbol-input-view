/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 * Author: android_zero · https://github.com/msmt2018
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings

/** Compose-native renderer extension; unlike the View API it never creates or retains an Android View. */
interface SymbolDrawerItemRenderer {
    val id: String
    val priority: Int get() = 0
    fun accepts(symbol: SymbolEntry): Boolean = true

    @Composable
    fun Content(
        symbol: SymbolEntry,
        selected: Boolean,
        modifier: Modifier,
        onClick: () -> Unit,
        onLongClick: () -> Unit
    )
}

/** Compose-native group indicator contribution. */
interface SymbolDrawerGroupIndicator {
    val id: String
    val priority: Int get() = 0

    @Composable
    fun Content(
        groups: List<SymbolGroup>,
        selectedIndex: Int,
        onSelect: (Int) -> Unit,
        modifier: Modifier
    )
}

/** Lifecycle-independent immutable Compose extension snapshot. */
@Immutable
data class ComposeSymbolDrawerExtensions(
    val itemRenderers: List<SymbolDrawerItemRenderer> = emptyList(),
    val groupIndicators: List<SymbolDrawerGroupIndicator> = emptyList()
) {
    init {
        require(itemRenderers.map { it.id }.distinct().size == itemRenderers.size)
        require(groupIndicators.map { it.id }.distinct().size == groupIndicators.size)
    }
}

