package com.zerostudio.idea.widget.editor.symbolinput.v2

import android.view.View
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerPage
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerPageHost
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.panel.DrawerPanelContent
import com.zerostudio.idea.widget.editor.symbolinput.v2.testing.DrawerTestHostActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DrawerPageHostLifecycleTest {

    private class TrackedContent(
        private val activity: DrawerTestHostActivity,
        private val label: String,
        private val events: MutableList<String>
    ) : DrawerPanelContent {
        override fun createView(): View = TextView(activity).apply { text = label }
        override fun onPanelAttached() { events += "$label:attached" }
        override fun onPanelVisible() { events += "$label:visible" }
        override fun onPanelHidden() { events += "$label:hidden" }
        override fun onPanelDetached() { events += "$label:detached" }
    }

    @Test
    fun selectionReplacementAndDisposeDispatchBalancedLifecycleCallbacks() {
        ActivityScenario.launch(DrawerTestHostActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val events = mutableListOf<String>()
                val host = DrawerPageHost(activity)
                activity.content.addView(host)
                val first = TrackedContent(activity, "first", events)
                val oldSecond = TrackedContent(activity, "old-second", events)
                val newSecond = TrackedContent(activity, "new-second", events)

                host.setPages(
                    listOf(
                        DrawerPage("first", "First", first),
                        DrawerPage("second", "Second", oldSecond)
                    )
                )
                host.setPanelVisible(true)
                assertTrue(host.selectPage("second"))

                host.setPages(
                    listOf(
                        DrawerPage("first", "First", first),
                        DrawerPage("second", "Second updated", newSecond)
                    )
                )
                assertEquals("second", host.currentPage()?.id)
                assertTrue(events.contains("old-second:hidden"))
                assertTrue(events.contains("old-second:detached"))
                assertTrue(events.contains("new-second:attached"))
                assertTrue(events.contains("new-second:visible"))

                host.dispose()
                host.dispose()
                assertEquals(1, events.count { it == "new-second:hidden" })
                assertEquals(1, events.count { it == "new-second:detached" })
                try {
                    host.selectPage("first")
                    throw AssertionError("Expected disposed host to reject selection")
                } catch (expected: IllegalStateException) {
                    assertTrue(expected.message.orEmpty().contains("disposed"))
                }
            }
        }
    }

    @Test
    fun rejectedDuplicatePageUpdateDoesNotEvictCurrentPage() {
        ActivityScenario.launch(DrawerTestHostActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val events = mutableListOf<String>()
                val host = DrawerPageHost(activity)
                activity.content.addView(host)
                val current = TrackedContent(activity, "current", events)
                host.setPages(listOf(DrawerPage("current", "Current", current)))
                host.setPanelVisible(true)

                try {
                    host.setPages(
                        listOf(
                            DrawerPage("duplicate", "One", current),
                            DrawerPage("duplicate", "Two", current)
                        )
                    )
                    throw AssertionError("Expected duplicate page IDs to be rejected")
                } catch (expected: IllegalArgumentException) {
                    assertTrue(expected.message.orEmpty().contains("unique"))
                }

                assertEquals("current", host.currentPage()?.id)
                assertEquals(1, events.count { it == "current:attached" })
                assertEquals(1, events.count { it == "current:visible" })
                assertFalse(events.contains("current:hidden"))
                host.dispose()
            }
        }
    }

    @Test
    fun disposedHostRejectsFurtherPageSelection() {
        ActivityScenario.launch(DrawerTestHostActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val host = DrawerPageHost(activity)
                activity.content.addView(host)
                host.dispose()
                try {
                    host.selectPage("anything")
                    throw AssertionError("Expected disposed host to reject selection")
                } catch (expected: IllegalStateException) {
                    assertTrue(expected.message.orEmpty().contains("disposed"))
                }
            }
        }
    }
}
