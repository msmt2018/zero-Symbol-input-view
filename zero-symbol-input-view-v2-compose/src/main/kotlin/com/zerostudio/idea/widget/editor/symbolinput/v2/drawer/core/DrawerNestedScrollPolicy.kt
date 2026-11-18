/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core

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
 * [childCanScrollBackward] means the child can consume a positive/downward
 * gesture by scrolling toward its start (for example, a list is not at the top).
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
 * - Downward motion belongs to a scrollable child first.
 * - At the child's start boundary, downward motion collapses the drawer.
 * - POST_SCROLL handles deltas left by the child using the same boundaries.
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

        val drawerOwnsDelta = when {
            delta < 0f -> canExpand
            delta > 0f -> canCollapse && !input.childCanScrollBackward
            else -> false
        }

        // PRE_SCROLL gives the drawer first refusal for expansion. For collapse,
        // it only participates at the child's start boundary. POST_SCROLL is
        // deliberately symmetric for any delta the child did not consume.
        val shouldConsume = when (input.phase) {
            DrawerNestedScrollPhase.PRE_SCROLL -> drawerOwnsDelta
            DrawerNestedScrollPhase.POST_SCROLL -> drawerOwnsDelta
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
