package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core

import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.config.DrawerPresentationMode
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerPage
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.panel.DrawerPanelContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultDrawerControllerTest {
    private fun input() = DrawerAnchorInput(
        minHeightPx = 80,
        maxHeightPx = 800,
        viewportHeightPx = 800,
        contentHeightPx = 600
    )

    private fun controller(): DefaultDrawerController {
        val geometry = input()
        val holder = DrawerStateHolder(
            initialInput = geometry,
            initialStrategy = ThreeStageAnchorStrategy(useAutomaticPeekHeight = false),
            initialAnchorId = "peek"
        )
        return DefaultDrawerController(holder) { geometry }
    }

    @Test
    fun controllerConfigurationStartsWithTheHoldersActiveAnchorStrategy() {
        val controller = controller()
        assertEquals(controller.stateHolder.anchorStrategy, controller.config.anchorStrategy)
        assertEquals(ThreeStageAnchorStrategy(useAutomaticPeekHeight = false), controller.config.anchorStrategy)
    }

    @Test
    fun semanticCommandsDelegateToTheSingleStateHolder() {
        val controller = controller()
        controller.expand(animated = false)
        assertEquals("expanded", controller.currentState()?.currentAnchorId)
        assertEquals(null, controller.currentState()?.targetAnchorId)
        assertTrue(controller.currentState()?.isAtStableAnchor == true)
        assertEquals(controller.stateHolder.snapshot, controller.currentState())

        controller.collapse()
        assertEquals("peek", controller.currentState()?.targetAnchorId)
    }


    @Test
    fun controllerExposesTheCurrentAnimationGeneration() {
        val controller = controller()
        assertEquals(null, controller.activeAnimationGeneration())

        controller.expand(animated = true)
        val generation = controller.activeAnimationGeneration()

        assertTrue(generation != null)
        assertEquals(generation, controller.stateHolder.activeAnimationGeneration)
        assertTrue(controller.stateHolder.finishAnimation("expanded", generation!!))
        assertEquals(null, controller.activeAnimationGeneration())
    }

    @Test
    fun presentationConfigurationDoesNotMutateGeometryState() {
        val controller = controller()
        val heightBefore = controller.stateHolder.currentHeightPx

        controller.setPresentationMode(DrawerPresentationMode.FULL_SCREEN)
        controller.setDragEnabled(false)
        controller.setDismissOnBack(false)

        assertEquals(DrawerPresentationMode.FULL_SCREEN, controller.config.presentationMode)
        assertFalse(controller.config.enableDrag)
        assertFalse(controller.config.dismissOnBack)
        assertEquals(heightBefore, controller.stateHolder.currentHeightPx)
    }

    @Test
    fun pageSelectionIsOwnedByNavigationController() {
        val controller = controller()
        controller.setPages(
            listOf(
                DrawerPage("first", "First", DrawerPanelContent {}),
                DrawerPage("second", "Second", DrawerPanelContent {})
            )
        )

        assertTrue(controller.selectPage("second"))
        assertEquals("second", controller.navigationController.selectedPage()?.id)
        assertFalse(controller.selectPage("missing"))
    }

    @Test
    fun refreshUsesHostSuppliedGeometry() {
        var geometry = input()
        val holder = DrawerStateHolder(geometry)
        val controller = DefaultDrawerController(holder) { geometry }

        geometry = geometry.copy(maxHeightPx = 1000, existingExpandedHeightPx = 1000)
        controller.refreshAnchors()

        assertEquals(1000, holder.anchors.maxOf { it.heightPx })
    }
    @Test
    fun presentationOnlyConfigUpdateDoesNotCancelActiveDrag() {
        val controller = controller()
        controller.stateHolder.beginDrag()
        controller.stateHolder.dragBy(-24f)
        val heightDuringDrag = controller.stateHolder.currentHeightPx

        controller.applyConfig(controller.config.copy(enableDrag = false))

        assertEquals(heightDuringDrag, controller.stateHolder.currentHeightPx)
        assertTrue(controller.stateHolder.isDragging)
        assertFalse(controller.config.enableDrag)
    }

    @Test
    fun reapplyingEquivalentAnchorStrategyDoesNotCancelActiveDrag() {
        val controller = controller()
        controller.stateHolder.beginDrag()
        controller.stateHolder.dragBy(-16f)
        val heightDuringDrag = controller.stateHolder.currentHeightPx
        val equivalentStrategy = ThreeStageAnchorStrategy(useAutomaticPeekHeight = false)

        controller.setAnchorStrategy(equivalentStrategy)

        assertEquals(heightDuringDrag, controller.stateHolder.currentHeightPx)
        assertTrue(controller.stateHolder.isDragging)
    }

}
