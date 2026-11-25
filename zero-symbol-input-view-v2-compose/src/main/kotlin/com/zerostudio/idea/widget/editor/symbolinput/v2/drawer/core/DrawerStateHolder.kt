/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core


/**
 * Listener-observable runtime state for the platform-neutral drawer engine.
 *
 * Geometry and anchor resolution are kept independent from layout. A Compose
 * host measures its content, supplies [DrawerAnchorInput], observes immutable
 * snapshots through [addStateListener], and renders them. Gesture modifiers
 * forward pixel deltas to this state holder; animation remains owned by the
 * host's Compose animation scope.
 *
 * The holder retains no Compose Runtime state, Context, View, Activity,
 * Lifecycle, or coroutine scope.
 *
 * All mutations and listener registration/removal must be serialized by the
 * caller on one thread. This holder is intentionally not a cross-thread
 * synchronization primitive: its published snapshots and associated plain
 * Kotlin transition fields form one logical state machine. Compose hosts
 * should route commands, gesture deltas and animation frames through their
 * composition/UI dispatcher. Listener callbacks run synchronously on the
 * publishing thread; reentrant publications are queued until the current
 * snapshot has been delivered to every listener.
 */
class DrawerStateHolder(
    initialInput: DrawerAnchorInput,
    initialStrategy: DrawerAnchorStrategy = ExistingHeightAnchorStrategy(),
    initialAnchorId: String? = null
) {
    private var input: DrawerAnchorInput = initialInput
    private var strategy: DrawerAnchorStrategy = initialStrategy
    private val statePublisher = SynchronousSnapshotPublisher<DrawerState>()

    private var heightPx = 0
    /** Fractional pixel movement retained across successive drag deltas. */
    private var dragRemainderPx = 0f
    private var dragging = false
    private var animating = false
    private var targetAnchorId: String? = null
    private var animationGeneration = 0L
    private var resolvedAnchors = DrawerAnchorResolver.resolve(initialStrategy, initialInput)

    var snapshot: DrawerState = createSnapshot(
        anchors = resolvedAnchors,
        requestedAnchorId = initialAnchorId
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
        // Repeated layout observations with identical measurements are no-op
        // updates. They must not cancel an active drag or animation.
        if (newInput == input) return
        val oldMin = resolvedAnchors.minOf { it.heightPx }
        val oldMax = resolvedAnchors.maxOf { it.heightPx }
        val oldFraction = if (oldMax > oldMin) {
            ((heightPx - oldMin).toFloat() / (oldMax - oldMin)).coerceIn(0f, 1f)
        } else 0f

        val retainedTargetId = targetAnchorId.takeIf { animating }
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
        dragRemainderPx = 0f
        if (retainedTargetId != null &&
            resolvedAnchors.any { it.id == retainedTargetId }
        ) {
            // Retarget the active semantic destination to its newly resolved
            // geometry. Incrementing the generation rejects any queued frame
            // from the pre-resize animation while keeping the transition alive.
            animationGeneration += 1L
            targetAnchorId = retainedTargetId
            animating = true
        } else {
            invalidateAnimation()
        }
        // A resize during a gesture must not silently terminate ownership.
        // The next drag delta continues against the newly resolved bounds.
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
            ((heightPx - oldMin).toFloat() / (oldMax - oldMin)).coerceIn(0f, 1f)
        } else {
            0f
        }

        val retainedTargetId = targetAnchorId.takeIf { animating }
        strategy = newStrategy
        resolvedAnchors = DrawerAnchorResolver.resolve(newStrategy, input)
        val newMin = resolvedAnchors.minOf { it.heightPx }
        val newMax = resolvedAnchors.maxOf { it.heightPx }
        heightPx = (newMin + (newMax - newMin) * oldFraction)
            .toInt()
            .coerceIn(newMin, newMax)
        dragRemainderPx = 0f
        if (retainedTargetId != null &&
            resolvedAnchors.any { it.id == retainedTargetId }
        ) {
            animationGeneration += 1L
            targetAnchorId = retainedTargetId
            animating = true
        } else {
            invalidateAnimation()
        }
        publish()
    }

    fun beginDrag() {
        invalidateAnimation()
        dragging = true
        dragRemainderPx = 0f
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
            invalidateAnimation()
            dragging = true
            dragRemainderPx = 0f
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
        invalidateAnimation()
        dragging = false
        dragRemainderPx = 0f
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
        invalidateAnimation()
        dragging = false
        publish()
        return heightPx
    }

    /**
     * Called by the Compose animation host for each animated pixel position.
     * Animation state is derived from the presence of an active semantic target;
     * unsolicited frame updates must not create a phantom transition.
     */
    fun updateAnimatedHeight(height: Int, generation: Long): Boolean {
        // Every animation frame must prove ownership, not only its completion
        // callback. A cancelled animation can still dispatch a queued frame.
        if (targetAnchorId == null || animationGeneration != generation) return false
        heightPx = height.coerceIn(
            resolvedAnchors.minOf { it.heightPx },
            resolvedAnchors.maxOf { it.heightPx }
        )
        animating = true
        publish()
        return true
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

        // Repeated semantic commands are common when a Compose host re-emits
        // state-driven intents. Do not restart an animation already travelling
        // to this exact target, or publish a redundant stable snapshot.
        if (animating && targetAnchorId == target.id) return target
        if (!dragging && !animating && heightPx == target.heightPx) return target

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
        invalidateAnimation()
        dragging = false
        publish()
        return target
    }

    fun addStateListener(listener: (DrawerState) -> Unit) {
        statePublisher.addListener(listener, snapshot)
    }

    fun removeStateListener(listener: (DrawerState) -> Unit) {
        statePublisher.removeListener(listener)
    }

    /** Invalidates every callback captured by the previous animation. */
    private fun invalidateAnimation() {
        animationGeneration += 1L
        animating = false
        targetAnchorId = null
    }

    private fun publish() {
        val published = DrawerState(
            currentAnchorId = DrawerAnchorResolver.nearestAnchor(resolvedAnchors, heightPx).id,
            currentHeightPx = heightPx,
            anchors = resolvedAnchors,
            isDragging = dragging,
            isAnimating = animating,
            targetAnchorId = targetAnchorId
        )
        snapshot = published
        // The shared publisher preserves synchronous ordering and safely queues
        // reentrant mutations until every observer receives this snapshot.
        statePublisher.publish(published)
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
