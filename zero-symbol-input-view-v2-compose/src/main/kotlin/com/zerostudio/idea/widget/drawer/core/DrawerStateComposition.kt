/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.drawer.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Bridges the platform-neutral drawer state engine into a Composition.
 *
 * The state holder itself owns no Compose state, Android View, or lifecycle
 * object. This adapter subscribes while the calling Composition is active and
 * releases the listener when that effect leaves its Composition.
 */
@Composable
fun rememberDrawerStateSnapshot(holder: DrawerStateHolder): DrawerState {
    var snapshot by remember(holder) { mutableStateOf(holder.snapshot) }
    DisposableEffect(holder) {
        val listener: (DrawerState) -> Unit = { snapshot = it }
        holder.addStateListener(listener)
        onDispose { holder.removeStateListener(listener) }
    }
    return snapshot
}
