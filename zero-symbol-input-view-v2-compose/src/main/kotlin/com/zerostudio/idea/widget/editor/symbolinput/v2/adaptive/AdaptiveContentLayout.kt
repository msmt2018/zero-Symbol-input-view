/*
 * This file is part of zero-Symbol-input-view-V2.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive

/** Shared responsive content metrics for settings, manager and reusable V2 surfaces. */
data class AdaptiveContentLayoutSpec(
    val maxContentWidthDp: Float,
    val horizontalPaddingDp: Float,
    val verticalSpacingDp: Float,
    val cardColumns: Int,
    val dialogWidthFraction: Float
) {
    init {
        require(maxContentWidthDp.isFinite() && maxContentWidthDp > 0f)
        require(horizontalPaddingDp.isFinite() && horizontalPaddingDp >= 0f)
        require(verticalSpacingDp.isFinite() && verticalSpacingDp >= 0f)
        require(cardColumns >= 1)
        require(dialogWidthFraction.isFinite() && dialogWidthFraction in 0.5f..1f)
    }
}

data class AdaptiveContentLayoutPolicy(
    val compactMaxWidthDp: Float = 560f,
    val mediumMaxWidthDp: Float = 840f,
    val expandedMaxWidthDp: Float = 1040f,
    val compactPaddingDp: Float = 12f,
    val mediumPaddingDp: Float = 24f,
    val expandedPaddingDp: Float = 32f,
    val compactDialogFraction: Float = 0.96f,
    val mediumDialogFraction: Float = 0.88f,
    val expandedDialogFraction: Float = 0.72f,
    val twoColumnBreakpointDp: Float = 840f
) {
    init {
        require(compactMaxWidthDp.isFinite() && compactMaxWidthDp > 0f)
        require(mediumMaxWidthDp.isFinite() && mediumMaxWidthDp >= compactMaxWidthDp)
        require(expandedMaxWidthDp.isFinite() && expandedMaxWidthDp >= mediumMaxWidthDp)
        require(compactPaddingDp.isFinite() && compactPaddingDp >= 0f)
        require(mediumPaddingDp.isFinite() && mediumPaddingDp >= 0f)
        require(expandedPaddingDp.isFinite() && expandedPaddingDp >= 0f)
        require(compactDialogFraction.isFinite() && compactDialogFraction in 0.5f..1f)
        require(mediumDialogFraction.isFinite() && mediumDialogFraction in 0.5f..1f)
        require(expandedDialogFraction.isFinite() && expandedDialogFraction in 0.5f..1f)
        require(twoColumnBreakpointDp.isFinite() && twoColumnBreakpointDp > 0f)
    }
}

object AdaptiveContentLayoutResolver {
    fun resolve(
        window: WindowAdaptiveInfo,
        policy: AdaptiveContentLayoutPolicy = AdaptiveContentLayoutPolicy()
    ): AdaptiveContentLayoutSpec = when (window.sizeClass) {
        WindowSizeClass.COMPACT -> AdaptiveContentLayoutSpec(
            policy.compactMaxWidthDp, policy.compactPaddingDp, 8f, 1,
            policy.compactDialogFraction
        )
        WindowSizeClass.MEDIUM -> AdaptiveContentLayoutSpec(
            policy.mediumMaxWidthDp, policy.mediumPaddingDp, 12f, 1,
            policy.mediumDialogFraction
        )
        WindowSizeClass.EXPANDED -> AdaptiveContentLayoutSpec(
            policy.expandedMaxWidthDp, policy.expandedPaddingDp, 16f,
            if (window.availableWidthDp >= policy.twoColumnBreakpointDp) 2 else 1,
            policy.expandedDialogFraction
        )
    }

    fun resolveDialogWidthPx(
        windowWidthPx: Int,
        density: Float,
        window: WindowAdaptiveInfo,
        policy: AdaptiveContentLayoutPolicy = AdaptiveContentLayoutPolicy()
    ): Int {
        require(windowWidthPx >= 0 && density.isFinite() && density > 0f)
        val spec = resolve(window, policy)
        val availablePx = (window.availableWidthDp.toDouble() * density.toDouble())
            .coerceAtMost(Int.MAX_VALUE.toDouble()).toInt().coerceAtLeast(0)
        val fractionWidth = (availablePx * spec.dialogWidthFraction).toInt()
        val maxWidth = (spec.maxContentWidthDp.toDouble() * density.toDouble())
            .coerceAtMost(Int.MAX_VALUE.toDouble()).toInt()
        return minOf(windowWidthPx, fractionWidth, maxWidth).coerceAtLeast(0)
    }
}
