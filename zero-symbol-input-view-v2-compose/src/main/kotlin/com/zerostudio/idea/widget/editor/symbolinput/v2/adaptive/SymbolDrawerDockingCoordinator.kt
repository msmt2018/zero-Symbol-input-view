/*
 * This file is part of zero-Symbol-input-view-V2.
 *
 * Copyright (C) 2026 android_zero
 *
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive

import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
import java.util.ArrayDeque

/**
 * Pure docking state coordinator. Window measurements are supplied by Compose.
 *
 * Calls are confined to the caller's serialized UI/state context. Reentrant
 * updates are queued so each resolved layout is delivered in update order.
 */
class SymbolDrawerDockingCoordinator(
    initialWindow: WindowAdaptiveInfo,
    settings: SymbolDrawerSettings,
    private var bottomCollapsedHeightDp: Float,
    private var bottomExpandedHeightDp: Float,
    private val onLayoutResolved: (SymbolDrawerDockingLayoutSpec) -> Unit
) {
    private var currentSettings = settings
    private var currentWindow = initialWindow
    private val pendingLayouts = ArrayDeque<SymbolDrawerDockingLayoutSpec>()
    private var dispatching = false

    init {
        requireFiniteNonNegative(bottomCollapsedHeightDp, "bottomCollapsedHeightDp")
        requireFiniteNonNegative(bottomExpandedHeightDp, "bottomExpandedHeightDp")
        require(bottomExpandedHeightDp >= bottomCollapsedHeightDp) {
            "bottomExpandedHeightDp must be >= bottomCollapsedHeightDp"
        }
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
        requireFiniteNonNegative(collapsedHeightDp, "collapsedHeightDp")
        requireFiniteNonNegative(expandedHeightDp, "expandedHeightDp")
        require(expandedHeightDp >= collapsedHeightDp) {
            "expandedHeightDp must be >= collapsedHeightDp"
        }
        if (bottomCollapsedHeightDp == collapsedHeightDp &&
            bottomExpandedHeightDp == expandedHeightDp
        ) return
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
        pendingLayouts.addLast(
            SymbolDrawerDockingLayoutResolver.resolve(
                settings = currentSettings,
                window = currentWindow,
                bottomCollapsedHeightDp = bottomCollapsedHeightDp,
                bottomExpandedHeightDp = bottomExpandedHeightDp
            )
        )
        if (dispatching) return

        dispatching = true
        var failure: Throwable? = null
        try {
            while (pendingLayouts.isNotEmpty()) {
                try {
                    onLayoutResolved(pendingLayouts.removeFirst())
                } catch (error: Throwable) {
                    val previous = failure
                    if (previous == null) failure = error else previous.addSuppressed(error)
                }
            }
        } finally {
            dispatching = false
        }
        failure?.let { throw it }
    }

    private fun requireFiniteNonNegative(value: Float, name: String) {
        require(value.isFinite() && value >= 0f) {
            "$name must be finite and >= 0"
        }
    }
}
