/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolDrawerStateTest {

    @Test
    fun initialExpandedSelectionIsAppliedAfterFirstMeasuredGeometry() {
        val state = SymbolDrawerState(initialPage = 2, initiallyExpanded = true)

        assertEquals(2, state.selectedPage)
        state.synchronizeGeometry(
            collapsedHeightPx = 120,
            expandedHeightPx = 720,
            density = 1f
        )

        assertTrue(state.expanded)
        assertEquals(720, state.stateHolder.currentHeightPx)
        assertFalse(state.stateHolder.isAnimating)
    }

    @Test
    fun semanticExpansionCommandsUseTheSharedAnchorHolder() {
        val state = SymbolDrawerState(initialPage = 0, initiallyExpanded = false)
        state.synchronizeGeometry(120, 720, 1f)

        state.expand()

        assertTrue(state.expanded)
        assertEquals("expanded", state.stateHolder.snapshot.targetAnchorId)
        state.stateHolder.finishAnimation(
            "expanded",
            state.stateHolder.activeAnimationGeneration!!
        )

        state.toggle()

        assertFalse(state.expanded)
        assertEquals("peek", state.stateHolder.snapshot.targetAnchorId)
    }

    @Test
    fun geometryChangesPreserveExpansionFractionAndPageSelection() {
        val state = SymbolDrawerState(initialPage = 1, initiallyExpanded = false)
        state.synchronizeGeometry(100, 700, 1f)
        state.stateHolder.synchronizeHeight(400)

        state.synchronizeGeometry(200, 1000, 1f)

        assertEquals(500, state.stateHolder.currentHeightPx)
        assertEquals(1, state.selectedPage)
        assertFalse(state.expanded)
    }
}
