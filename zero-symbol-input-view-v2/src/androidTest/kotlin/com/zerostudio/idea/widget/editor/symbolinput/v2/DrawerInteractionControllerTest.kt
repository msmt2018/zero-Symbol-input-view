package com.zerostudio.idea.widget.editor.symbolinput.v2

import android.view.MotionEvent
import androidx.test.core.app.ActivityScenario
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core.DrawerInteractionController
import com.zerostudio.idea.widget.editor.symbolinput.v2.testing.DrawerTestHostActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerInteractionControllerTest {
    @Test fun symbolDrawerUsesPersistedCollapsedRowSettingAndRetainsGroupPaging() {
        ActivityScenario.launch(DrawerTestHostActivity::class.java).use { scenario ->
            lateinit var drawer: SymbolDrawerView
            val groupsCommitted = java.util.concurrent.CountDownLatch(1)
            scenario.onActivity { activity ->
                drawer = SymbolDrawerView(activity)
                val configuredRows = 4
                val settings = com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings(
                    columns = 5,
                    collapsedRows = configuredRows
                )
                drawer.applySettings(settings)
                val width = (360f * activity.resources.displayMetrics.density).toInt()
                drawer.measure(
                    android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
                    android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
                )
                val expectedHeight = com.zerostudio.idea.widget.editor.symbolinput.v2.state.DrawerGeometry
                    .collapsedHeightPx(settings.toDrawerConfig(), activity.resources.displayMetrics.density)
                assertEquals(expectedHeight, drawer.measuredHeight)

                val groups = listOf(
                    com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup("first", "First", emptyList()),
                    com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup("second", "Second", emptyList()),
                    com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup("third", "Third", emptyList())
                )
                drawer.submitGroups(groups) { groupsCommitted.countDown() }
            }
            assertTrue("Symbol groups did not finish committing", groupsCommitted.await(5, java.util.concurrent.TimeUnit.SECONDS))
            scenario.onActivity {
                drawer.selectGroup(1, false)
                assertEquals(1, drawer.getCurrentGroupIndex())
                drawer.selectGroup(2, false)
                assertEquals(2, drawer.getCurrentGroupIndex())
            }
        }
    }

    @Test fun continuousDragUsesFeatureSuppliedBoundsAndReportsIntermediateHeights() {
        ActivityScenario.launch(DrawerTestHostActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val heights = mutableListOf<Int>()
                val controller = DrawerInteractionController(
                    context = activity,
                    onHeightChanged = { height, _, _ -> heights += height }
                )
                controller.setBounds(100, 300, 100, animationDurationMs = 0L)
                controller.onAttached()

                val down = event(MotionEvent.ACTION_DOWN, 100f, 300f, 10L)
                val move = event(MotionEvent.ACTION_MOVE, 100f, 250f, 20L)
                try {
                    assertFalse(controller.onInterceptTouchEvent(down) { _, _ -> false })
                    assertTrue(controller.onInterceptTouchEvent(move) { _, _ -> false })
                    assertTrue(controller.onTouchEvent(move))
                    assertEquals(150, controller.currentHeightPx)
                    assertTrue(heights.contains(150))

                    val cancel = event(MotionEvent.ACTION_CANCEL, 100f, 250f, 30L)
                    try {
                        assertTrue(controller.onTouchEvent(cancel))
                        assertEquals(100, controller.currentHeightPx)
                        assertFalse(controller.isDragging)
                    } finally {
                        cancel.recycle()
                    }
                } finally {
                    down.recycle()
                    move.recycle()
                    controller.onDetached()
                }
            }
        }
    }

    @Test fun horizontalPagerGestureRemainsLockedToTheChild() {
        ActivityScenario.launch(DrawerTestHostActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val controller = DrawerInteractionController(activity, onHeightChanged = { _, _, _ -> })
                controller.setBounds(100, 300, 100)
                val down = event(MotionEvent.ACTION_DOWN, 10f, 100f, 10L)
                val horizontal = event(MotionEvent.ACTION_MOVE, 70f, 102f, 20L)
                val laterVertical = event(MotionEvent.ACTION_MOVE, 70f, 40f, 30L)
                try {
                    assertFalse(controller.onInterceptTouchEvent(down) { _, _ -> false })
                    assertFalse(controller.onInterceptTouchEvent(horizontal) { _, _ -> false })
                    assertFalse(controller.onInterceptTouchEvent(laterVertical) { _, _ -> false })
                } finally {
                    down.recycle()
                    horizontal.recycle()
                    laterVertical.recycle()
                    controller.onDetached()
                }
            }
        }
    }

    @Test fun downwardGestureDefersToScrollableChildUntilItReachesTop() {
        ActivityScenario.launch(DrawerTestHostActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val controller = DrawerInteractionController(activity, onHeightChanged = { _, _, _ -> })
                controller.setBounds(100, 300, 250)
                val down = event(MotionEvent.ACTION_DOWN, 100f, 100f, 10L)
                val move = event(MotionEvent.ACTION_MOVE, 100f, 160f, 20L)
                try {
                    assertFalse(controller.onInterceptTouchEvent(down) { _, _ -> true })
                    assertFalse(controller.onInterceptTouchEvent(move) { _, _ -> true })
                    assertFalse(controller.isDragging)
                } finally {
                    down.recycle()
                    move.recycle()
                    controller.onDetached()
                }
            }
        }
    }

    @Test fun downwardGestureTransfersToDrawerWhenScrollableChildReachesTop() {
        ActivityScenario.launch(DrawerTestHostActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val controller = DrawerInteractionController(activity, onHeightChanged = { _, _, _ -> })
                controller.setBounds(100, 300, 250)
                controller.onAttached()
                val down = event(MotionEvent.ACTION_DOWN, 100f, 100f, 10L)
                val childScroll = event(MotionEvent.ACTION_MOVE, 100f, 130f, 20L)
                val reachesTop = event(MotionEvent.ACTION_MOVE, 100f, 160f, 30L)
                val continues = event(MotionEvent.ACTION_MOVE, 100f, 180f, 40L)
                try {
                    assertFalse(controller.onInterceptTouchEvent(down) { _, _ -> false })
                    assertFalse(controller.onInterceptTouchEvent(childScroll) { _, _ -> true })
                    assertTrue(controller.onInterceptTouchEvent(reachesTop) { _, _ -> false })
                    assertTrue(controller.onTouchEvent(reachesTop))
                    assertEquals("Only movement after the child reached its top transfers to the drawer",
                        220, controller.currentHeightPx)
                    assertTrue(controller.onInterceptTouchEvent(continues) { _, _ -> false })
                    assertTrue(controller.onTouchEvent(continues))
                    assertEquals(200, controller.currentHeightPx)
                } finally {
                    down.recycle()
                    childScroll.recycle()
                    reachesTop.recycle()
                    continues.recycle()
                    controller.onDetached()
                }
            }
        }
    }

    private fun event(action: Int, x: Float, y: Float, time: Long): MotionEvent =
        MotionEvent.obtain(0L, time, action, x, y, 0)
}
