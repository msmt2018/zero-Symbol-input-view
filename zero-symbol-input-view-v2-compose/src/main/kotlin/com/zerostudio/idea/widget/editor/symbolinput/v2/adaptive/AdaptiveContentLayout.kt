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
    val largeMaxWidthDp: Float = 1200f,
    val extraLargeMaxWidthDp: Float = 1440f,
    val compactPaddingDp: Float = 12f,
    val mediumPaddingDp: Float = 24f,
    val expandedPaddingDp: Float = 32f,
    val largePaddingDp: Float = 40f,
    val extraLargePaddingDp: Float = 48f,
    val compactDialogFraction: Float = 0.96f,
    val mediumDialogFraction: Float = 0.88f,
    val expandedDialogFraction: Float = 0.72f,
    val largeDialogFraction: Float = 0.64f,
    val extraLargeDialogFraction: Float = 0.56f,
    val twoColumnBreakpointDp: Float = 840f,
    val threeColumnBreakpointDp: Float = 1200f,
    val fourColumnBreakpointDp: Float = 1600f
) {
    init {
        val widths = listOf(
            compactMaxWidthDp, mediumMaxWidthDp, expandedMaxWidthDp,
            largeMaxWidthDp, extraLargeMaxWidthDp
        )
        require(widths.all { it.isFinite() && it > 0f })
        require(widths.zipWithNext().all { (a, b) -> b >= a })
        val paddings = listOf(
            compactPaddingDp, mediumPaddingDp, expandedPaddingDp,
            largePaddingDp, extraLargePaddingDp
        )
        require(paddings.all { it.isFinite() && it >= 0f })
        val fractions = listOf(
            compactDialogFraction, mediumDialogFraction, expandedDialogFraction,
            largeDialogFraction, extraLargeDialogFraction
        )
        require(fractions.all { it.isFinite() && it in 0.5f..1f })
        require(twoColumnBreakpointDp.isFinite() && twoColumnBreakpointDp > 0f)
        require(threeColumnBreakpointDp >= twoColumnBreakpointDp)
        require(fourColumnBreakpointDp >= threeColumnBreakpointDp)
    }
}

object AdaptiveContentLayoutResolver {
    fun resolve(
        window: WindowAdaptiveInfo,
        policy: AdaptiveContentLayoutPolicy = AdaptiveContentLayoutPolicy()
    ): AdaptiveContentLayoutSpec {
        val width = window.availableWidthDp
        val height = window.availableHeightDp

        // Keep content bounded by the current window class while allowing
        // large and extra-large windows to use their explicitly configured
        // wider reading measure.
        val (targetMaxWidth, padding, dialogFraction, columns) = when {
            width < 600f -> Quadruple(
                policy.compactMaxWidthDp, policy.compactPaddingDp,
                policy.compactDialogFraction, 1
            )
            width < 840f -> Quadruple(
                policy.mediumMaxWidthDp, policy.mediumPaddingDp,
                policy.mediumDialogFraction, 1
            )
            width < 1200f -> Quadruple(
                policy.expandedMaxWidthDp, policy.expandedPaddingDp,
                policy.expandedDialogFraction, 2
            )
            width < 1600f -> Quadruple(
                policy.largeMaxWidthDp, policy.largePaddingDp,
                policy.largeDialogFraction, 3
            )
            else -> Quadruple(
                policy.extraLargeMaxWidthDp, policy.extraLargePaddingDp,
                policy.extraLargeDialogFraction, 4
            )
        }

        val availableContentWidth = (width - padding * 2f).coerceAtLeast(1f)
        val maxContentWidth = minOf(targetMaxWidth, availableContentWidth)
        val verticalSpacing = when {
            height < 480f -> 8f
            height < 900f -> 12f
            else -> 16f
        }

        return AdaptiveContentLayoutSpec(
            maxContentWidthDp = maxContentWidth,
            horizontalPaddingDp = minOf(padding, (width - 1f).coerceAtLeast(0f) / 2f),
            verticalSpacingDp = verticalSpacing,
            cardColumns = columns,
            dialogWidthFraction = dialogFraction
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
        val availableWidthPx = (window.availableWidthDp.toDouble() * density.toDouble())
            .coerceAtMost(Int.MAX_VALUE.toDouble()).toInt().coerceAtLeast(0)
        val fractionWidthPx = (windowWidthPx.toDouble() * spec.dialogWidthFraction)
            .coerceAtMost(Int.MAX_VALUE.toDouble()).toInt().coerceAtLeast(0)
        val contentMaxWidthPx = (spec.maxContentWidthDp.toDouble() * density.toDouble())
            .coerceAtMost(Int.MAX_VALUE.toDouble()).toInt().coerceAtLeast(0)

        // Dialog fractions are relative to the actual host window. Padding is
        // a content-layout concern and must not be subtracted a second time.
        return minOf(windowWidthPx, availableWidthPx, fractionWidthPx, contentMaxWidthPx)
            .coerceAtLeast(0)
    }

    private data class Quadruple(
        val maxWidth: Float,
        val padding: Float,
        val dialogFraction: Float,
        val columns: Int
    )
}
