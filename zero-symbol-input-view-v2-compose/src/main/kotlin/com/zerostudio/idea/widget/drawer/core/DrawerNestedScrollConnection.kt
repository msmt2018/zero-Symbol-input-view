/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.drawer.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

/**
 * Compose adapter for [DrawerStateHolder]'s platform-independent scroll policy.
 *
 * Attach this connection with Modifier.nestedScroll(connection) to the drawer
 * surface. Upward deltas expand the drawer in pre-scroll; downward deltas are
 * offered to the drawer only in post-scroll, after descendants have consumed
 * their available portion. The holder remains the sole mutable height owner.
 *
 * Fling settlement happens in [onPostFling], after descendants have had the
 * opportunity to consume their velocity. The resulting target is published by
 * [DrawerStateHolder]; the Compose host observes targetAnchorId and performs
 * the actual animation. This adapter never creates a coroutine scope or runs
 * animations itself.
 *
 * A host that uses a separate direct-drag pointer-input pipeline must call
 * DrawerStateHolder.endDrag itself when that gesture ends; nested-scroll fling
 * callbacks only complete gestures dispatched through the nested-scroll chain.
 */
class DrawerNestedScrollConnection(
    private val stateHolder: DrawerStateHolder,
    private val minimumFlingVelocityPxPerSecond: Float = 400f
) : NestedScrollConnection {

    /**
     * Tracks whether this connection actually consumed scroll in the current
     * nested-scroll gesture. The holder may also be driven by a direct pointer
     * gesture, so its global dragging flag alone is not proof that this
     * connection owns the fling callback.
     */
    private var ownsNestedScrollGesture = false

    init {
        require(minimumFlingVelocityPxPerSecond.isFinite() &&
            minimumFlingVelocityPxPerSecond >= 0f
        ) { "Minimum fling velocity must be finite and non-negative" }
    }

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        // Programmatic descendant scrolling must not move the drawer. This
        // connection participates only in user-driven nested gestures.
        if (source != NestedScrollSource.UserInput) return Offset.Zero
        val result = stateHolder.dispatchNestedScroll(
            availableDeltaY = available.y,
            childCanScrollBackward = false,
            phase = DrawerNestedScrollPhase.PRE_SCROLL
        )
        if (result.consumedDeltaY != 0f) ownsNestedScrollGesture = true
        return Offset(x = 0f, y = result.consumedDeltaY)
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        // Side-effect scroll deltas (for example animateScrollToItem) belong
        // to the descendant/ancestor scroll chain, not drawer gestures.
        if (source != NestedScrollSource.UserInput) return Offset.Zero
        val result = stateHolder.dispatchNestedScroll(
            availableDeltaY = available.y,
            childCanScrollBackward = false,
            phase = DrawerNestedScrollPhase.POST_SCROLL
        )
        if (result.consumedDeltaY != 0f) ownsNestedScrollGesture = true
        return Offset(x = 0f, y = result.consumedDeltaY)
    }

    /**
     * Clears gesture ownership when the host's pointer/nested-scroll gesture
     * is cancelled before a fling callback is dispatched.
     *
     * This method deliberately does not cancel the shared holder's drag:
     * another input pipeline may currently own that holder. The host that
     * owns the cancelled gesture must coordinate [DrawerStateHolder.cancelDrag]
     * when appropriate.
     */
    fun cancelGesture() {
        ownsNestedScrollGesture = false
    }

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity
    ): Velocity {
        val ownsGesture = ownsNestedScrollGesture
        ownsNestedScrollGesture = false
        if (!ownsGesture || !stateHolder.isDragging) return Velocity.Zero

        val heightBeforeSettle = stateHolder.currentHeightPx
        // Nested-scroll velocity is expressed in scroll/content coordinates,
        // opposite to the finger velocity consumed by DrawerStateHolder and the
        // legacy View interaction controller. Convert exactly once here:
        // an upward finger fling (negative) is a positive scroll velocity and
        // must expand; a downward finger fling must collapse.
        val target = stateHolder.endDrag(
            fingerVelocityY = -available.y,
            minimumFlingVelocityPxPerSecond = minimumFlingVelocityPxPerSecond
        )

        // Consume only the vertical velocity that actually caused a drawer
        // transition. Horizontal velocity always remains available to ancestors.
        return if (target.heightPx != heightBeforeSettle) {
            Velocity(x = 0f, y = available.y)
        } else {
            Velocity.Zero
        }
    }
}
