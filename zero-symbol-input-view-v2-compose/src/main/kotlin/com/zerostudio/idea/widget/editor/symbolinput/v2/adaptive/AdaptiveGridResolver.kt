/*
 * This file is part of zero-Symbol-input-view-V2.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive

data class AdaptiveGridSpec(
    val columnCount: Int,
    val horizontalSpacingDp: Int,
    val verticalSpacingDp: Int = horizontalSpacingDp
) {
    init {
        require(columnCount >= 1)
        require(horizontalSpacingDp in -24..48)
        require(verticalSpacingDp in -24..48)
    }
}

/**
 * Resolves a responsive grid from the width actually allocated by its parent.
 *
 * The preferred column count is an upper bound. Each cell is kept at least
 * [minimumCellWidthDp] wide whenever the available width permits it. Negative
 * spacing remains supported for the symbol drawer's compact presentation, but
 * it is not allowed to make the column-count calculation divide by zero.
 */
object AdaptiveGridResolver {
    fun resolve(
        window: WindowAdaptiveInfo,
        preferredColumns: Int,
        minimumCellWidthDp: Float,
        baseHorizontalSpacingDp: Int,
        adaptive: Boolean = true,
        baseVerticalSpacingDp: Int = baseHorizontalSpacingDp,
        lockColumnCount: Boolean = false,
        compactHeightVerticalSpacingScale: Float = 0.75f
    ): AdaptiveGridSpec {
        require(preferredColumns >= 1)
        require(minimumCellWidthDp.isFinite() && minimumCellWidthDp > 0f)
        require(
            compactHeightVerticalSpacingScale.isFinite() &&
                compactHeightVerticalSpacingScale in 0f..1f
        )

        // Runtime config can originate from an older host or persisted user
        // settings. Clamp only to the supported spacing range; do not silently
        // add size-class spacing, which would override the user's chosen value.
        val horizontalSpacing = baseHorizontalSpacingDp.coerceIn(-24, 48)
        val configuredVerticalSpacing = baseVerticalSpacingDp.coerceIn(-24, 48)
        val verticalSpacing = if (window.availableHeightDp < COMPACT_HEIGHT_UPPER_BOUND_DP &&
            configuredVerticalSpacing > 0
        ) {
            (configuredVerticalSpacing * compactHeightVerticalSpacingScale).toInt()
        } else {
            configuredVerticalSpacing
        }

        if (!adaptive || lockColumnCount) {
            return AdaptiveGridSpec(
                columnCount = preferredColumns,
                horizontalSpacingDp = horizontalSpacing,
                verticalSpacingDp = verticalSpacing
            )
        }

        val availableWidth = window.availableWidthDp.coerceAtLeast(0f)
        val effectiveSpacing = horizontalSpacing.coerceAtLeast(0).toFloat()
        val minimumTrackWidth = minimumCellWidthDp + effectiveSpacing
        val fittingColumns = if (availableWidth < minimumCellWidthDp) {
            1
        } else {
            // GridCells.Adaptive packs the maximum number of minimum-sized
            // tracks and distributes the remaining cross-axis space evenly.
            ((availableWidth + effectiveSpacing) / minimumTrackWidth)
                .toInt()
                .coerceAtLeast(1)
        }

        return AdaptiveGridSpec(
            columnCount = fittingColumns.coerceAtMost(preferredColumns),
            horizontalSpacingDp = horizontalSpacing,
            verticalSpacingDp = verticalSpacing
        )
    }

    private const val COMPACT_HEIGHT_UPPER_BOUND_DP = 480f
}
