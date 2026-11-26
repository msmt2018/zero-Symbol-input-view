/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.drawer.core

/**
 * Pure nested-scroll arbitration for a vertically oriented bottom drawer.
 *
 * The policy uses Compose's nested-scroll sign convention: negative deltas move
 * content upward and positive deltas move it downward. It deliberately has no
 * Compose dependency so gesture ownership can be unit-tested independently.
 */
enum class DrawerNestedScrollPhase {
    PRE_SCROLL,
    POST_SCROLL
}

/**
 * Immutable facts needed to decide whether the drawer or its child owns a
 * portion of a nested-scroll delta.
 *
 * [childCanScrollBackward] is retained as diagnostic input for hosts. Actual
 * child consumption is represented by the delta passed to POST_SCROLL; the
 * policy therefore does not infer consumption from a potentially stale flag.
 */
data class DrawerNestedScrollInput(
    val availableDeltaY: Float,
    val currentHeightPx: Int,
    val minimumHeightPx: Int,
    val maximumHeightPx: Int,
    val childCanScrollBackward: Boolean,
    val phase: DrawerNestedScrollPhase
) {
    init {
        require(availableDeltaY.isFinite()) { "Nested-scroll delta must be finite" }
        require(minimumHeightPx >= 0)
        require(maximumHeightPx >= minimumHeightPx)
        require(currentHeightPx in minimumHeightPx..maximumHeightPx)
    }
}

/** Result preserves the sign of the incoming delta and never over-consumes it. */
data class DrawerNestedScrollResult(
    val consumedDeltaY: Float,
    val remainingDeltaY: Float
) {
    init {
        require(consumedDeltaY.isFinite() && remainingDeltaY.isFinite())
    }
}

/**
 * Decides nested-scroll ownership without mutating drawer or child state.
 *
 * Policy:
 * - Upward motion expands the drawer before the child while the drawer is not
 *   fully expanded.
 * - Once fully expanded, upward motion belongs to the child.
 * - PRE_SCROLL: upward motion expands the drawer before the child; downward
 *   motion is never consumed, so the child gets first refusal while collapsing.
 * - POST_SCROLL: only downward remainder is offered to the drawer. Compose has
 *   already let the child consume its share, so this remainder represents motion
 *   the child could not use (normally because it reached its start boundary).
 * - Upward remainder is not consumed in POST_SCROLL; expansion belongs to the
 *   pre-scroll phase and must not be delayed until after child scrolling.
 */
object DrawerNestedScrollPolicy {
    @JvmStatic
    fun resolve(input: DrawerNestedScrollInput): DrawerNestedScrollResult {
        val delta = input.availableDeltaY
        if (delta == 0f || input.minimumHeightPx == input.maximumHeightPx) {
            return DrawerNestedScrollResult(0f, delta)
        }

        val canExpand = input.currentHeightPx < input.maximumHeightPx
        val canCollapse = input.currentHeightPx > input.minimumHeightPx

        // Nested-scroll ordering is directional and phase-sensitive. The child
        // must get the first opportunity to consume downward motion; the drawer
        // only receives the unconsumed remainder in POST_SCROLL.
        val shouldConsume = when (input.phase) {
            DrawerNestedScrollPhase.PRE_SCROLL -> delta < 0f && canExpand
            DrawerNestedScrollPhase.POST_SCROLL -> delta > 0f && canCollapse
        }

        if (!shouldConsume) return DrawerNestedScrollResult(0f, delta)

        val availableHeight = if (delta < 0f) {
            (input.maximumHeightPx - input.currentHeightPx).toFloat()
        } else {
            (input.currentHeightPx - input.minimumHeightPx).toFloat()
        }
        val magnitude = minOf(kotlin.math.abs(delta), availableHeight)
        val consumed = if (delta < 0f) -magnitude else magnitude
        return DrawerNestedScrollResult(consumed, delta - consumed)
    }
}
