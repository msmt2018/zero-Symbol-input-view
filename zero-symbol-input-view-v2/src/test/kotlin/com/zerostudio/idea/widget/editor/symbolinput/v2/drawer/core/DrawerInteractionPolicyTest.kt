package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerInteractionPolicyTest {
    @Test fun upwardDragIsAcceptedOnlyBelowExpandedBound() {
        assertTrue(DrawerInteractionController.shouldInterceptVerticalDrag(-20f, 180, 80, 300, false))
        assertFalse(DrawerInteractionController.shouldInterceptVerticalDrag(-20f, 300, 80, 300, false))
    }

    @Test fun downwardDragWaitsUntilNestedChildReachesTop() {
        assertFalse(DrawerInteractionController.shouldInterceptVerticalDrag(20f, 300, 80, 300, true))
        assertTrue(DrawerInteractionController.shouldInterceptVerticalDrag(20f, 300, 80, 300, false))
    }

    @Test fun collapsedDrawerDoesNotConsumeDownwardDrag() {
        assertFalse(DrawerInteractionController.shouldInterceptVerticalDrag(20f, 80, 80, 300, false))
    }

    @Test fun zeroMovementAndZeroRangeAreNeverIntercepted() {
        assertFalse(DrawerInteractionController.shouldInterceptVerticalDrag(0f, 180, 80, 300, false))
        assertFalse(DrawerInteractionController.shouldInterceptVerticalDrag(-20f, 80, 80, 80, false))
    }

    @Test fun downwardDragCanCollapseFromAnIntermediateHeight() {
        assertTrue(DrawerInteractionController.shouldInterceptVerticalDrag(1f, 81, 80, 300, false))
    }

    @Test fun upwardDragCanExpandFromTheFirstPixelAbovePeek() {
        assertTrue(DrawerInteractionController.shouldInterceptVerticalDrag(-1f, 81, 80, 300, false))
    }
}
