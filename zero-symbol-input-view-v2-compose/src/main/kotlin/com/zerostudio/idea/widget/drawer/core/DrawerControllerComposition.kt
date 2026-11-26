/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.drawer.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * Creates one drawer state engine for the lifetime of the calling Composition.
 *
 * The constructor arguments are initial values: subsequent geometry changes
 * must be delivered through [DrawerStateHolder.updateGeometry], and strategy
 * changes through [DrawerStateHolder.setAnchorStrategy]. This prevents
 * recomposition from silently replacing an active drag or animation.
 *
 * No Activity, Fragment, LifecycleOwner, View, or external coroutine scope is
 * retained by the returned state holder.
 */
@Composable
fun rememberDrawerStateHolder(
    initialInput: DrawerAnchorInput,
    initialStrategy: DrawerAnchorStrategy = ExistingHeightAnchorStrategy(),
    initialAnchorId: String? = null
): DrawerStateHolder = remember {
    DrawerStateHolder(
        initialInput = initialInput,
        initialStrategy = initialStrategy,
        initialAnchorId = initialAnchorId
    )
}

/**
 * Remembers the default controller and its single backing state engine.
 *
 * The latest geometry provider is observed without recreating the controller.
 * Calling [DefaultDrawerController.refreshAnchors] therefore always reads the
 * latest host measurements while preserving the controller's identity and
 * in-flight state.
 */
@Composable
fun rememberDefaultDrawerController(
    initialInput: DrawerAnchorInput,
    anchorInputProvider: () -> DrawerAnchorInput,
    initialStrategy: DrawerAnchorStrategy = ExistingHeightAnchorStrategy(),
    initialAnchorId: String? = null
): DefaultDrawerController {
    val stateHolder = rememberDrawerStateHolder(
        initialInput = initialInput,
        initialStrategy = initialStrategy,
        initialAnchorId = initialAnchorId
    )
    val currentInputProvider by rememberUpdatedState(anchorInputProvider)
    return remember(stateHolder) {
        DefaultDrawerController(stateHolder) { currentInputProvider() }
    }
}
