package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation

import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.panel.DrawerPanelContent
import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerNavigationControllerTest {
    private fun page(id: String) = DrawerPage(id, id, object : DrawerPanelContent {
        override fun createView(): View = error("View must not be created by navigation state")
    })

    @Test fun selectsPagesAndPreservesSelectionAcrossUpdates() {
        val controller = DrawerNavigationController()
        controller.submitPages(listOf(page("one"), page("two")))
        assertEquals("one", controller.selectedPage()?.id)
        assertTrue(controller.selectPage("two"))
        controller.submitPages(listOf(page("two"), page("three")))
        assertEquals("two", controller.selectedPage()?.id)
    }

    @Test fun rejectsUnknownPageAndInvalidIndex() {
        val controller = DrawerNavigationController()
        controller.submitPages(listOf(page("one")))
        assertFalse(controller.selectPage("missing"))
        assertFalse(controller.selectPage(4))
        assertEquals(0, controller.selectedIndex())
    }

    @Test fun observersReceiveInitialAndUpdatedSnapshots() {
        val controller = DrawerNavigationController()
        val selected = mutableListOf<String?>()
        val listener = DrawerNavigationController.Listener { snapshot, _ ->
            selected += snapshot.selectedPageId
        }
        controller.addListener(listener)
        controller.submitPages(listOf(page("a"), page("b")))
        controller.selectPage("b")
        controller.removeListener(listener)
        assertEquals(listOf(null, "a", "b"), selected)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDuplicatePageIds() {
        DrawerNavigationController().submitPages(listOf(page("same"), page("same")))
    }
}
