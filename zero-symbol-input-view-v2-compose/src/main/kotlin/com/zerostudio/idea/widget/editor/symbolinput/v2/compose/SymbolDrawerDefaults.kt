/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 * Author: android_zero · https://github.com/msmt2018
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings

@Composable
fun rememberSymbolDrawerState(
    initiallyExpanded: Boolean = false,
    initialSelectedGroupId: String? = null
): SymbolDrawerState = rememberSaveable(saver = SymbolDrawerState.Saver) {
    SymbolDrawerState(initiallyExpanded, initialSelectedGroupId)
}

object SymbolDrawerDefaults {
    fun settings(): SymbolDrawerSettings = SymbolDrawerSettings()
}
