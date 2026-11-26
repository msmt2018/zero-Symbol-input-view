/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.drawer.core


/**
 * Deprecated compatibility facade for the former two-bound interaction API.
 *
 * All runtime state is delegated to [DrawerStateHolder]. The stable annotation
 * exposes the holder-backed observable getters to Compose callers without adding
 * a second state source. This class intentionally
 * contains no independent height, bounds, or dragging state, preventing legacy
 * integrations from creating a second source of truth beside the anchor engine.
 *
 * New integrations should use DrawerStateHolder directly so intermediate anchors,
 * target-anchor identity, nested scrolling and semantic navigation remain available.
 */
@Deprecated(
    message = "Use DrawerStateHolder with DrawerAnchorStrategy; this facade is retained for source compatibility.",
    replaceWith = ReplaceWith("DrawerStateHolder")
)
class DrawerInteractionController(
    minimumHeightPx: Int,
    maximumHeightPx: Int,
    initialHeightPx: Int = minimumHeightPx
) {
    private val holder: DrawerStateHolder

    init {
        require(minimumHeightPx >= 0)
        require(maximumHeightPx >= minimumHeightPx)
        val input = DrawerAnchorInput(
            minHeightPx = minimumHeightPx,
            maxHeightPx = maximumHeightPx,
            viewportHeightPx = maximumHeightPx,
            contentHeightPx = maximumHeightPx,
            existingExpandedHeightPx = maximumHeightPx
        )
        holder = DrawerStateHolder(
            initialInput = input,
            initialStrategy = ExistingHeightAnchorStrategy(),
            initialAnchorId = if (initialHeightPx == maximumHeightPx) "expanded" else "peek"
        )
        holder.synchronizeHeight(initialHeightPx)
    }

    val minimumHeightPx: Int
        get() = holder.anchors.minOf { it.heightPx }

    val maximumHeightPx: Int
        get() = holder.anchors.maxOf { it.heightPx }

    val currentHeightPx: Int
        get() = holder.currentHeightPx

    val isDragging: Boolean
        get() = holder.isDragging

    val expansionFraction: Float
        get() = holder.snapshot.expansionFraction

    fun updateBounds(
        minimumHeightPx: Int,
        maximumHeightPx: Int,
        preserveExpansionFraction: Boolean = true
    ): Int {
        require(minimumHeightPx >= 0)
        require(maximumHeightPx >= minimumHeightPx)
        val nextInput = DrawerAnchorInput(
            minHeightPx = minimumHeightPx,
            maxHeightPx = maximumHeightPx,
            viewportHeightPx = maximumHeightPx,
            contentHeightPx = maximumHeightPx,
            existingExpandedHeightPx = maximumHeightPx
        )
        holder.updateGeometry(nextInput, preserveExpansionFraction)
        return holder.currentHeightPx
    }

    fun beginDrag() {
        holder.beginDrag()
    }

    /** Applies a finger delta in pixels; negative deltas expand a bottom sheet. */
    fun dragBy(deltaPx: Float): Int = holder.dragBy(deltaPx)

    /**
     * Ends a drag and returns the selected stable height. With this legacy
     * two-anchor facade, a qualifying fling resolves to the corresponding bound.
     */
    fun endDrag(
        fingerVelocityY: Float,
        minimumFlingVelocityPxPerSecond: Float
    ): Int = holder.endDrag(
        fingerVelocityY = fingerVelocityY,
        minimumFlingVelocityPxPerSecond = minimumFlingVelocityPxPerSecond
    ).heightPx

    fun cancelDrag(): Int = holder.cancelDrag()

    fun synchronizeHeight(heightPx: Int): Int = holder.synchronizeHeight(heightPx)
}
