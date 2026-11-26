/*
 * This file is part of zero-Symbol-input-view-V2.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive

import kotlin.math.floor
import kotlin.math.roundToInt

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

/** Resolves a responsive grid using the width allocated by its parent. */
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
        require(compactHeightVerticalSpacingScale.isFinite() && compactHeightVerticalSpacingScale in 0f..1f)

        val horizontalBase = baseHorizontalSpacingDp.coerceIn(MIN_SPACING_DP, MAX_SPACING_DP)
        val verticalBase = baseVerticalSpacingDp.coerceIn(MIN_SPACING_DP, MAX_SPACING_DP)
        if (!adaptive) {
            return AdaptiveGridSpec(preferredColumns, horizontalBase, verticalBase)
        }

        // Keep the established View V2 spacing contract: compact = +0dp,
        // medium = +1dp and expanded = +3dp. This is an additive layout
        // adjustment, not a multiplicative scale (2dp at expanded becomes 5dp).
        val spacingAdjustment = when (window.sizeClass) {
            WindowSizeClass.COMPACT -> COMPACT_SPACING_ADJUSTMENT_DP
            WindowSizeClass.MEDIUM -> MEDIUM_SPACING_ADJUSTMENT_DP
            WindowSizeClass.EXPANDED -> EXPANDED_SPACING_ADJUSTMENT_DP
        }
        val horizontalSpacing = (horizontalBase + spacingAdjustment)
            .coerceIn(MIN_SPACING_DP, MAX_SPACING_DP)
        val scaledVertical = (verticalBase + spacingAdjustment)
            .coerceIn(MIN_SPACING_DP, MAX_SPACING_DP)
        val verticalSpacing = if (
            window.availableHeightDp < COMPACT_HEIGHT_UPPER_BOUND_DP && scaledVertical > 0
        ) {
            (scaledVertical * compactHeightVerticalSpacingScale).roundToInt()
                .coerceIn(MIN_SPACING_DP, MAX_SPACING_DP)
        } else scaledVertical

        if (lockColumnCount) {
            return AdaptiveGridSpec(preferredColumns, horizontalSpacing, verticalSpacing)
        }

        val availableWidth = window.availableWidthDp.toDouble().coerceAtLeast(0.0)
        val cellWidth = minimumCellWidthDp.toDouble()
        val spacing = horizontalSpacing.toDouble()
        val trackPitch = (cellWidth + spacing).coerceAtLeast(MIN_TRACK_PITCH_DP)
        val fittingColumns = if (availableWidth < cellWidth) 1 else {
            floor((availableWidth - spacing).coerceAtLeast(0.0) / trackPitch)
                .toInt()
                .coerceAtLeast(1)
        }
        return AdaptiveGridSpec(
            fittingColumns.coerceAtMost(preferredColumns).coerceAtLeast(1),
            horizontalSpacing,
            verticalSpacing
        )
    }

    private const val COMPACT_HEIGHT_UPPER_BOUND_DP = 480f
    private const val COMPACT_SPACING_ADJUSTMENT_DP = 0
    private const val MEDIUM_SPACING_ADJUSTMENT_DP = 1
    private const val EXPANDED_SPACING_ADJUSTMENT_DP = 3
    private const val MIN_SPACING_DP = -24
    private const val MAX_SPACING_DP = 48
    private const val MIN_TRACK_PITCH_DP = 1.0
}
