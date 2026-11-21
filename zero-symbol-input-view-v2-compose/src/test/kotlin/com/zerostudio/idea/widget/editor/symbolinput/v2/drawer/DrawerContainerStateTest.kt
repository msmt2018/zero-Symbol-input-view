package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer

import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core.DrawerAnchorInput
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core.ThreeStageAnchorStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerContainerStateTest {
    private fun geometry() = DrawerAnchorInput(
        minHeightPx = 80,
        maxHeightPx = 800,
        viewportHeightPx = 800,
        contentHeightPx = 800,
        existingExpandedHeightPx = 800
    )

    @Test
    fun collapsedContainerDelegatesExpansionToSharedAnchorEngine() {
        val state = DrawerContainerState(initiallyExpanded = false)
        state.synchronizeGeometry(geometry(), ThreeStageAnchorStrategy())

        assertFalse(state.expanded)
        assertEquals("peek", state.stateHolder.snapshot.currentAnchorId)

        state.expand()

        assertEquals("expanded", state.stateHolder.snapshot.targetAnchorId)
        assertTrue(state.expanded)
        assertTrue(state.stateHolder.isAnimating)
    }

    @Test
    fun expandedContainerInitializesAtTheMaximumResolvedAnchor() {
        val state = DrawerContainerState(initiallyExpanded = true)
        state.synchronizeGeometry(geometry(), ThreeStageAnchorStrategy())

        assertTrue(state.expanded)
        assertEquals("expanded", state.stateHolder.snapshot.currentAnchorId)
        assertTrue(state.stateHolder.snapshot.isAtStableAnchor)
    }

    @Test
    fun toggleUsesTheSharedStateHolderInsteadOfLocalExpansionState() {
        val state = DrawerContainerState(initiallyExpanded = false)
        state.synchronizeGeometry(geometry(), ThreeStageAnchorStrategy())

        state.toggle()
        assertEquals("expanded", state.stateHolder.snapshot.targetAnchorId)

        val generation = state.stateHolder.activeAnimationGeneration!!
        assertTrue(state.stateHolder.finishAnimation("expanded", generation))

        state.toggle()
        assertEquals("peek", state.stateHolder.snapshot.targetAnchorId)
    }

    @Test
    fun geometryChangesPreserveExpansionFractionThroughTheCoreEngine() {
        val state = DrawerContainerState(initiallyExpanded = false)
        state.synchronizeGeometry(geometry(), ThreeStageAnchorStrategy())
        state.stateHolder.synchronizeHeight(440)

        val resized = geometry().copy(maxHeightPx = 1000, viewportHeightPx = 1000)
        state.synchronizeGeometry(resized, ThreeStageAnchorStrategy())

        assertEquals(540, state.stateHolder.currentHeightPx)
    }
}
