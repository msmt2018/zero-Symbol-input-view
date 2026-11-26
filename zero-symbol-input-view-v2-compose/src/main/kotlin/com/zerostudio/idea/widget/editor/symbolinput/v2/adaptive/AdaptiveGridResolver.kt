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
        val spacingScale = when {
            !adaptive || window.availableWidthDp < MEDIUM_WIDTH_DP -> 1f
            window.availableWidthDp < EXPANDED_WIDTH_DP -> 1.25f
            else -> 1.5f
        }
        val horizontalSpacing = adaptSpacing(horizontalBase, spacingScale)
        val scaledVertical = adaptSpacing(verticalBase, spacingScale)
        val verticalSpacing = if (window.availableHeightDp < COMPACT_HEIGHT_UPPER_BOUND_DP && scaledVertical > 0) {
            (scaledVertical * compactHeightVerticalSpacingScale).roundToInt()
        } else scaledVertical

        if (!adaptive || lockColumnCount) {
            return AdaptiveGridSpec(preferredColumns, horizontalSpacing, verticalSpacing)
        }

        val availableWidth = window.availableWidthDp.toDouble().coerceAtLeast(0.0)
        val cellWidth = minimumCellWidthDp.toDouble()
        val spacing = horizontalSpacing.toDouble()
        val trackPitch = (cellWidth + spacing).coerceAtLeast(MIN_TRACK_PITCH_DP)
        val fittingColumns = if (availableWidth < cellWidth) 1 else {
            floor((availableWidth - spacing).coerceAtLeast(0.0) / trackPitch).toInt().coerceAtLeast(1)
        }
        return AdaptiveGridSpec(
            fittingColumns.coerceAtMost(preferredColumns).coerceAtLeast(1),
            horizontalSpacing,
            verticalSpacing
        )
    }

    private fun adaptSpacing(spacingDp: Int, scale: Float): Int {
        if (spacingDp <= 0 || scale == 1f) return spacingDp
        val scaled = (spacingDp * scale).roundToInt()
        return scaled.coerceIn(MIN_SPACING_DP, MAX_SPACING_DP)
    }

    private const val COMPACT_HEIGHT_UPPER_BOUND_DP = 480f
    private const val MEDIUM_WIDTH_DP = 600f
    private const val EXPANDED_WIDTH_DP = 840f
    private const val MIN_SPACING_DP = -24
    private const val MAX_SPACING_DP = 48
    private const val MIN_TRACK_PITCH_DP = 1.0
}
