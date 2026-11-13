package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerStateTest {
    private val anchors = listOf(
        DrawerAnchor("peek", 100, DrawerAnchorState.PEEK),
        DrawerAnchor("half", 500, DrawerAnchorState.HALF_EXPANDED),
        DrawerAnchor("expanded", 900, DrawerAnchorState.EXPANDED)
    )

    @Test fun exposesStablePeekAndExpandedSemantics() {
        val peek = DrawerState("peek", 100, anchors)
        assertTrue(peek.isAtStableAnchor)
        assertTrue(peek.isAtPeekAnchor)
        assertFalse(peek.isExpanded)
        assertEquals(0f, peek.expansionFraction, 0f)

        val expanded = DrawerState("expanded", 900, anchors)
        assertTrue(expanded.isAtStableAnchor)
        assertTrue(expanded.isAtExpandedAnchor)
        assertTrue(expanded.isExpanded)
        assertEquals(1f, expanded.expansionFraction, 0f)
    }

    @Test fun exposesContinuousProgressDuringDragWithoutCallingItStable() {
        val dragging = DrawerState("half", 350, anchors, isDragging = true)
        assertFalse(dragging.isAtStableAnchor)
        assertFalse(dragging.isAtPeekAnchor)
        assertFalse(dragging.isAtExpandedAnchor)
        assertTrue(dragging.isExpanded)
        assertEquals(0.3125f, dragging.expansionFraction, 0.0001f)
    }

    @Test fun clampsProgressAndHandlesSingleAnchorSnapshots() {
        val overExpanded = DrawerState("expanded", 1200, anchors)
        assertEquals(1f, overExpanded.expansionFraction, 0f)

        val single = DrawerState(
            "only",
            120,
            listOf(DrawerAnchor("only", 120, DrawerAnchorState.CUSTOM))
        )
        assertEquals(0f, single.expansionFraction, 0f)
        assertTrue(single.isAtStableAnchor)
    }
}
