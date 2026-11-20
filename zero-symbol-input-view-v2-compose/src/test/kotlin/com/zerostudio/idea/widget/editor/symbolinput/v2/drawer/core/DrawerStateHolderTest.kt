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
        val target = holder.endDrag(0f, 100f)
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
        val target = holder.endDrag(-500f, 100f)
        assertEquals("expanded", target.id)
        assertTrue(holder.isAnimating)
        holder.finishAnimation(target.id)
        assertEquals(800, holder.currentHeightPx)
        assertTrue(holder.snapshot.isAtStableAnchor)
    }



    @Test
    fun semanticNavigationUsesResolvedAnchorSet() {
        val holder = DrawerStateHolder(
            initialInput = input(),
            initialStrategy = ThreeStageAnchorStrategy(useAutomaticPeekHeight = false),
            initialAnchorId = "half"
        )

        assertEquals("expanded", holder.expandToMaxAnchor()?.id)
        assertEquals("expanded", holder.snapshot.targetAnchorId)
        holder.finishAnimation("expanded")

        assertEquals(null, holder.moveToAdjacentAnchor(expand = true))
        assertEquals("peek", holder.collapseToMinAnchor()?.id)
        holder.finishAnimation("peek")
        assertEquals("half", holder.moveToAdjacentAnchor(expand = true)?.id)
    }

    @Test
    fun adjacentNavigationDoesNotSkipAnchorFromIntermediateHeight() {
        val holder = DrawerStateHolder(
            initialInput = input(),
            initialStrategy = ThreeStageAnchorStrategy(useAutomaticPeekHeight = false),
            initialAnchorId = "half"
        )
        holder.beginDrag()
        holder.dragBy(-20f)
        val target = holder.moveToAdjacentAnchor(expand = true)
        assertEquals("expanded", target?.id)
    }

    @Test
    fun animationTargetIsDistinctFromNearestCurrentAnchorAndClearedOnFinish() {
        val holder = DrawerStateHolder(
            initialInput = input(),
            initialStrategy = ThreeStageAnchorStrategy(useAutomaticPeekHeight = false),
            initialAnchorId = "half"
        )

        val target = holder.moveToAnchor("expanded")
        assertEquals("expanded", target?.id)
        assertEquals("expanded", holder.snapshot.targetAnchorId)
        assertEquals("half", holder.snapshot.currentAnchorId)
        assertTrue(holder.snapshot.isAnimating)

        holder.updateAnimatedHeight(550)
        assertEquals("expanded", holder.snapshot.targetAnchorId)
        assertEquals("half", holder.snapshot.currentAnchorId)

        holder.finishAnimation("expanded")
        assertEquals(null, holder.snapshot.targetAnchorId)
        assertEquals("expanded", holder.snapshot.currentAnchorId)
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
    fun cancelDragRetainsContinuousHeightAndSynchronizeClearsTransitionState() {
        val holder = DrawerStateHolder(
            initialInput = input(),
            initialStrategy = ThreeStageAnchorStrategy(useAutomaticPeekHeight = false),
            initialAnchorId = "half"
        )
        holder.dragBy(-35f)
        assertEquals(435, holder.currentHeightPx)
        assertEquals(435, holder.cancelDrag())
        assertFalse(holder.isDragging)
        assertFalse(holder.isAnimating)
        assertEquals(null, holder.snapshot.targetAnchorId)

        assertEquals(800, holder.synchronizeHeight(900))
        assertEquals(800, holder.currentHeightPx)
        assertTrue(holder.snapshot.isAtStableAnchor)
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

    @Test
    fun fractionalDragDeltasAccumulateInsteadOfBeingDiscarded() {
        val holder = DrawerStateHolder(
            initialInput = input(),
            initialStrategy = ExistingHeightAnchorStrategy(),
            initialAnchorId = "expanded"
        )
        holder.beginDrag()
        repeat(4) { holder.dragBy(0.25f) }
        assertEquals(799, holder.currentHeightPx)
        repeat(4) { holder.dragBy(-0.25f) }
        assertEquals(800, holder.currentHeightPx)
    }


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
        assertEquals(1000, holder.currentHeightPx)
        assertEquals(-500f, result.consumedDeltaY, 0f)
        assertEquals(-9_500f, result.remainingDeltaY, 0f)
        assertTrue(holder.isDragging)
    }

    @Test
    fun nestedScrollPreScrollNeverConsumesDownwardMotionRegardlessOfChildFlag() {
        val holder = DrawerStateHolder(
            input().copy(minHeightPx = 100, maxHeightPx = 1000, existingExpandedHeightPx = 1000),
            ThreeStageAnchorStrategy(useAutomaticPeekHeight = false),
            "half"
        )
        val before = holder.currentHeightPx
        val result = holder.dispatchNestedScroll(40f, true, DrawerNestedScrollPhase.PRE_SCROLL)
        assertEquals(0f, result.consumedDeltaY, 0f)
        assertEquals(40f, result.remainingDeltaY, 0f)
        assertEquals(before, holder.currentHeightPx)
        assertFalse(holder.isDragging)
    }
}

private fun DrawerStateHolder.collapseToPeekForTest() {
    val peek = anchors.minBy { it.heightPx }
    finishAnimation(peek.id)
}
