/*
 * This file is part of Zero Symbol Input View.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only
 * License text: https://www.gnu.org/licenses/agpl-3.0.html
 * Project additional terms: ADDITIONAL-TERMS.md
 */

package com.zerostudio.idea.widget.drawer.core

/** Runtime snapshot shared by the container and application-facing controller. */
data class DrawerState(
    val currentAnchorId: String,
    val currentHeightPx: Int,
    val anchors: List<DrawerAnchor>,
    val isDragging: Boolean = false,
    val isAnimating: Boolean = false
) {
    init {
        require(currentAnchorId.isNotBlank())
        require(currentHeightPx >= 0)
        require(anchors.isNotEmpty())
        require(anchors.map { it.id }.distinct().size == anchors.size) {
            "Drawer state anchor IDs must be unique"
        }
        require(anchors.any { it.id == currentAnchorId }) {
            "Current anchor '$currentAnchorId' must exist in the state anchor list"
        }
    }

    val currentAnchor: DrawerAnchor?
        get() = anchors.firstOrNull { it.id == currentAnchorId }

    /** True only when the continuous height exactly matches a stable anchor. */
    val isAtStableAnchor: Boolean
        get() = !isDragging && !isAnimating && currentAnchor?.heightPx == currentHeightPx

    /** Whether the drawer is above its most-collapsed (Peek) position. */
    val isExpanded: Boolean
        get() = currentHeightPx > (anchors.minOfOrNull { it.heightPx } ?: 0)

    /** Normalized progress from the lowest to the highest resolved anchor. */
    val expansionFraction: Float
        get() {
            val minHeight = anchors.minOfOrNull { it.heightPx } ?: return 0f
            val maxHeight = anchors.maxOfOrNull { it.heightPx } ?: return 0f
            if (maxHeight <= minHeight) return 0f
            return ((currentHeightPx - minHeight).toFloat() / (maxHeight - minHeight))
                .coerceIn(0f, 1f)
        }

    val isAtPeekAnchor: Boolean
        get() = anchors.minByOrNull { it.heightPx }?.id == currentAnchorId &&
            currentAnchor?.heightPx == currentHeightPx && !isDragging && !isAnimating

    val isAtExpandedAnchor: Boolean
        get() = anchors.maxByOrNull { it.heightPx }?.id == currentAnchorId &&
            currentAnchor?.heightPx == currentHeightPx && !isDragging && !isAnimating
}
