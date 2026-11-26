/*
 * This file is part of zero-Symbol-input-view-V2.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive

enum class CanonicalLayoutType { LIST_DETAIL, FEED, SUPPORTING_PANE }
enum class PaneArrangement { SINGLE, SIDE_BY_SIDE, MAIN_WITH_SUPPORTING, FEED_GRID, STACKED }

/** Explicit, inspectable result; UI containers decide how to apply the arrangement. */
data class AdaptiveLayoutSpec(
    val layoutType: CanonicalLayoutType,
    val arrangement: PaneArrangement,
    val primaryPaneWeight: Float = 1f,
    val secondaryPaneWeight: Float = 0f,
    val feedColumnCount: Int = 1,
    val useHingeAsBoundary: Boolean = false
) {
    init {
        require(primaryPaneWeight.isFinite() && primaryPaneWeight > 0f)
        require(secondaryPaneWeight.isFinite() && secondaryPaneWeight >= 0f)
        require(feedColumnCount >= 1)
        if (arrangement == PaneArrangement.SINGLE || arrangement == PaneArrangement.STACKED) {
            require(secondaryPaneWeight == 0f)
        }
    }
}

data class AdaptiveLayoutPolicy(
    val listDetailBreakpointDp: Float = WindowAdaptiveInfo.EXPANDED_WIDTH_DP,
    val supportingPaneBreakpointDp: Float = WindowAdaptiveInfo.MEDIUM_WIDTH_DP,
    val minimumMultiPaneHeightDp: Float = 480f,
    val feedMinimumColumnWidthDp: Float = 280f,
    val feedMaximumColumns: Int = 4,
    val supportingPaneMainWeight: Float = 0.7f,
    val supportingPaneSecondaryWeight: Float = 0.3f
) {
    init {
        require(listDetailBreakpointDp.isFinite() && listDetailBreakpointDp > 0f)
        require(supportingPaneBreakpointDp.isFinite() && supportingPaneBreakpointDp > 0f)
        require(minimumMultiPaneHeightDp.isFinite() && minimumMultiPaneHeightDp > 0f)
        require(feedMinimumColumnWidthDp.isFinite() && feedMinimumColumnWidthDp > 0f)
        require(feedMaximumColumns >= 1)
        require(supportingPaneMainWeight.isFinite() && supportingPaneMainWeight > 0f)
        require(supportingPaneSecondaryWeight.isFinite() && supportingPaneSecondaryWeight > 0f)
    }
}

object AdaptiveLayoutResolver {
    fun resolve(
        info: WindowAdaptiveInfo,
        type: CanonicalLayoutType,
        policy: AdaptiveLayoutPolicy = AdaptiveLayoutPolicy()
    ): AdaptiveLayoutSpec {
        val width = info.availableWidthDp
        val height = info.availableHeightDp

        // Only a vertically oriented separating feature can divide a window
        // into side-by-side panes. A horizontal tabletop fold is not a
        // meaningful boundary for list/detail columns.
        val folding = info.foldingFeature
        val verticalSeparatingHinge = folding.isSeparating &&
            folding.hasBounds &&
            (folding.boundsBottomDp - folding.boundsTopDp) >=
            (folding.boundsRightDp - folding.boundsLeftDp)

        // Width and height are independent constraints. A wide but very short
        // landscape/freeform window remains single-pane when there is not
        // enough vertical room for the paired content surfaces.
        val hasMultiPaneSpace = height >= policy.minimumMultiPaneHeightDp
        val useHingeBoundary = verticalSeparatingHinge && hasMultiPaneSpace

        return when (type) {
            CanonicalLayoutType.LIST_DETAIL -> {
                val dualPane = hasMultiPaneSpace &&
                    (width >= policy.listDetailBreakpointDp || verticalSeparatingHinge)
                AdaptiveLayoutSpec(
                    layoutType = type,
                    arrangement = if (dualPane) PaneArrangement.SIDE_BY_SIDE else PaneArrangement.SINGLE,
                    primaryPaneWeight = if (dualPane) 0.4f else 1f,
                    secondaryPaneWeight = if (dualPane) 0.6f else 0f,
                    useHingeAsBoundary = useHingeBoundary
                )
            }

            CanonicalLayoutType.FEED -> {
                val columns = kotlin.math.floor(
                    width.toDouble() / policy.feedMinimumColumnWidthDp.toDouble()
                ).toInt().coerceIn(1, policy.feedMaximumColumns)
                AdaptiveLayoutSpec(
                    layoutType = type,
                    arrangement = PaneArrangement.FEED_GRID,
                    feedColumnCount = columns,
                    useHingeAsBoundary = useHingeBoundary
                )
            }

            CanonicalLayoutType.SUPPORTING_PANE -> {
                val showSupporting = hasMultiPaneSpace &&
                    (width >= policy.supportingPaneBreakpointDp || verticalSeparatingHinge)
                AdaptiveLayoutSpec(
                    layoutType = type,
                    arrangement = if (showSupporting) {
                        PaneArrangement.MAIN_WITH_SUPPORTING
                    } else {
                        PaneArrangement.STACKED
                    },
                    primaryPaneWeight = if (showSupporting) policy.supportingPaneMainWeight else 1f,
                    secondaryPaneWeight = if (showSupporting) policy.supportingPaneSecondaryWeight else 0f,
                    useHingeAsBoundary = useHingeBoundary
                )
            }
        }
    }
}
