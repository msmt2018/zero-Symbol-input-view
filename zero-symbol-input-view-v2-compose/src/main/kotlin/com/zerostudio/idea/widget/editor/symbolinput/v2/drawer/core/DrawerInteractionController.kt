/*
 * This file is part of zero-Symbol-input-view-V2.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core

import kotlin.math.abs

/**
 * Platform-neutral drag geometry used by Compose gesture modifiers.
 *
 * This class deliberately does not own MotionEvent, VelocityTracker, View,
 * Context, animator, attachment state or a UI lifecycle. Compose owns gesture
 * collection and animation; this type only resolves bounded height updates.
 */
@Deprecated(
    message = "Use DrawerStateHolder with DrawerAnchorStrategy; this legacy controller maintains a separate two-bound state.",
    replaceWith = ReplaceWith("DrawerStateHolder")
)
class DrawerInteractionController(
    minimumHeightPx: Int,
    maximumHeightPx: Int,
    initialHeightPx: Int = minimumHeightPx
) {
    var minimumHeightPx: Int = minimumHeightPx
        private set
    var maximumHeightPx: Int = maximumHeightPx
        private set
    var currentHeightPx: Int = initialHeightPx.coerceIn(minimumHeightPx, maximumHeightPx)
        private set
    var isDragging: Boolean = false
        private set

    init {
        require(minimumHeightPx >= 0)
        require(maximumHeightPx >= minimumHeightPx)
    }

    val expansionFraction: Float
        get() {
            val range = maximumHeightPx - minimumHeightPx
            return if (range <= 0) 0f
            else (currentHeightPx - minimumHeightPx).toFloat() / range
        }

    fun updateBounds(
        minimumHeightPx: Int,
        maximumHeightPx: Int,
        preserveExpansionFraction: Boolean = true
    ): Int {
        require(minimumHeightPx >= 0)
        require(maximumHeightPx >= minimumHeightPx)
        val fraction = expansionFraction
        this.minimumHeightPx = minimumHeightPx
        this.maximumHeightPx = maximumHeightPx
        currentHeightPx = if (preserveExpansionFraction) {
            (minimumHeightPx + (maximumHeightPx - minimumHeightPx) * fraction).toInt()
        } else {
            currentHeightPx.coerceIn(minimumHeightPx, maximumHeightPx)
        }
        return currentHeightPx
    }

    fun beginDrag() {
        isDragging = true
    }

    /** Applies a finger delta in pixels; negative deltas expand a bottom sheet. */
    fun dragBy(deltaPx: Float): Int {
        require(deltaPx.isFinite())
        if (!isDragging) isDragging = true
        currentHeightPx = (currentHeightPx - deltaPx).coerceIn(
            minimumHeightPx.toFloat(),
            maximumHeightPx.toFloat()
        ).toInt()
        return currentHeightPx
    }

    /**
     * Ends a drag and returns a stable target. Fling direction follows finger
     * velocity: an upward (negative) finger velocity expands the sheet.
     */
    fun endDrag(
        fingerVelocityY: Float,
        minimumFlingVelocityPxPerSecond: Float
    ): Int {
        require(fingerVelocityY.isFinite())
        require(minimumFlingVelocityPxPerSecond.isFinite() &&
            minimumFlingVelocityPxPerSecond >= 0f)
        isDragging = false
        return when {
            abs(fingerVelocityY) >= minimumFlingVelocityPxPerSecond ->
                if (fingerVelocityY < 0f) maximumHeightPx else minimumHeightPx
            currentHeightPx >= (minimumHeightPx + maximumHeightPx) / 2 ->
                maximumHeightPx
            else -> minimumHeightPx
        }.also { currentHeightPx = it }
    }

    fun cancelDrag(): Int {
        isDragging = false
        currentHeightPx = currentHeightPx.coerceIn(minimumHeightPx, maximumHeightPx)
        return currentHeightPx
    }

    fun synchronizeHeight(heightPx: Int): Int {
        isDragging = false
        currentHeightPx = heightPx.coerceIn(minimumHeightPx, maximumHeightPx)
        return currentHeightPx
    }
}
