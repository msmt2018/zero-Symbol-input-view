/*
 * This file is part of zero-Symbol-input-view-V2.
 *
 * Copyright (C) 2026 android_zero
 *
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive

import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings

/** Pure docking state coordinator. Window measurements are supplied by Compose. */
class SymbolDrawerDockingCoordinator(
    initialWindow: WindowAdaptiveInfo,
    settings: SymbolDrawerSettings,
    private var bottomCollapsedHeightDp: Float,
    private var bottomExpandedHeightDp: Float,
    private val onLayoutResolved: (SymbolDrawerDockingLayoutSpec) -> Unit
) {
    private var currentSettings = settings
    private var currentWindow = initialWindow

    init {
        require(bottomCollapsedHeightDp >= 0f)
        require(bottomExpandedHeightDp >= bottomCollapsedHeightDp)
    }

    fun updateWindow(value: WindowAdaptiveInfo) {
        if (currentWindow == value) return
        currentWindow = value
        dispatch()
    }

    fun updateSettings(value: SymbolDrawerSettings) {
        if (currentSettings == value) return
        currentSettings = value
        dispatch()
    }

    fun updateBottomGeometry(collapsedHeightDp: Float, expandedHeightDp: Float) {
        require(collapsedHeightDp >= 0f)
        require(expandedHeightDp >= collapsedHeightDp)
        if (bottomCollapsedHeightDp == collapsedHeightDp && bottomExpandedHeightDp == expandedHeightDp) return
        bottomCollapsedHeightDp = collapsedHeightDp
        bottomExpandedHeightDp = expandedHeightDp
        dispatch()
    }

    fun updateFoldingFeature(value: FoldingFeatureInfo) {
        if (currentWindow.foldingFeature == value) return
        currentWindow = currentWindow.copy(foldingFeature = value)
        dispatch()
    }

    fun refresh() = dispatch()

    private fun dispatch() {
        onLayoutResolved(
            SymbolDrawerDockingLayoutResolver.resolve(
                settings = currentSettings,
                window = currentWindow,
                bottomCollapsedHeightDp = bottomCollapsedHeightDp,
                bottomExpandedHeightDp = bottomExpandedHeightDp
            )
        )
    }
}
