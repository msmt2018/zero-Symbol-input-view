/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.compose

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SymbolDrawerDetentStateTest {
    @Test
    fun startsAtRequestedStableAnchor() {
        val state = SymbolDrawerDetentState(
            initialDetentId = SymbolDrawerDetent.HalfExpanded.id,
            selectedGroupId = "editing"
        )
        assertEquals("half-expanded", state.targetDetentId)
        assertEquals(0.5f, state.expansionFraction, 0f)
        assertEquals("editing", state.selectedGroupId)
    }

    @Test
    fun settlesToAdjacentAnchorWhenFlinging() = runBlocking {
        val state = SymbolDrawerDetentState()
        state.snapToDetent("half-expanded")
        state.settle(velocityFraction = 1f)
        assertEquals("expanded", state.targetDetentId)
        assertEquals(1f, state.expansionFraction, 0f)

        state.settle(velocityFraction = -1f)
        assertEquals("half-expanded", state.targetDetentId)
        assertEquals(0.5f, state.expansionFraction, 0f)
    }

    @Test
    fun lowVelocityChoosesNearestAnchorAndLowerWinsTie() = runBlocking {
        val state = SymbolDrawerDetentState()
        state.dragBy(0.24f)
        state.settle(velocityFraction = 0f)
        assertEquals("collapsed", state.targetDetentId)

        state.dragBy(0.25f)
        state.settle(velocityFraction = 0f)
        assertEquals("half-expanded", state.targetDetentId)
    }

    @Test
    fun dragIsClampedToNormalizedRange() = runBlocking {
        val state = SymbolDrawerDetentState()
        state.dragBy(2f)
        assertEquals(1f, state.expansionFraction, 0f)
        state.dragBy(-3f)
        assertEquals(0f, state.expansionFraction, 0f)
    }

    @Test
    fun dynamicAnchorReplacementRetainsNearestStablePosition() {
        val state = SymbolDrawerDetentState()
        state.selectGroup("symbols")
        state.updateDetents(
            listOf(
                SymbolDrawerDetent.Collapsed,
                SymbolDrawerDetent("peek", 0.3f),
                SymbolDrawerDetent.Expanded
            )
        )
        assertEquals("collapsed", state.targetDetentId)
        assertEquals("symbols", state.selectedGroupId)
    }

    @Test
    fun rejectsDuplicateIdsAndMissingBoundaryAnchors() {
        assertThrows(IllegalArgumentException::class.java) {
            SymbolDrawerDetentState(
                detents = listOf(
                    SymbolDrawerDetent("same", 0f),
                    SymbolDrawerDetent("same", 1f)
                )
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            SymbolDrawerDetentState(
                detents = listOf(
                    SymbolDrawerDetent("peek", 0.3f),
                    SymbolDrawerDetent.Expanded
                )
            )
        }
    }

    @Test
    fun rejectsNonFiniteDragAndVelocity() = runBlocking {
        val state = SymbolDrawerDetentState()
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { state.dragBy(Float.NaN) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { state.settle(velocityFraction = Float.POSITIVE_INFINITY) }
        }
    }
}
