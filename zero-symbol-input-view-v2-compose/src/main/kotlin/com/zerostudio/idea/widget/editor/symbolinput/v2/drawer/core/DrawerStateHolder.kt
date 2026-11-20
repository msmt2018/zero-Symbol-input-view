/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Composition-observable runtime state for the generic drawer engine.
 *
 * Geometry and anchor resolution are kept independent from layout. A Compose
 * host measures its content, supplies [DrawerAnchorInput], and renders
 * [snapshot]. Gesture modifiers forward pixel deltas to this state holder;
 * animation remains owned by the host's Compose animation scope.
 *
 * The holder retains no Context, View, Activity, Lifecycle, or coroutine scope.
 */
@Stable
class DrawerStateHolder(
    initialInput: DrawerAnchorInput,
    initialStrategy: DrawerAnchorStrategy = ExistingHeightAnchorStrategy(),
    initialAnchorId: String? = null
) {
    private var input: DrawerAnchorInput = initialInput
    private var strategy: DrawerAnchorStrategy = initialStrategy
    private var listeners: Set<(DrawerState) -> Unit> = emptySet()

    private var heightPx by mutableStateOf(0)
    /** Fractional pixel movement retained across successive drag deltas. */
    private var dragRemainderPx = 0f
    private var dragging by mutableStateOf(false)
    private var animating by mutableStateOf(false)
    private var targetAnchorId by mutableStateOf<String?>(null)
    private var animationGeneration = 0L
    private var resolvedAnchors by mutableStateOf(
        DrawerAnchorResolver.resolve(initialStrategy, initialInput)
    )

    var snapshot: DrawerState by mutableStateOf(
        createSnapshot(
            anchors = DrawerAnchorResolver.resolve(initialStrategy, initialInput),
            requestedAnchorId = initialAnchorId
        )
    )
        private set

    init {
        require(initialStrategy.id.isNotBlank()) { "Anchor strategy ID must not be blank" }
        val initialAnchors = resolvedAnchors
        val initial = initialAnchors.firstOrNull { it.id == initialAnchorId }
            ?: initialAnchors.minBy { it.heightPx }
        heightPx = initial.heightPx
        publish()
    }

    /** Strategy currently resolving [anchors]; exposed read-only for controller initialization. */
    val anchorStrategy: DrawerAnchorStrategy get() = strategy

    val currentHeightPx: Int get() = heightPx
    val anchors: List<DrawerAnchor> get() = resolvedAnchors
    val isDragging: Boolean get() = dragging
    val isAnimating: Boolean get() = animating
    val activeAnimationGeneration: Long? get() = animationGeneration.takeIf { targetAnchorId != null }

    fun updateGeometry(newInput: DrawerAnchorInput, preserveExpansionFraction: Boolean = true) {
        val oldMin = resolvedAnchors.minOf { it.heightPx }
        val oldMax = resolvedAnchors.maxOf { it.heightPx }
        val oldFraction = if (oldMax > oldMin) {
            (heightPx - oldMin).toFloat() / (oldMax - oldMin)
        } else 0f

        input = newInput
        resolvedAnchors = DrawerAnchorResolver.resolve(strategy, newInput)
        heightPx = if (preserveExpansionFraction) {
            val newMin = resolvedAnchors.minOf { it.heightPx }
            val newMax = resolvedAnchors.maxOf { it.heightPx }
            (newMin + (newMax - newMin) * oldFraction).toInt()
        } else {
            heightPx.coerceIn(
                resolvedAnchors.minOf { it.heightPx },
                resolvedAnchors.maxOf { it.heightPx }
            )
        }
        dragging = false
        animating = false
        dragRemainderPx = 0f
        targetAnchorId = null
        publish()
    }

    fun setAnchorStrategy(newStrategy: DrawerAnchorStrategy) {
        require(newStrategy.id.isNotBlank()) { "Anchor strategy ID must not be blank" }
        // Equal immutable policies do not change anchor resolution. Avoid
        // resetting an in-flight gesture/animation for redundant host updates.
        if (newStrategy === strategy || newStrategy == strategy) return
        val oldMin = resolvedAnchors.minOf { it.heightPx }
        val oldMax = resolvedAnchors.maxOf { it.heightPx }
        val oldFraction = if (oldMax > oldMin) {
            (heightPx - oldMin).toFloat() / (oldMax - oldMin)
        } else {
            0f
        }

        strategy = newStrategy
        resolvedAnchors = DrawerAnchorResolver.resolve(newStrategy, input)
        val newMin = resolvedAnchors.minOf { it.heightPx }
        val newMax = resolvedAnchors.maxOf { it.heightPx }
        heightPx = (newMin + (newMax - newMin) * oldFraction)
            .toInt()
            .coerceIn(newMin, newMax)
        dragging = false
        animating = false
        dragRemainderPx = 0f
        targetAnchorId = null
        publish()
    }

    fun beginDrag() {
        dragging = true
        animating = false
        dragRemainderPx = 0f
        targetAnchorId = null
        publish()
    }

    /**
     * Applies one nested-scroll delta through the shared state engine.
     *
     * The policy decides ownership; this holder remains the only mutable source
     * of drawer height. The returned remainder must be passed back to Compose's
     * nested-scroll dispatcher so the child/ancestor can consume it.
     */
    fun dispatchNestedScroll(
        availableDeltaY: Float,
        childCanScrollBackward: Boolean,
        phase: DrawerNestedScrollPhase
    ): DrawerNestedScrollResult {
        val minimum = resolvedAnchors.minOf { it.heightPx }
        val maximum = resolvedAnchors.maxOf { it.heightPx }
        val result = DrawerNestedScrollPolicy.resolve(
            DrawerNestedScrollInput(
                availableDeltaY = availableDeltaY,
                currentHeightPx = heightPx.coerceIn(minimum, maximum),
                minimumHeightPx = minimum,
                maximumHeightPx = maximum,
                childCanScrollBackward = childCanScrollBackward,
                phase = phase
            )
        )
        if (result.consumedDeltaY != 0f) {
            dragBy(result.consumedDeltaY)
        }
        return result
    }

    /** Applies finger movement in pixels; upward/negative movement expands. */
    fun dragBy(fingerDeltaY: Float): Int {
        require(fingerDeltaY.isFinite()) { "Drag delta must be finite" }
        if (!dragging) {
            dragging = true
            animating = false
            dragRemainderPx = 0f
            targetAnchorId = null
        }
        val minimum = resolvedAnchors.minOf { it.heightPx }
        val maximum = resolvedAnchors.maxOf { it.heightPx }
        val exactHeight = heightPx.toFloat() - fingerDeltaY + dragRemainderPx
        val nextHeight = exactHeight.toInt().coerceIn(minimum, maximum)
        heightPx = nextHeight
        dragRemainderPx = if (nextHeight == minimum || nextHeight == maximum) {
            0f
        } else {
            exactHeight - nextHeight
        }
        publish()
        return heightPx
    }

    /**
     * Completes a drag and returns the stable anchor selected from the complete
     * anchor set. A fling advances at most one stable stage from the current
     * intermediate height.
     */
    fun endDrag(
        fingerVelocityY: Float,
        minimumFlingVelocityPxPerSecond: Float
    ): DrawerAnchor {
        require(fingerVelocityY.isFinite()) { "Fling velocity must be finite" }
        require(minimumFlingVelocityPxPerSecond.isFinite() &&
            minimumFlingVelocityPxPerSecond >= 0f
        ) { "Minimum fling velocity must be finite and non-negative" }

        val target = DrawerAnchorResolver.flingTarget(
            anchors = resolvedAnchors,
            currentHeightPx = heightPx,
            velocityY = -fingerVelocityY,
            minimumFlingVelocity = minimumFlingVelocityPxPerSecond
        )
        dragging = false
        dragRemainderPx = 0f
        animating = target.heightPx != heightPx
        animationGeneration += 1L
        targetAnchorId = target.id.takeIf { animating }
        publish()
        return target
    }

    /**
     * Cancels an active drag without snapping to an anchor. The continuous
     * height is retained; the host may subsequently request a semantic target.
     */
    fun cancelDrag(): Int {
        dragging = false
        animating = false
        dragRemainderPx = 0f
        targetAnchorId = null
        publish()
        return heightPx
    }

    /**
     * Synchronizes an externally-owned, already-resolved pixel position.
     * Intended for compatibility adapters and geometry restoration, not for
     * per-frame animation (use updateAnimatedHeight for that).
     */
    fun synchronizeHeight(height: Int): Int {
        heightPx = height.coerceIn(
            resolvedAnchors.minOf { it.heightPx },
            resolvedAnchors.maxOf { it.heightPx }
        )
        dragRemainderPx = 0f
        dragging = false
        animating = false
        targetAnchorId = null
        publish()
        return heightPx
    }

    /**
     * Called by the Compose animation host for each animated pixel position.
     * Animation state is derived from the presence of an active semantic target;
     * unsolicited frame updates must not create a phantom transition.
     */
    fun updateAnimatedHeight(height: Int) {
        heightPx = height.coerceIn(
            resolvedAnchors.minOf { it.heightPx },
            resolvedAnchors.maxOf { it.heightPx }
        )
        animating = targetAnchorId != null
        publish()
    }

    fun finishAnimation(anchorId: String, generation: Long): Boolean {
        if (targetAnchorId != anchorId || animationGeneration != generation) return false
        val target = resolvedAnchors.firstOrNull { it.id == anchorId } ?: return false
        heightPx = target.heightPx
        dragRemainderPx = 0f
        dragging = false
        animating = false
        targetAnchorId = null
        animationGeneration += 1L
        publish()
        return true
    }


    /** Selects the most expanded resolved anchor without owning animation execution. */
    fun expandToMaxAnchor(): DrawerAnchor? =
        resolvedAnchors.maxByOrNull { it.heightPx }?.let { moveToAnchor(it.id) }

    /** Selects the most collapsed resolved anchor without owning animation execution. */
    fun collapseToMinAnchor(): DrawerAnchor? =
        resolvedAnchors.minByOrNull { it.heightPx }?.let { moveToAnchor(it.id) }

    /**
     * Selects the adjacent stable anchor in visible-height direction.
     *
     * [expand] advances toward greater visible height; false advances toward
     * lower visible height. The current continuous height is used, so a drag
     * released between anchors does not accidentally skip the nearest stage.
     */
    fun moveToAdjacentAnchor(expand: Boolean): DrawerAnchor? {
        val ordered = resolvedAnchors.sortedBy { it.heightPx }
        val target = if (expand) {
            ordered.firstOrNull { it.heightPx > heightPx }
        } else {
            ordered.lastOrNull { it.heightPx < heightPx }
        } ?: return null
        return moveToAnchor(target.id)
    }

    fun moveToAnchor(anchorId: String): DrawerAnchor? {
        val target = resolvedAnchors.firstOrNull { it.id == anchorId } ?: return null
        animating = target.heightPx != heightPx
        dragging = false
        dragRemainderPx = 0f
        animationGeneration += 1L
        targetAnchorId = target.id.takeIf { animating }
        publish()
        return target
    }

    /**
     * Moves directly to a stable anchor without scheduling host animation.
     * This is the synchronous counterpart to [moveToAnchor].
     */
    fun snapToAnchor(anchorId: String): DrawerAnchor? {
        val target = resolvedAnchors.firstOrNull { it.id == anchorId } ?: return null
        heightPx = target.heightPx
        dragRemainderPx = 0f
        dragging = false
        animating = false
        targetAnchorId = null
        publish()
        return target
    }

    fun addStateListener(listener: (DrawerState) -> Unit) {
        listeners = listeners + listener
        listener(snapshot)
    }

    fun removeStateListener(listener: (DrawerState) -> Unit) {
        listeners = listeners - listener
    }

    private fun publish() {
        snapshot = DrawerState(
            currentAnchorId = DrawerAnchorResolver.nearestAnchor(resolvedAnchors, heightPx).id,
            currentHeightPx = heightPx,
            anchors = resolvedAnchors,
            isDragging = dragging,
            isAnimating = animating,
            targetAnchorId = targetAnchorId
        )
        // A callback may add or remove listeners. Iterate over a stable snapshot
        // so listener mutations cannot invalidate the active dispatch iterator.
        listeners.toList().forEach { it(snapshot) }
    }

    private fun createSnapshot(
        anchors: List<DrawerAnchor>,
        requestedAnchorId: String?
    ): DrawerState {
        val selected = anchors.firstOrNull { it.id == requestedAnchorId }
            ?: anchors.minBy { it.heightPx }
        return DrawerState(
            currentAnchorId = selected.id,
            currentHeightPx = selected.heightPx,
            anchors = anchors
        )
    }
}
