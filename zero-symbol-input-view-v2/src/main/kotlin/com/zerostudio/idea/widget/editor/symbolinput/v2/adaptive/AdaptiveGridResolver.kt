/*
 * This file is part of ZeroStudio.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive

data class AdaptiveGridSpec(
    val columnCount: Int,
    val horizontalSpacingDp: Int
) {
    init {
        require(columnCount >= 1)
        require(horizontalSpacingDp in -10..24)
    }
}

/** Resolves a grid that can reflow inside a fixed-height host without changing host geometry. */
object AdaptiveGridResolver {
    fun resolve(
        window: WindowAdaptiveInfo,
        preferredColumns: Int,
        minimumCellWidthDp: Float,
        baseHorizontalSpacingDp: Int,
        adaptive: Boolean = true
    ): AdaptiveGridSpec {
        require(preferredColumns >= 1)
        require(minimumCellWidthDp > 0f)
        require(baseHorizontalSpacingDp in -10..24)
        if (!adaptive) return AdaptiveGridSpec(preferredColumns, baseHorizontalSpacingDp)

        val spacingAdjustment = when (window.sizeClass) {
            WindowSizeClass.COMPACT -> 0
            WindowSizeClass.MEDIUM -> 1
            WindowSizeClass.EXPANDED -> 3
        }
        val spacing = (baseHorizontalSpacingDp + spacingAdjustment).coerceIn(-10, 24)
        val effectiveSpacing = spacing.coerceAtLeast(0)
        val columns = (window.availableWidthDp / (minimumCellWidthDp + effectiveSpacing))
            .toInt()
            .coerceIn(1, preferredColumns)
        return AdaptiveGridSpec(columns, spacing)
    }
}
