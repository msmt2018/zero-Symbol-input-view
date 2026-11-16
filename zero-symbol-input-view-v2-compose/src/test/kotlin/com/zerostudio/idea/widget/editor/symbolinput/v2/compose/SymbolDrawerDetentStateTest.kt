/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.compose

import androidx.compose.runtime.MonotonicFrameClock
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
    fun settlesToAdjacentAnchorWhenFlinging() = runBlocking(AdvancingFrameClock()) {
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
    fun lowVelocityChoosesNearestAnchorAndLowerWinsTie() = runBlocking(AdvancingFrameClock()) {
        val state = SymbolDrawerDetentState()
        state.dragBy(0.25f)
        state.settle(velocityFraction = 0f)
        assertEquals("collapsed", state.targetDetentId)

        state.dragBy(0.26f)
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
    fun dynamicAnchorReplacementRetainsNearestStablePosition() = runBlocking {
        val state = SymbolDrawerDetentState(
            initialDetentId = SymbolDrawerDetent.HalfExpanded.id
        )
        state.selectGroup("symbols")
        state.updateDetents(
            listOf(
                SymbolDrawerDetent.Collapsed,
                SymbolDrawerDetent("peek", 0.4f),
                SymbolDrawerDetent.Expanded
            )
        )
        assertEquals("peek", state.targetDetentId)
        assertEquals("symbols", state.selectedGroupId)
        assertEquals("peek", state.currentDetentId)
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
    fun updatingFractionOfStableAnchorRepositionsSettledDrawer() = runBlocking {
        val state = SymbolDrawerDetentState(
            detents = listOf(
                SymbolDrawerDetent.Collapsed,
                SymbolDrawerDetent("half-expanded", 0.5f),
                SymbolDrawerDetent.Expanded
            ),
            initialDetentId = "half-expanded"
        )
        state.updateDetents(
            listOf(
                SymbolDrawerDetent.Collapsed,
                SymbolDrawerDetent("half-expanded", 0.62f),
                SymbolDrawerDetent.Expanded
            )
        )
        assertEquals("half-expanded", state.targetDetentId)
        assertEquals(0.62f, state.expansionFraction, 0f)
    }

    @Test
    fun saverRestoresStableTargetAndSelectedGroup() {
        val original = SymbolDrawerDetentState(
            initialDetentId = "half-expanded",
            selectedGroupId = "group-b"
        )
        val saver = SymbolDrawerDetentState.saver(SymbolDrawerDetent.Defaults)
        val restored = saver.restore(
            listOf(
                original.targetDetentId,
                original.currentDetentId,
                original.selectedGroupId.orEmpty()
            )
        )
        assertEquals("half-expanded", restored?.targetDetentId)
        assertEquals("group-b", restored?.selectedGroupId)
    }

    @Test
    fun rejectsNonFiniteDragAndVelocity(): Unit = runBlocking {
        val state = SymbolDrawerDetentState()
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { state.dragBy(Float.NaN) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { state.settle(velocityFraction = Float.POSITIVE_INFINITY) }
        }
    }
    private class AdvancingFrameClock : MonotonicFrameClock {
        private var frameTimeNanos = 0L

        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
            frameTimeNanos += 16_666_667L
            return onFrame(frameTimeNanos)
        }
    }
}
