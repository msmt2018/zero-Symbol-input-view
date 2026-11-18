/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerStateHolderTest {

    private fun input() = DrawerAnchorInput(
        minHeightPx = 80,
        maxHeightPx = 800,
        viewportHeightPx = 800,
        contentHeightPx = 600,
        viewportWidthPx = 400
    )

    @Test
    fun initialStateUsesRequestedAnchor() {
        val holder = DrawerStateHolder(
            initialInput = input(),
            initialStrategy = ThreeStageAnchorStrategy(useAutomaticPeekHeight = false),
            initialAnchorId = "half"
        )

        assertEquals("half", holder.snapshot.currentAnchorId)
        assertEquals(400, holder.currentHeightPx)
        assertFalse(holder.isDragging)
    }

    @Test
    fun dragAndSettleUseAllResolvedAnchors() {
        val holder = DrawerStateHolder(
            initialInput = input(),
            initialStrategy = ThreeStageAnchorStrategy(useAutomaticPeekHeight = false),
            initialAnchorId = "half"
        )

        holder.beginDrag()
        holder.dragBy(-30f)
        assertEquals(430, holder.currentHeightPx)
        assertTrue(holder.isDragging)

        val target = holder.endDrag(
            fingerVelocityY = 0f,
            minimumFlingVelocityPxPerSecond = 100f
        )
        assertEquals("half", target.id)
        assertFalse(holder.isDragging)
    }

    @Test
    fun upwardFlingAdvancesOneAnchorFromIntermediateHeight() {
        val holder = DrawerStateHolder(
            initialInput = input(),
            initialStrategy = ThreeStageAnchorStrategy(useAutomaticPeekHeight = false),
            initialAnchorId = "half"
        )

        holder.beginDrag()
        holder.dragBy(-30f)
        val target = holder.endDrag(
            fingerVelocityY = -500f,
            minimumFlingVelocityPxPerSecond = 100f
        )

        assertEquals("expanded", target.id)
        assertTrue(holder.isAnimating)
        holder.finishAnimation(target.id)
        assertEquals(800, holder.currentHeightPx)
        assertTrue(holder.snapshot.isAtStableAnchor)
    }

    @Test
    fun geometryChangePreservesExpansionFraction() {
        val holder = DrawerStateHolder(
            initialInput = input(),
            initialStrategy = ExistingHeightAnchorStrategy(),
            initialAnchorId = "expanded"
        )

        holder.updateGeometry(
            input().copy(minHeightPx = 100, maxHeightPx = 1000, existingExpandedHeightPx = 1000),
            preserveExpansionFraction = true
        )

        assertEquals(1000, holder.currentHeightPx)
        assertNotNull(holder.snapshot.currentAnchor)
    }

    @Test
    fun listenersReceiveInitialAndSubsequentSnapshots() {
        val holder = DrawerStateHolder(input())
        val observed = mutableListOf<DrawerState>()
        val listener: (DrawerState) -> Unit = { observed += it }

        holder.addStateListener(listener)
        holder.beginDrag()
        holder.removeStateListener(listener)
        holder.collapseToPeekForTest()

        assertTrue(observed.size >= 2)
        assertTrue(observed.last().isDragging)
    }
}

private fun DrawerStateHolder.collapseToPeekForTest() {
    val peek = anchors.minBy { it.heightPx }
    finishAnimation(peek.id)
    @Test
    fun nestedScrollMutatesTheSingleHolderStateAndReturnsRemainder() {
        val holder = DrawerStateHolder(
            input().copy(minHeightPx = 100, maxHeightPx = 1000, existingExpandedHeightPx = 1000),
            ThreeStageAnchorStrategy(useAutomaticPeekHeight = false),
            "half"
        )
        val result = holder.dispatchNestedScroll(
            availableDeltaY = -10_000f,
            childCanScrollBackward = false,
            phase = DrawerNestedScrollPhase.PRE_SCROLL
        )
        assertEquals(-500, holder.currentHeightPx)
        assertEquals(-500f, result.consumedDeltaY, 0f)
        assertEquals(-9_500f, result.remainingDeltaY, 0f)
        assertEquals(true, holder.isDragging)
    }

    @Test
    fun nestedScrollLeavesDownwardMotionToChildWhenChildCanScrollBackward() {
        val holder = DrawerStateHolder(
            input().copy(minHeightPx = 100, maxHeightPx = 1000, existingExpandedHeightPx = 1000),
            ThreeStageAnchorStrategy(useAutomaticPeekHeight = false),
            "half"
        )
        val before = holder.currentHeightPx
        val result = holder.dispatchNestedScroll(
            availableDeltaY = 40f,
            childCanScrollBackward = true,
            phase = DrawerNestedScrollPhase.PRE_SCROLL
        )
        assertEquals(0f, result.consumedDeltaY, 0f)
        assertEquals(40f, result.remainingDeltaY, 0f)
        assertEquals(before, holder.currentHeightPx)
        assertEquals(false, holder.isDragging)
    }

}
