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
        assertEquals("expanded", controller.currentState()?.targetAnchorId)
        assertEquals(controller.stateHolder.snapshot, controller.currentState())

        controller.stateHolder.finishAnimation("expanded")
        controller.collapse()
        assertEquals("peek", controller.currentState()?.targetAnchorId)
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
}
