/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 * Author: android_zero · https://github.com/msmt2018
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Material 3 window-width breakpoints used by the symbol drawer host. */
enum class SymbolDrawerWindowWidthClass { COMPACT, MEDIUM, EXPANDED }

enum class SymbolDrawerDockMode { AUTO, BOTTOM, START, END }

fun resolveSymbolDrawerWindowWidthClass(width: Dp): SymbolDrawerWindowWidthClass = when {
    width < 600.dp -> SymbolDrawerWindowWidthClass.COMPACT
    width < 840.dp -> SymbolDrawerWindowWidthClass.MEDIUM
    else -> SymbolDrawerWindowWidthClass.EXPANDED
}

/**
 * Places the drawer as a bottom sheet on compact/medium windows and as a side
 * rail on expanded windows. Hosts provide editor/content and the drawer surface.
 */
@Composable
fun AdaptiveSymbolDrawerLayout(
    modifier: Modifier = Modifier,
    dockMode: SymbolDrawerDockMode = SymbolDrawerDockMode.AUTO,
    sideWidth: Dp = 360.dp,
    content: @Composable () -> Unit,
    drawer: @Composable () -> Unit
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val widthClass = resolveSymbolDrawerWindowWidthClass(maxWidth)
        val useSideDock = when (dockMode) {
            SymbolDrawerDockMode.BOTTOM -> false
            SymbolDrawerDockMode.START, SymbolDrawerDockMode.END -> true
            SymbolDrawerDockMode.AUTO -> widthClass == SymbolDrawerWindowWidthClass.EXPANDED
        }
        if (useSideDock) {
            val resolvedWidth = sideWidth.coerceAtMost(maxWidth * 0.45f)
            Row(Modifier.fillMaxSize()) {
                if (dockMode == SymbolDrawerDockMode.START) {
                    Box(Modifier.width(resolvedWidth)) { drawer() }
                    Box(Modifier.weight(1f).fillMaxSize()) { content() }
                } else {
                    Box(Modifier.weight(1f).fillMaxSize()) { content() }
                    Box(Modifier.width(resolvedWidth)) { drawer() }
                }
            }
        } else {
            Box(Modifier.fillMaxSize()) {
                content()
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) { drawer() }
            }
        }
    }
}
