package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core

import org.junit.Assert.*
import org.junit.Test

class DrawerInteractionPolicyTest {
    @Test fun upwardDragIncreasesHeightAndClampsAtExpandedBound() {
        val controller = DrawerInteractionController(80, 300)
        controller.beginDrag()
        assertEquals(180, controller.dragBy(-100f))
        assertEquals(300, controller.dragBy(-500f))
        assertEquals(300, controller.endDrag(-1200f, 900f))
        assertFalse(controller.isDragging)
    }

    @Test fun downwardDragDecreasesHeightAndClampsAtCollapsedBound() {
        val controller = DrawerInteractionController(80, 300, 240)
        controller.beginDrag()
        assertEquals(140, controller.dragBy(100f))
        assertEquals(80, controller.dragBy(500f))
        assertEquals(80, controller.endDrag(1100f, 900f))
    }

    @Test fun slowDragSettlesToNearestAnchor() {
        val expanded = DrawerInteractionController(80, 300, 220)
        expanded.beginDrag()
        assertEquals(300, expanded.endDrag(100f, 900f))
        val collapsed = DrawerInteractionController(80, 300, 140)
        collapsed.beginDrag()
        assertEquals(80, collapsed.endDrag(100f, 900f))
    }

    @Test fun cancelAndSynchronizeClearDragState() {
        val controller = DrawerInteractionController(80, 300)
        controller.beginDrag()
        controller.dragBy(-80f)
        assertEquals(160, controller.cancelDrag())
        assertFalse(controller.isDragging)
        assertEquals(250, controller.synchronizeHeight(250))
    }

    @Test fun updatingBoundsCanPreserveExpansionFraction() {
        val controller = DrawerInteractionController(100, 300, 200)
        assertEquals(400, controller.updateBounds(200, 600))
        assertEquals(0.5f, controller.expansionFraction)
        assertEquals(400, controller.updateBounds(200, 300, preserveExpansionFraction = false))
    }

    @Test fun invalidDeltasAndBoundsAreRejected() {
        val controller = DrawerInteractionController(80, 300)
        assertTrue(runCatching { controller.dragBy(Float.NaN) }.isFailure)
        assertTrue(runCatching { controller.updateBounds(300, 200) }.isFailure)
    }
}
