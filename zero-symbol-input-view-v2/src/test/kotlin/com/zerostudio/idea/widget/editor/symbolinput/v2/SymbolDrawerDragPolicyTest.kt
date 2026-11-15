package com.zerostudio.idea.widget.editor.symbolinput.v2

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolDrawerDragPolicyTest {
    @Test fun upwardGestureExpandsOnlyWhileBelowExpandedHeight() {
        assertFalse(SymbolDrawerDragPolicy.shouldInterceptVerticalDrag(
            fingerDeltaY = -24f,
            currentHeightPx = 300,
            collapsedHeightPx = 80,
            expandedHeightPx = 300,
            childCanScrollUp = false
        ))
        assertTrue(SymbolDrawerDragPolicy.shouldInterceptVerticalDrag(
            fingerDeltaY = -24f,
            currentHeightPx = 180,
            collapsedHeightPx = 80,
            expandedHeightPx = 300,
            childCanScrollUp = false
        ))
    }

    @Test fun downwardGestureLetsScrollableChildReachItsStartBeforeCollapsing() {
        assertFalse(SymbolDrawerDragPolicy.shouldInterceptVerticalDrag(
            fingerDeltaY = 24f,
            currentHeightPx = 300,
            collapsedHeightPx = 80,
            expandedHeightPx = 300,
            childCanScrollUp = true
        ))
        assertTrue(SymbolDrawerDragPolicy.shouldInterceptVerticalDrag(
            fingerDeltaY = 24f,
            currentHeightPx = 300,
            collapsedHeightPx = 80,
            expandedHeightPx = 300,
            childCanScrollUp = false
        ))
    }

    @Test fun collapsedDrawerDoesNotConsumeDownwardGestures() {
        assertFalse(SymbolDrawerDragPolicy.shouldInterceptVerticalDrag(
            fingerDeltaY = 24f,
            currentHeightPx = 80,
            collapsedHeightPx = 80,
            expandedHeightPx = 300,
            childCanScrollUp = false
        ))
    }

    @Test fun noMovementOrNoExpansionRangeNeverIntercepts() {
        assertFalse(SymbolDrawerDragPolicy.shouldInterceptVerticalDrag(
            fingerDeltaY = 0f,
            currentHeightPx = 180,
            collapsedHeightPx = 80,
            expandedHeightPx = 300,
            childCanScrollUp = false
        ))
        assertFalse(SymbolDrawerDragPolicy.shouldInterceptVerticalDrag(
            fingerDeltaY = -10f,
            currentHeightPx = 80,
            collapsedHeightPx = 80,
            expandedHeightPx = 80,
            childCanScrollUp = false
        ))
    }
}
