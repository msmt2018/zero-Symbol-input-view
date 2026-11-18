/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

/**
 * Compose adapter for [DrawerStateHolder]'s platform-independent scroll policy.
 *
 * Attach this connection with Modifier.nestedScroll(connection) to the drawer
 * surface. Upward deltas expand the drawer in pre-scroll; downward deltas are
 * offered to the drawer only in post-scroll, after descendants have consumed
 * their available portion. The holder remains the sole mutable height owner.
 *
 * Fling is intentionally not intercepted here. Fling velocity is resolved by
 * the host's drag/settle pipeline so nested-scroll and direct-drag velocity do
 * not compete to settle the same anchor.
 */
class DrawerNestedScrollConnection(
    private val stateHolder: DrawerStateHolder
) : NestedScrollConnection {

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        val result = stateHolder.dispatchNestedScroll(
            availableDeltaY = available.y,
            childCanScrollBackward = false,
            phase = DrawerNestedScrollPhase.PRE_SCROLL
        )
        return Offset(x = 0f, y = result.consumedDeltaY)
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        val result = stateHolder.dispatchNestedScroll(
            availableDeltaY = available.y,
            childCanScrollBackward = false,
            phase = DrawerNestedScrollPhase.POST_SCROLL
        )
        return Offset(x = 0f, y = result.consumedDeltaY)
    }
}
