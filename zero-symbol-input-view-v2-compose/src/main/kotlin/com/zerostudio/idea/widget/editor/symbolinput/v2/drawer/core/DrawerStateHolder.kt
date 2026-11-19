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
    private var dragging by mutableStateOf(false)
    private var animating by mutableStateOf(false)
    private var targetAnchorId by mutableStateOf<String?>(null)
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

    val currentHeightPx: Int get() = heightPx
    val anchors: List<DrawerAnchor> get() = resolvedAnchors
    val isDragging: Boolean get() = dragging
    val isAnimating: Boolean get() = animating

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
        targetAnchorId = null
        publish()
    }

    fun setAnchorStrategy(newStrategy: DrawerAnchorStrategy) {
        require(newStrategy.id.isNotBlank()) { "Anchor strategy ID must not be blank" }
        strategy = newStrategy
        resolvedAnchors = DrawerAnchorResolver.resolve(newStrategy, input)
        heightPx = heightPx.coerceIn(
            resolvedAnchors.minOf { it.heightPx },
            resolvedAnchors.maxOf { it.heightPx }
        )
        dragging = false
        animating = false
        targetAnchorId = null
        publish()
    }

    fun beginDrag() {
        dragging = true
        animating = false
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
            targetAnchorId = null
        }
        heightPx = (heightPx - fingerDeltaY).toInt().coerceIn(
            resolvedAnchors.minOf { it.heightPx },
            resolvedAnchors.maxOf { it.heightPx }
        )
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
        animating = target.heightPx != heightPx
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
        dragging = false
        animating = false
        targetAnchorId = null
        publish()
        return heightPx
    }

    /** Called by the Compose animation host for each animated pixel position. */
    fun updateAnimatedHeight(height: Int) {
        heightPx = height.coerceIn(
            resolvedAnchors.minOf { it.heightPx },
            resolvedAnchors.maxOf { it.heightPx }
        )
        animating = true
        publish()
    }

    fun finishAnimation(anchorId: String) {
        val target = resolvedAnchors.firstOrNull { it.id == anchorId }
            ?: return
        heightPx = target.heightPx
        dragging = false
        animating = false
        targetAnchorId = null
        publish()
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
        listeners.forEach { it(snapshot) }
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
