/*
 * This file is part of zero-Symbol-input-view-V2.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive

import kotlin.math.roundToInt

/**
 * Bounded scaling for layout dimensions based on the app window, not the
 * physical device. Width follows the five Android window width size classes;
 * compact height applies an additional density-independent layout adjustment
 * for short landscape and split-screen windows.
 *
 * Text must continue to use sp and interactive targets must retain their
 * minimum size. This policy does not replace Android density or font scaling.
 */
data class AdaptiveDimensionPolicy(
    val compactScale: Float = 0.96f,
    val mediumScale: Float = 1f,
    val expandedScale: Float = 1.06f,
    val largeScale: Float = 1.1f,
    val extraLargeScale: Float = 1.12f,
    val compactHeightScale: Float = 0.9f,
    val minimumTouchTargetDp: Float = 48f,
    val maximumScaledDimensionDp: Float = 160f
) {
    init {
        listOf(
            compactScale,
            mediumScale,
            expandedScale,
            largeScale,
            extraLargeScale,
            compactHeightScale
        ).forEach { require(it.isFinite() && it in 0.75f..1.25f) }
        require(minimumTouchTargetDp.isFinite() && minimumTouchTargetDp >= 48f)
        require(maximumScaledDimensionDp.isFinite() && maximumScaledDimensionDp > 0f)
    }
}

class AdaptiveDimensions(
    private val info: WindowAdaptiveInfo,
    private val policy: AdaptiveDimensionPolicy = AdaptiveDimensionPolicy()
) {
    /**
     * Uses the official Android width breakpoints:
     * compact < 600, medium < 840, expanded < 1200, large < 1600,
     * and extra-large >= 1600 dp.
     *
     * A compact-height window (< 480 dp) cannot assume the same vertical
     * breathing room as a portrait window, even when its width is medium.
     */
    val scale: Float
        get() {
            val widthScale = when {
                info.availableWidthDp < COMPACT_WIDTH_UPPER_BOUND_DP -> policy.compactScale
                info.availableWidthDp < EXPANDED_WIDTH_UPPER_BOUND_DP -> policy.mediumScale
                info.availableWidthDp < LARGE_WIDTH_UPPER_BOUND_DP -> policy.expandedScale
                info.availableWidthDp < EXTRA_LARGE_WIDTH_UPPER_BOUND_DP -> policy.largeScale
                else -> policy.extraLargeScale
            }
            return if (info.availableHeightDp < COMPACT_HEIGHT_UPPER_BOUND_DP) {
                minOf(widthScale, policy.compactHeightScale)
            } else {
                widthScale
            }
        }

    fun dp(baseDp: Float, scale: Boolean = true): Float {
        require(baseDp.isFinite() && baseDp >= 0f)
        val value = if (scale) baseDp * this.scale else baseDp
        return value.coerceAtMost(policy.maximumScaledDimensionDp)
    }

    /** Keeps touch targets at or above the 48 dp Material minimum. */
    fun touchTargetDp(requestedDp: Float): Float {
        require(requestedDp.isFinite() && requestedDp >= 0f)
        return maxOf(policy.minimumTouchTargetDp, dp(requestedDp))
    }

    fun px(baseDp: Float, scale: Boolean = true): Int =
        (dp(baseDp, scale).toDouble() * info.density.toDouble()).roundToInt().coerceAtLeast(0)

    private companion object {
        const val COMPACT_WIDTH_UPPER_BOUND_DP = 600f
        const val EXPANDED_WIDTH_UPPER_BOUND_DP = 840f
        const val LARGE_WIDTH_UPPER_BOUND_DP = 1200f
        const val EXTRA_LARGE_WIDTH_UPPER_BOUND_DP = 1600f
        const val COMPACT_HEIGHT_UPPER_BOUND_DP = 480f
    }
}
