/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.drawer.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** Bridges the platform-neutral navigation controller into Composition. */
@Composable
fun rememberDrawerNavigationSnapshot(
    controller: DrawerNavigationController
): DrawerNavigationSnapshot {
    var snapshot by remember(controller) { mutableStateOf(controller.snapshot()) }
    DisposableEffect(controller) {
        val listener = DrawerNavigationController.Listener { next, _ -> snapshot = next }
        controller.addListener(listener)
        onDispose { controller.removeListener(listener) }
    }
    return snapshot
}
