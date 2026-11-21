/*
 * This file is part of zero-Symbol-input-view-V2.
 *
 * Copyright (C) 2026 android_zero
 *
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive

import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolDrawerDockingCoordinatorTest {
    @Test
    fun reentrantWindowUpdateIsDeliveredAfterCurrentLayout() {
        val widths = mutableListOf<Float>()
        lateinit var coordinator: SymbolDrawerDockingCoordinator
        var updated = false
        coordinator = SymbolDrawerDockingCoordinator(
            initialWindow = window(400),
            settings = SymbolDrawerSettings(
                largeScreenLayoutMode = com.zerostudio.idea.widget.editor.symbolinput.v2.settings.LargeScreenLayoutMode.SIDE
            ),
            bottomCollapsedHeightDp = 48f,
            bottomExpandedHeightDp = 240f
        ) { layout ->
            widths += layout.expandedExtentDp
            if (layout.remainingEditorWidthDp == 360f && !updated) {
                updated = true
                coordinator.updateWindow(window(800))
            }
        }

        coordinator.updateWindow(window(600))

        assertEquals(listOf(240f, 440f), widths)
    }

    @Test
    fun invalidBottomGeometryIsRejectedBeforeStateMutation() {
        val widths = mutableListOf<Float>()
        val coordinator = SymbolDrawerDockingCoordinator(
            initialWindow = window(400),
            settings = SymbolDrawerSettings(),
            bottomCollapsedHeightDp = 48f,
            bottomExpandedHeightDp = 240f
        ) { widths += it.collapsedExtentDp }

        assertTrue(runCatching {
            coordinator.updateBottomGeometry(Float.POSITIVE_INFINITY, 300f)
        }.isFailure)
        coordinator.refresh()

        assertEquals(listOf(48f), widths)
    }

    private fun window(width: Int) = WindowAdaptiveInfo(
        widthDp = width.toFloat(),
        heightDp = 700f
    )
}
