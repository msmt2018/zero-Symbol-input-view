package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation

import android.view.View
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.panel.DrawerPanelContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Directly exercises every public state/query/listener method on the navigation controller. */
class DrawerNavigationControllerMethodExecutionTest {
    private fun page(id: String) = DrawerPage(
        id = id,
        title = "Page $id",
        content = object : DrawerPanelContent {
            override fun createView(): View = error("Navigation must not create page views")
        }
    )

    @Test
    fun publicQueriesAndSelectionOverloadsReturnConsistentSnapshots() {
        val controller = DrawerNavigationController()
        assertTrue(controller.pages().isEmpty())
        assertNull(controller.selectedPage())
        assertEquals(-1, controller.selectedIndex())
        assertEquals(DrawerNavigationSnapshot(null, emptyList()), controller.snapshot())

        controller.submitPages(listOf(page("one"), page("two"), page("three")))
        assertEquals(listOf("one", "two", "three"), controller.pages().map { it.id })
        assertEquals(DrawerNavigationSnapshot("one", listOf("one", "two", "three")), controller.snapshot())
        assertEquals("one", controller.selectedPage()?.id)
        assertEquals(0, controller.selectedIndex())

        assertTrue(controller.selectPage(2))
        assertEquals("three", controller.selectedPage()?.id)
        assertEquals(2, controller.selectedIndex())
        assertTrue(controller.selectPage("two"))
        assertEquals("two", controller.snapshot().selectedPageId)
        assertEquals(1, controller.selectedIndex())

        assertTrue(controller.selectPage("two"))
        assertFalse(controller.selectPage(-1))
        assertFalse(controller.selectPage(3))
        assertFalse(controller.selectPage("missing"))
        assertEquals("two", controller.selectedPage()?.id)
    }

    @Test
    fun selectionCanBePreservedOrResetWhenPagesAreReplaced() {
        val controller = DrawerNavigationController()
        controller.submitPages(listOf(page("one"), page("two")))
        controller.selectPage("two")

        controller.submitPages(listOf(page("two"), page("three")), preserveSelection = true)
        assertEquals("two", controller.selectedPage()?.id)

        controller.submitPages(listOf(page("three"), page("four")), preserveSelection = false)
        assertEquals("three", controller.selectedPage()?.id)

        controller.submitPages(emptyList())
        assertNull(controller.selectedPage())
        assertEquals(-1, controller.selectedIndex())
        assertEquals(DrawerNavigationSnapshot(null, emptyList()), controller.snapshot())
    }

    @Test
    fun listenerReceivesImmediateAndSubsequentSnapshotsAndCanBeRemoved() {
        val controller = DrawerNavigationController()
        val events = mutableListOf<Pair<String?, Int>>()
        val listener = DrawerNavigationController.Listener { snapshot, index ->
            events += snapshot.selectedPageId to index
        }

        controller.addListener(listener)
        assertEquals(listOf(null to -1), events)

        controller.submitPages(listOf(page("one"), page("two")))
        controller.selectPage(1)
        controller.removeListener(listener)
        controller.selectPage(0)
        assertEquals(listOf(null to -1, "one" to 0, "two" to 1), events)
    }

    @Test
    fun duplicatePageSubmissionIsRejectedWithoutReplacingExistingState() {
        val controller = DrawerNavigationController()
        controller.submitPages(listOf(page("stable"), page("other")))
        controller.selectPage("other")
        val before = controller.snapshot()

        assertTrue(runCatching {
            controller.submitPages(listOf(page("duplicate"), page("duplicate")))
        }.isFailure)

        assertEquals(before, controller.snapshot())
        assertEquals(listOf("stable", "other"), controller.pages().map { it.id })
    }
}
