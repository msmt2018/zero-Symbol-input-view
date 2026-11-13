package com.zerostudio.idea.widget.editor.symbolinput.v2

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ImageButton
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.DrawerContainer
import com.zerostudio.idea.widget.editor.symbolinput.v2.testing.DrawerTestHostActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class DrawerHandleInteractionTest {

    @Test
    fun handleHitTargetTracksTheVisibleBottomSheetEdgeAndAcceptsVerticalDrag() {
        ActivityScenario.launch(DrawerTestHostActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val drawer = DrawerContainer(activity)
                activity.content.addView(
                    drawer,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                val width = activity.resources.displayMetrics.widthPixels
                val height = activity.resources.displayMetrics.heightPixels
                drawer.measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                )
                drawer.layout(0, 0, width, height)

                val state = requireNotNull(drawer.controller.currentState())
                val sheet = drawer.getChildAt(drawer.childCount - 1) as LinearLayout
                val handleTarget = sheet.getChildAt(0)
                val visibleSheetTop = sheet.top + sheet.translationY
                assertEquals(
                    drawer.height - state.currentHeightPx,
                    visibleSheetTop.roundToInt()
                )
                assertTrue(handleTarget.isShown)
                assertTrue(handleTarget.isEnabled)
                assertTrue(handleTarget.height >= (48f * activity.resources.displayMetrics.density).toInt())
                assertTrue(
                    sheet.top + sheet.translationY + handleTarget.bottom <= drawer.height
                )

                val startHeight = state.currentHeightPx
                val downTime = SystemClock.uptimeMillis()
                fun send(action: Int, y: Float, timeOffset: Long) {
                    val event = MotionEvent.obtain(
                        downTime,
                        downTime + timeOffset,
                        action,
                        handleTarget.width / 2f,
                        y,
                        0
                    )
                    try {
                        handleTarget.dispatchTouchEvent(event)
                    } finally {
                        event.recycle()
                    }
                }
                send(MotionEvent.ACTION_DOWN, 24f, 0)
                send(MotionEvent.ACTION_MOVE, -96f, 80)
                val draggedHeight = requireNotNull(drawer.controller.currentState()).currentHeightPx
                assertTrue("Dragging the visible handle upward must increase drawer height", draggedHeight > startHeight)
                send(MotionEvent.ACTION_CANCEL, -96f, 100)
            }
        }
    }

    @Test
    fun settingsEntryIsPinnedAtTheEndOfTheGroupIndicatorAndRemainsClickable() {
        ActivityScenario.launch(DrawerTestHostActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val drawer = SymbolDrawerView(activity)
                var settingsOpened = 0
                drawer.onOpenSettingsListener = { settingsOpened++ }
                activity.content.addView(
                    drawer,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        (220f * activity.resources.displayMetrics.density).toInt()
                    )
                )
                fun layoutAt(widthDp: Int) {
                    val width = (widthDp * activity.resources.displayMetrics.density).toInt()
                    val height = (220f * activity.resources.displayMetrics.density).toInt()
                    drawer.measure(
                        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                    )
                    drawer.layout(0, 0, width, height)
                }
                fun findSettingsButton(view: View): ImageButton? {
                    if (view is ImageButton && view.contentDescription == "符号设置") return view
                    if (view is ViewGroup) {
                        for (index in 0 until view.childCount) {
                            findSettingsButton(view.getChildAt(index))?.let { return it }
                        }
                    }
                    return null
                }
                for (widthDp in listOf(400, 900)) {
                    layoutAt(widthDp)
                    val button = requireNotNull(findSettingsButton(drawer))
                    assertTrue(button.isClickable)
                    assertTrue(button.minimumWidth >= (48f * activity.resources.displayMetrics.density).toInt())
                    assertTrue(button.minimumHeight >= (48f * activity.resources.displayMetrics.density).toInt())
                    assertEquals(button.parent.let { (it as View).width }, button.right)
                    button.performClick()
                }
                assertEquals(2, settingsOpened)
            }
        }
    }
}
