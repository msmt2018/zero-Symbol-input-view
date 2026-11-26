/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** Remembers an adaptive-window publisher for the calling Composition. */
@Composable
fun rememberAdaptiveWindowController(
    initialSnapshot: WindowAdaptiveInfo
): AdaptiveWindowController = remember {
    AdaptiveWindowController(initialSnapshot)
}

/** Bridges a platform-neutral adaptive snapshot publisher into Composition. */
@Composable
fun rememberAdaptiveWindowSnapshot(
    controller: AdaptiveWindowController
): WindowAdaptiveInfo {
    var snapshot by remember(controller) { mutableStateOf(controller.snapshot) }
    DisposableEffect(controller) {
        val handle = controller.addListener { snapshot = it }
        onDispose { handle.close() }
    }
    return snapshot
}
