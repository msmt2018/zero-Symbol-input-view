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
 * Supplies adaptive window information from the actual Compose layout bounds.
 *
 * Recomposition automatically updates the snapshot when the host resizes,
 * enters split-screen, changes density or updates its folding-feature input.
 * There is no View listener, attach observer, close handle or lifecycle API.
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
        val safeInsets = if (insetsMode == WindowInsetsMode.SUBTRACT_SYSTEM_INSETS) {
            WindowInsets.safeDrawing
        } else {
            WindowInsets(0, 0, 0, 0)
        }
        val insets = WindowInsetsDp(
            left = with(density) { safeInsets.getLeft(density, layoutDirection).toDp().value },
            top = with(density) { safeInsets.getTop(density).toDp().value },
            right = with(density) { safeInsets.getRight(density, layoutDirection).toDp().value },
            bottom = with(density) { safeInsets.getBottom(density).toDp().value }
        )
        content(
            WindowAdaptiveInfo(
                widthDp = maxWidth.value.takeIf { it.isFinite() } ?: 0f,
                heightDp = maxHeight.value.takeIf { it.isFinite() } ?: 0f,
                density = density.density,
                insets = insets,
                foldingFeature = foldingFeature
            )
        )
    }
}
