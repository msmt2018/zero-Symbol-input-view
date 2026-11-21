/*
 * This file is part of zero-Symbol-input-view-V2.
 *
 * Copyright (C) 2026 android_zero
 *
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive

/**
 * Physical layout contract for a docked symbol toolbar.
 *
 * The resolver only computes extents; the host remains responsible for placing
 * the surface in its Compose editor container. Bottom-mode extents are passed
 * through unchanged so the established drawer-height/anchor contract is preserved.
 */
data class SymbolDrawerDockingLayoutSpec(
    val docking: SymbolDrawerDocking,
    /** Extent on the opening/closing axis: height for bottom, width for side. */
    val collapsedExtentDp: Float,
    val expandedExtentDp: Float,
    /** Cross-axis extent: width for bottom, height for side. */
    val crossAxisExtentDp: Float,
    /** Remaining editor width when a side dock is fully expanded. */
    val remainingEditorWidthDp: Float,
    val sideRailWidthDp: Float
) {
    val isSideDocked: Boolean
        get() = docking != SymbolDrawerDocking.BOTTOM
}

/** Resolves dimensions for bottom and edge-docked symbol toolbars. */
object SymbolDrawerDockingLayoutResolver {
    fun resolve(
        settings: com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings,
        window: WindowAdaptiveInfo,
        bottomCollapsedHeightDp: Float,
        bottomExpandedHeightDp: Float,
        collapsedSideWidthDp: Float = DEFAULT_COLLAPSED_SIDE_WIDTH_DP,
        maxExpandedSideWidthDp: Float = DEFAULT_MAX_EXPANDED_SIDE_WIDTH_DP,
        minRemainingEditorWidthDp: Float = MIN_REMAINING_EDITOR_WIDTH_DP,
        sideRailWidthDp: Float = DEFAULT_SIDE_RAIL_WIDTH_DP
    ): SymbolDrawerDockingLayoutSpec = resolve(
        docking = SymbolDrawerDockingPolicy.resolve(settings, window),
        window = window,
        bottomCollapsedHeightDp = bottomCollapsedHeightDp,
        bottomExpandedHeightDp = bottomExpandedHeightDp,
        collapsedSideWidthDp = collapsedSideWidthDp,
        maxExpandedSideWidthDp = maxExpandedSideWidthDp,
        minRemainingEditorWidthDp = minRemainingEditorWidthDp,
        sideRailWidthDp = sideRailWidthDp
    )

    const val DEFAULT_COLLAPSED_SIDE_WIDTH_DP = 72f
    const val MIN_COLLAPSED_SIDE_WIDTH_DP = 56f
    const val MAX_COLLAPSED_SIDE_WIDTH_DP = 96f
    const val DEFAULT_MAX_EXPANDED_SIDE_WIDTH_DP = 480f
    const val MIN_REMAINING_EDITOR_WIDTH_DP = 360f
    const val DEFAULT_SIDE_RAIL_WIDTH_DP = 48f

    fun resolve(
        docking: SymbolDrawerDocking,
        window: WindowAdaptiveInfo,
        bottomCollapsedHeightDp: Float,
        bottomExpandedHeightDp: Float,
        collapsedSideWidthDp: Float = DEFAULT_COLLAPSED_SIDE_WIDTH_DP,
        maxExpandedSideWidthDp: Float = DEFAULT_MAX_EXPANDED_SIDE_WIDTH_DP,
        minRemainingEditorWidthDp: Float = MIN_REMAINING_EDITOR_WIDTH_DP,
        sideRailWidthDp: Float = DEFAULT_SIDE_RAIL_WIDTH_DP
    ): SymbolDrawerDockingLayoutSpec {
        requireFiniteNonNegative(bottomCollapsedHeightDp, "bottomCollapsedHeightDp")
        requireFiniteNonNegative(bottomExpandedHeightDp, "bottomExpandedHeightDp")
        require(bottomExpandedHeightDp >= bottomCollapsedHeightDp) {
            "bottomExpandedHeightDp must be >= bottomCollapsedHeightDp"
        }
        requireFiniteNonNegative(collapsedSideWidthDp, "collapsedSideWidthDp")
        requireFiniteNonNegative(maxExpandedSideWidthDp, "maxExpandedSideWidthDp")
        require(maxExpandedSideWidthDp >= collapsedSideWidthDp) {
            "maxExpandedSideWidthDp must be >= collapsedSideWidthDp"
        }
        requireFiniteNonNegative(minRemainingEditorWidthDp, "minRemainingEditorWidthDp")
        requireFiniteNonNegative(sideRailWidthDp, "sideRailWidthDp")

        if (docking == SymbolDrawerDocking.BOTTOM) {
            return SymbolDrawerDockingLayoutSpec(
                docking = docking,
                collapsedExtentDp = bottomCollapsedHeightDp,
                expandedExtentDp = bottomExpandedHeightDp,
                crossAxisExtentDp = window.availableWidthDp,
                remainingEditorWidthDp = window.availableWidthDp,
                sideRailWidthDp = 0f
            )
        }

        val availableWidth = window.availableWidthDp
        val collapsed = collapsedSideWidthDp.coerceIn(
            MIN_COLLAPSED_SIDE_WIDTH_DP,
            MAX_COLLAPSED_SIDE_WIDTH_DP
        ).coerceAtMost(availableWidth)
        val widthBudget = (availableWidth - minRemainingEditorWidthDp).coerceAtLeast(collapsed)
        val expanded = maxOf(collapsed, minOf(maxExpandedSideWidthDp, widthBudget))
        return SymbolDrawerDockingLayoutSpec(
            docking = docking,
            collapsedExtentDp = collapsed,
            expandedExtentDp = expanded,
            crossAxisExtentDp = window.availableHeightDp,
            remainingEditorWidthDp = (availableWidth - expanded).coerceAtLeast(0f),
            sideRailWidthDp = sideRailWidthDp.coerceAtMost(expanded)
        )
    }

    private fun requireFiniteNonNegative(value: Float, name: String) {
        require(value.isFinite() && value >= 0f) {
            "$name must be finite and >= 0"
        }
    }
}
