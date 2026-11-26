package com.zerostudio.idea.widget.drawer.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@Suppress("DEPRECATION")
class DrawerInteractionControllerTest {

    @Test
    fun legacyFacadeDelegatesAllRuntimeStateToItsSingleAnchorHolder() {
        val controller = DrawerInteractionController(
            minimumHeightPx = 80,
            maximumHeightPx = 800,
            initialHeightPx = 400
        )

        assertEquals(80, controller.minimumHeightPx)
        assertEquals(800, controller.maximumHeightPx)
        assertEquals(400, controller.currentHeightPx)
        assertEquals(320f / 720f, controller.expansionFraction, 0.0001f)
        assertFalse(controller.isDragging)

        controller.beginDrag()
        assertTrue(controller.isDragging)
        assertEquals(425, controller.dragBy(-25f))
        assertEquals(425, controller.currentHeightPx)

        assertEquals(425, controller.cancelDrag())
        assertFalse(controller.isDragging)
    }

    @Test
    fun updatingBoundsPreservesExpansionFractionByDefault() {
        val controller = DrawerInteractionController(
            minimumHeightPx = 100,
            maximumHeightPx = 500,
            initialHeightPx = 300
        )

        assertEquals(0.5f, controller.expansionFraction, 0.0001f)
        assertEquals(500, controller.updateBounds(200, 800))
        assertEquals(500, controller.currentHeightPx)
        assertEquals(0.5f, controller.expansionFraction, 0.0001f)
    }

    @Test
    fun updatingBoundsCanClampAbsoluteHeightWithoutPreservingFraction() {
        val controller = DrawerInteractionController(
            minimumHeightPx = 100,
            maximumHeightPx = 500,
            initialHeightPx = 300
        )

        assertEquals(300, controller.updateBounds(
            minimumHeightPx = 200,
            maximumHeightPx = 800,
            preserveExpansionFraction = false
        ))
        assertEquals(300, controller.currentHeightPx)
        assertEquals(1f / 6f, controller.expansionFraction, 0.0001f)
    }

    @Test
    fun synchronizationClampsExternalHeightToCurrentResolvedBounds() {
        val controller = DrawerInteractionController(
            minimumHeightPx = 80,
            maximumHeightPx = 800,
            initialHeightPx = 300
        )

        assertEquals(80, controller.synchronizeHeight(-1))
        assertEquals(800, controller.synchronizeHeight(900))
    }

    @Test
    fun invalidBoundsAreRejectedBeforeStateChanges() {
        val controller = DrawerInteractionController(
            minimumHeightPx = 80,
            maximumHeightPx = 800,
            initialHeightPx = 300
        )

        val before = controller.currentHeightPx
        val invalidMinimum = runCatching {
            controller.updateBounds(minimumHeightPx = -1, maximumHeightPx = 800)
        }
        val invalidRange = runCatching {
            controller.updateBounds(minimumHeightPx = 900, maximumHeightPx = 800)
        }

        assertTrue(invalidMinimum.isFailure)
        assertTrue(invalidRange.isFailure)
        assertEquals(before, controller.currentHeightPx)
    }
}
