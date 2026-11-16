/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.compose

/**
 * Synchronous projection for nested-scroll consumption.
 *
 * Nested-scroll callbacks must return consumed pixels synchronously, while the
 * Animatable state is updated by suspend functions. This coordinator advances a
 * projected fraction at callback time so consecutive callbacks never calculate
 * against the same stale Animatable value.
 *
 * The caller must apply each returned [deltaFraction] in callback order and
 * call [reconcile] only after all previously accepted deltas have been applied.
 */
internal class DrawerNestedScrollCoordinator(
    initialFraction: Float,
    rangePx: Float
) {
    private var projectedFraction = initialFraction.requireFraction()
    private var rangePx = rangePx.requireRange()
    private var pendingMutationDelta = 0f
    private var nextMutationDrainId = 0L
    private var activeMutationDrainId: Long? = null

    @Synchronized
    fun consumePreScroll(availableY: Float): Consumption {
        if (!availableY.isFinite() || availableY >= 0f) return Consumption.None
        val expandablePx = (1f - projectedFraction) * rangePx
        val consumedY = -minOf(-availableY, expandablePx)
        return advance(consumedY)
    }

    @Synchronized
    fun consumePostScroll(availableY: Float): Consumption {
        if (!availableY.isFinite() || availableY <= 0f) return Consumption.None
        val collapsiblePx = projectedFraction * rangePx
        val consumedY = minOf(availableY, collapsiblePx)
        return advance(consumedY)
    }

    @Synchronized
    fun projectedFraction(): Float = projectedFraction

    /**
     * Projects a handle drag immediately. Positive pointer movement is
     * downward and therefore collapses the drawer.
     */
    @Synchronized
    fun consumeDragDelta(deltaY: Float): Float {
        if (!deltaY.isFinite() || rangePx <= 0f) return 0f
        val previous = projectedFraction
        projectedFraction = (previous - deltaY / rangePx).coerceIn(0f, 1f)
        return projectedFraction - previous
    }

    /**
     * Coalesces accepted state mutations. Returns true only when the caller
     * must start the single drain worker. The projected position is advanced
     * synchronously by consume methods; this queue only mirrors those deltas
     * into the Animatable-backed state.
     */
    @Synchronized
    fun enqueueMutation(deltaFraction: Float): Long? {
        if (!deltaFraction.isFinite() || deltaFraction == 0f) return null
        pendingMutationDelta += deltaFraction
        activeMutationDrainId?.let { return null }
        val leaseId = ++nextMutationDrainId
        activeMutationDrainId = leaseId
        return leaseId
    }

    /**
     * Returns the coalesced delta for [leaseId]. A stale worker cannot drain a
     * newer worker's queue. Returning null atomically releases an empty lease.
     */
    @Synchronized
    fun takePendingMutation(leaseId: Long): Float? {
        if (activeMutationDrainId != leaseId) return null
        if (pendingMutationDelta == 0f) {
            activeMutationDrainId = null
            return null
        }
        return pendingMutationDelta.also { pendingMutationDelta = 0f }
    }

    /**
     * Releases a worker lease after cancellation or failure. The ID check is
     * essential: a late completion from an old worker must not unlock a newer
     * worker that has already acquired the drain.
     */
    @Synchronized
    fun releaseMutationDrain(leaseId: Long): Boolean {
        if (activeMutationDrainId != leaseId) return false
        activeMutationDrainId = null
        return true
    }

    @Synchronized
    fun hasPendingMutation(): Boolean = pendingMutationDelta != 0f || activeMutationDrainId != null

    /** Update pixel geometry without changing the normalized expansion position. */
    @Synchronized
    fun updateRange(newRangePx: Float) {
        rangePx = newRangePx.requireRange()
    }

    /**
     * Rebase after an external state mutation or after the caller has drained all
     * accepted deltas. Calling this while deltas are pending discards projection.
     */
    @Synchronized
    fun reconcile(actualFraction: Float) {
        projectedFraction = actualFraction.requireFraction()
    }

    private fun advance(consumedY: Float): Consumption {
        if (consumedY == 0f || rangePx <= 0f) return Consumption.None
        // Upward (negative) pixels expand; downward (positive) pixels collapse.
        val deltaFraction = -consumedY / rangePx
        projectedFraction = (projectedFraction + deltaFraction).coerceIn(0f, 1f)
        return Consumption(consumedY, deltaFraction)
    }

    internal data class Consumption(
        val consumedY: Float,
        val deltaFraction: Float
    ) {
        companion object {
            val None = Consumption(0f, 0f)
        }
    }

    private fun Float.requireFraction(): Float {
        require(isFinite() && this in 0f..1f) { "fraction must be finite and within 0..1" }
        return this
    }

    private fun Float.requireRange(): Float {
        require(isFinite() && this >= 0f) { "rangePx must be finite and non-negative" }
        return this
    }
}
