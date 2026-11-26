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
 * The preferred column count is an upper bound. The resolver uses the effective
 * track pitch (cell width plus spacing), including supported negative spacing,
 * so compact layouts do not lose columns merely because cells intentionally
 * overlap. The result remains bounded by the configured preferred count.
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

        // Preserve the configured spacing, including intentional negative
        // overlap. Clamping is limited to the public supported range.
        val horizontalSpacing = baseHorizontalSpacingDp.coerceIn(MIN_SPACING_DP, MAX_SPACING_DP)
        val configuredVerticalSpacing = baseVerticalSpacingDp.coerceIn(MIN_SPACING_DP, MAX_SPACING_DP)
        val verticalSpacing = if (
            window.availableHeightDp < COMPACT_HEIGHT_UPPER_BOUND_DP &&
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

        val availableWidth = window.availableWidthDp.toDouble().coerceAtLeast(0.0)
        val cellWidth = minimumCellWidthDp.toDouble()
        val spacing = horizontalSpacing.toDouble()
        // A negative spacing reduces track pitch, but must never collapse the
        // pitch to zero or invert the grid's column calculation.
        val trackPitch = (cellWidth + spacing).coerceAtLeast(MIN_TRACK_PITCH_DP)
        val fittingColumns = if (availableWidth < cellWidth) {
            1
        } else {
            kotlin.math.floor(
                (availableWidth - spacing).coerceAtLeast(0.0) / trackPitch
            ).toInt().coerceAtLeast(1)
        }

        // The first track always occupies at least its minimum cell width.
        val boundedColumns = fittingColumns.coerceAtMost(preferredColumns)
            .coerceAtLeast(1)
        return AdaptiveGridSpec(
            columnCount = boundedColumns,
            horizontalSpacingDp = horizontalSpacing,
            verticalSpacingDp = verticalSpacing
        )
    }

    private const val COMPACT_HEIGHT_UPPER_BOUND_DP = 480f
    private const val MIN_SPACING_DP = -24
    private const val MAX_SPACING_DP = 48
    private const val MIN_TRACK_PITCH_DP = 1.0
}
