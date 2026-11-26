/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.zerostudio.idea.widget.editor.symbolinput.v2.R
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolDrawerDefaultGroups
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup

/**
 * Resolves the localized default group title at the Compose presentation edge,
 * then memoizes the immutable domain catalogue for the active composition.
 */
@Composable
fun rememberSymbolDrawerDefaultGroups(): List<SymbolGroup> {
    val groupTitle = stringResource(R.string.symbol_drawer_default_group_common)
    return remember(groupTitle) { SymbolDrawerDefaultGroups.createGroups(groupTitle) }
}
