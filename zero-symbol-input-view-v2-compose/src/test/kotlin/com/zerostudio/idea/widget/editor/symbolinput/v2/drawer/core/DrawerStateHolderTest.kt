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
        holder.finishAnimation(target.id, holder.activeAnimationGeneration!!)
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
        holder.finishAnimation("expanded", holder.activeAnimationGeneration!!)

        assertEquals(null, holder.moveToAdjacentAnchor(expand = true))
        assertEquals("peek", holder.collapseToMinAnchor()?.id)
        holder.finishAnimation("peek", holder.activeAnimationGeneration!!)
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
    fun animationFrameWithoutTargetDoesNotCreatePhantomAnimatingState() {
        val holder = DrawerStateHolder(input(), ThreeStageAnchorStrategy(useAutomaticPeekHeight = false), "half")
        assertFalse(holder.updateAnimatedHeight(450, 0L))
        // The requested initial anchor is "half" (400px); a rejected frame
        // must leave that exact initial height unchanged.
        assertEquals(400, holder.currentHeightPx)
        assertFalse(holder.isAnimating)
        assertEquals(null, holder.snapshot.targetAnchorId)
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

        val generation = holder.activeAnimationGeneration!!
        assertTrue(holder.updateAnimatedHeight(550, generation))
        assertEquals(550, holder.currentHeightPx)
        assertEquals("expanded", holder.snapshot.targetAnchorId)
        assertEquals("half", holder.snapshot.currentAnchorId)

        holder.finishAnimation("expanded", generation)
        assertEquals(null, holder.snapshot.targetAnchorId)
        assertEquals("expanded", holder.snapshot.currentAnchorId)
        assertTrue(holder.snapshot.isAtStableAnchor)
    }

    @Test
    fun staleAnimationCompletionCannotOverrideNewerTarget() {
        val holder = DrawerStateHolder(
            initialInput = input(),
            initialStrategy = ThreeStageAnchorStrategy(useAutomaticPeekHeight = false),
            initialAnchorId = "half"
        )

        holder.moveToAnchor("expanded")
        val staleGeneration = holder.activeAnimationGeneration!!
        holder.moveToAnchor("peek")
        val currentGeneration = holder.activeAnimationGeneration!!
        holder.finishAnimation("expanded", staleGeneration)

        assertEquals(400, holder.currentHeightPx)
        assertEquals("peek", holder.snapshot.targetAnchorId)
        assertTrue(holder.isAnimating)

        holder.finishAnimation("peek", currentGeneration)
        assertEquals(80, holder.currentHeightPx)
        assertFalse(holder.isAnimating)
    }


    @Test
    fun staleCompletionForSameAnchorCannotFinishNewAnimation() {
        val holder = DrawerStateHolder(
            input(),
            ThreeStageAnchorStrategy(useAutomaticPeekHeight = false),
            "half"
        )

        holder.moveToAnchor("expanded")
        val firstGeneration = holder.activeAnimationGeneration!!
        holder.moveToAnchor("peek")
        holder.moveToAnchor("expanded")
        val latestGeneration = holder.activeAnimationGeneration!!

        assertTrue(latestGeneration > firstGeneration)
        assertFalse(holder.finishAnimation("expanded", firstGeneration))
        assertEquals("expanded", holder.snapshot.targetAnchorId)
        assertTrue(holder.isAnimating)

        assertTrue(holder.finishAnimation("expanded", latestGeneration))
        assertEquals(800, holder.currentHeightPx)
        assertFalse(holder.isAnimating)
    }


    @Test
    fun staleAnimationFrameCannotOverwriteNewerAnimation() {
        val holder = DrawerStateHolder(
            input(),
            ThreeStageAnchorStrategy(useAutomaticPeekHeight = false),
            "half"
        )
        holder.moveToAnchor("expanded")
        val staleGeneration = holder.activeAnimationGeneration!!
        holder.moveToAnchor("peek")
        val activeGeneration = holder.activeAnimationGeneration!!

        assertFalse(holder.updateAnimatedHeight(700, staleGeneration))
        assertEquals(400, holder.currentHeightPx)
        assertTrue(holder.updateAnimatedHeight(300, activeGeneration))
        assertEquals(300, holder.currentHeightPx)
        assertEquals("peek", holder.snapshot.targetAnchorId)
    }

    @Test
    fun animationFrameIsRejectedAfterTransitionIsCancelled() {
        val holder = DrawerStateHolder(
            input(),
            ThreeStageAnchorStrategy(useAutomaticPeekHeight = false),
            "half"
        )
        holder.moveToAnchor("expanded")
        val generation = holder.activeAnimationGeneration!!
        holder.cancelDrag()

        assertFalse(holder.updateAnimatedHeight(700, generation))
        assertEquals(400, holder.currentHeightPx)
        assertFalse(holder.isAnimating)
    }

    @Test
    fun cancelledAnimationGenerationCannotBeReusedBySameTarget() {
        val holder = DrawerStateHolder(
            input(),
            ThreeStageAnchorStrategy(useAutomaticPeekHeight = false),
            "half"
        )

        holder.moveToAnchor("expanded")
        val cancelledGeneration = holder.activeAnimationGeneration!!
        holder.cancelDrag()
        holder.moveToAnchor("expanded")
        val replacementGeneration = holder.activeAnimationGeneration!!

        assertTrue(replacementGeneration > cancelledGeneration)
        assertFalse(holder.updateAnimatedHeight(700, cancelledGeneration))
        assertEquals(400, holder.currentHeightPx)
        assertTrue(holder.updateAnimatedHeight(600, replacementGeneration))
        assertEquals(600, holder.currentHeightPx)
    }

    @Test
    fun changingAnchorStrategyPreservesExpansionFraction() {
        val oldStrategy = CustomAnchorStrategy(
            listOf(
                DrawerAnchor("peek", 100, DrawerAnchorState.PEEK),
                DrawerAnchor("expanded", 500, DrawerAnchorState.EXPANDED)
            )
        )
        val newStrategy = CustomAnchorStrategy(
            listOf(
                DrawerAnchor("peek", 200, DrawerAnchorState.PEEK),
                DrawerAnchor("expanded", 1000, DrawerAnchorState.EXPANDED)
            )
        )
        val holder = DrawerStateHolder(
            initialInput = input(),
            initialStrategy = oldStrategy,
            initialAnchorId = "expanded"
        )
        holder.synchronizeHeight(300)

        holder.setAnchorStrategy(newStrategy)

        // Strategy output is bounded by the measured 800px maximum: 200..800 midpoint = 500.
        assertEquals(500, holder.currentHeightPx)
        assertFalse(holder.isDragging)
        assertFalse(holder.isAnimating)
        assertEquals(null, holder.snapshot.targetAnchorId)
    }

    @Test
    fun equivalentGeometryUpdatePreservesActiveDragAndAnimation() {
        val geometry = input()
        val holder = DrawerStateHolder(
            geometry,
            ThreeStageAnchorStrategy(useAutomaticPeekHeight = false),
            "half"
        )
        holder.beginDrag()
        holder.dragBy(-20f)
        val draggedHeight = holder.currentHeightPx

        holder.updateGeometry(geometry)

        assertEquals(draggedHeight, holder.currentHeightPx)
        assertTrue(holder.isDragging)

        holder.cancelDrag()
        holder.moveToAnchor("expanded")
        val generation = holder.activeAnimationGeneration
        holder.updateGeometry(geometry)

        assertEquals("expanded", holder.snapshot.targetAnchorId)
        assertEquals(generation, holder.activeAnimationGeneration)
        assertTrue(holder.isAnimating)
    }

    @Test
    fun equivalentAnchorStrategyUpdatePreservesActiveDrag() {
        val strategy = ThreeStageAnchorStrategy(useAutomaticPeekHeight = false)
        val holder = DrawerStateHolder(input(), strategy, "half")
        holder.beginDrag()
        holder.dragBy(-18f)
        val heightDuringDrag = holder.currentHeightPx

        holder.setAnchorStrategy(ThreeStageAnchorStrategy(useAutomaticPeekHeight = false))

        assertEquals(heightDuringDrag, holder.currentHeightPx)
        assertTrue(holder.isDragging)
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
    fun listenerCanRemoveItselfDuringStateDispatch() {
        val holder = DrawerStateHolder(input())
        var callbackCount = 0
        lateinit var listener: (DrawerState) -> Unit
        listener = { state ->
            callbackCount++
            if (state.isDragging) {
                holder.removeStateListener(listener)
            }
        }

        holder.addStateListener(listener)
        holder.beginDrag()
        holder.dragBy(-1f)

        assertEquals(2, callbackCount)
        assertTrue(holder.isDragging)
    }

    @Test
    fun failingStateListenerDoesNotPreventOtherListenersFromReceivingSnapshot() {
        val holder = DrawerStateHolder(input())
        var secondListenerCalls = 0
        holder.addStateListener {
            if (it.isDragging) throw IllegalStateException("observer failure")
        }
        holder.addStateListener { secondListenerCalls++ }

        try {
            holder.beginDrag()
            throw AssertionError("Expected observer failure to be reported")
        } catch (expected: IllegalStateException) {
            assertEquals("observer failure", expected.message)
        }

        assertEquals(2, secondListenerCalls)
        assertTrue(holder.isDragging)
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
        // The half anchor is 400px and the expanded bound is 1000px: 600px can be consumed.
        assertEquals(-600f, result.consumedDeltaY, 0f)
        assertEquals(-9_400f, result.remainingDeltaY, 0f)
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
    snapToAnchor(peek.id)
}
