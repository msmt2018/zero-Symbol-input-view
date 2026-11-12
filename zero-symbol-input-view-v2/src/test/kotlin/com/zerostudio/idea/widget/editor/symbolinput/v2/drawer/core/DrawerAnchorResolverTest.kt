package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerAnchorResolverTest {
    private val input = DrawerAnchorInput(
        minHeightPx = 120,
        maxHeightPx = 900,
        viewportHeightPx = 1000,
        contentHeightPx = 640,
        existingExpandedHeightPx = 720
    )

    @Test fun existingHeightStrategyPreservesCurrentTwoAnchorLayout() {
        val anchors = DrawerAnchorResolver.resolve(ExistingHeightAnchorStrategy(), input)
        assertEquals(listOf(120, 720), anchors.map { it.heightPx })
        assertEquals(listOf(DrawerAnchorState.PEEK, DrawerAnchorState.EXPANDED), anchors.map { it.state })
    }

    @Test fun threeStageStrategyCreatesIndependentThreeAnchors() {
        val anchors = DrawerAnchorResolver.resolve(ThreeStageAnchorStrategy(), input)
        assertEquals(listOf(120, 500, 900), anchors.map { it.heightPx })
        assertEquals(
            listOf(DrawerAnchorState.PEEK, DrawerAnchorState.HALF_EXPANDED, DrawerAnchorState.EXPANDED),
            anchors.map { it.state }
        )
    }

    @Test fun contentBasedStrategyUsesMeasuredContentHeight() {
        val anchors = DrawerAnchorResolver.resolve(ContentBasedAnchorStrategy(), input)
        assertEquals(listOf(120, 640), anchors.map { it.heightPx })
    }

    @Test fun customStrategyClampsAndSortsDeveloperAnchors() {
        val strategy = CustomAnchorStrategy(listOf(
            DrawerAnchor("high", 1200, DrawerAnchorState.EXPANDED),
            DrawerAnchor("low", 40, DrawerAnchorState.PEEK),
            DrawerAnchor("middle", 400, DrawerAnchorState.CUSTOM)
        ))
        val anchors = DrawerAnchorResolver.resolve(strategy, input)
        assertEquals(listOf(120, 400, 900), anchors.map { it.heightPx })
    }

    @Test fun duplicateHeightsAreCollapsed() {
        val anchors = DrawerAnchorResolver.resolve(
            ThreeStageAnchorStrategy(halfExpandedFraction = 0.1f, expandedViewportFraction = 0.2f),
            input.copy(viewportHeightPx = 100, minHeightPx = 120)
        )
        assertEquals(1, anchors.size)
        assertEquals(120, anchors.single().heightPx)
    }

    @Test fun flingMovesAtMostOneAnchorPerGesture() {
        val anchors = DrawerAnchorResolver.resolve(ThreeStageAnchorStrategy(), input)
        val target = DrawerAnchorResolver.flingTarget(anchors, 500, 1600f, 500f)
        assertEquals("expanded", target.id)
    }

    @Test fun nearestAnchorChoosesClosestHeight() {
        val anchors = DrawerAnchorResolver.resolve(ThreeStageAnchorStrategy(), input)
        assertEquals("half", DrawerAnchorResolver.nearestAnchor(anchors, 530).id)
        assertTrue(anchors.zipWithNext().all { (a, b) -> a.heightPx <= b.heightPx })
    }
}
