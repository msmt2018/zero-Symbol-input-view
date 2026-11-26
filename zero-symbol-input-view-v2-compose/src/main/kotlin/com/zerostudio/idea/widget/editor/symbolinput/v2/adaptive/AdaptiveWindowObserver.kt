/*
 * This file is part of zero-Symbol-input-view-V2.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp

/** Controls whether safe-drawing insets are subtracted from the measured Compose bounds. */
enum class WindowInsetsMode {
    SUBTRACT_SYSTEM_INSETS,
    VIEW_BOUNDS_ALREADY_EXCLUDE_INSETS
}

/**
 * Publishes adaptive information from the actual constraints supplied by the
 * Compose parent. The constraints therefore follow the app window through
 * rotation, split-screen, fold/unfold and freeform resizing.
 *
 * The parent should provide bounded window/content constraints. If one axis is
 * unbounded, its minimum constraint is used as the best available local bound
 * rather than converting infinity to a misleading zero-sized window.
 *
 * Insets are reported separately from the measured bounds. Consumers use
 * [WindowAdaptiveInfo.availableWidthDp] and [WindowAdaptiveInfo.availableHeightDp]
 * to make layout decisions without coupling this observer to a particular
 * Activity, View, WindowMetrics owner or lifecycle.
 */
@Composable
fun AdaptiveWindowObserver(
    modifier: Modifier = Modifier,
    foldingFeature: FoldingFeatureInfo = FoldingFeatureInfo(),
    insetsMode: WindowInsetsMode = WindowInsetsMode.SUBTRACT_SYSTEM_INSETS,
    content: @Composable (WindowAdaptiveInfo) -> Unit
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    BoxWithConstraints(modifier = modifier) {
        val measuredWidthDp = maxWidth.value
            .takeIf(Float::isFinite)
            ?: minWidth.value.takeIf(Float::isFinite)
            ?: 0f
        val measuredHeightDp = maxHeight.value
            .takeIf(Float::isFinite)
            ?: minHeight.value.takeIf(Float::isFinite)
            ?: 0f

        val safeInsets = if (insetsMode == WindowInsetsMode.SUBTRACT_SYSTEM_INSETS) {
            WindowInsets.safeDrawing
        } else {
            WindowInsets(0, 0, 0, 0)
        }
        val insets = WindowInsetsDp(
            left = with(density) {
                safeInsets.getLeft(density, layoutDirection).toDp().value
            },
            top = with(density) {
                safeInsets.getTop(density).toDp().value
            },
            right = with(density) {
                safeInsets.getRight(density, layoutDirection).toDp().value
            },
            bottom = with(density) {
                safeInsets.getBottom(density).toDp().value
            }
        )

        content(
            WindowAdaptiveInfo(
                widthDp = measuredWidthDp,
                heightDp = measuredHeightDp,
                density = density.density,
                insets = insets,
                foldingFeature = foldingFeature
            )
        )
    }
}
