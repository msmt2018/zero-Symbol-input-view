package com.zerostudio.idea.widget.editor.symbolinput.v2

import android.os.Parcel
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.core.view.ViewCompat
import com.google.android.material.tabs.TabLayout
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.DrawerContainer
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.config.DrawerConfig
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.config.DrawerPresentationMode
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core.DrawerAnchorStrategies
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerNavigationConfig
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerPage
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.panel.DrawerPanelContent
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerColors
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerDimensions
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerMotion
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerShape
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerStyle
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerThemeMode
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerTypography
import com.zerostudio.idea.widget.editor.symbolinput.v2.testing.DrawerTestHostActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the public DrawerController surface through a real Android View host.
 * This is intentionally an API contract test rather than a screenshot test.
 */
@RunWith(AndroidJUnit4::class)
class DrawerControllerContractTest {

    @Test
    fun everyControllerOperationCanBeAppliedToAnAttachedDrawer() {
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

                val panel = object : DrawerPanelContent {
                    override fun createView(): View = TextView(activity).apply { text = "Panel" }
                }
                val controller = drawer.controller
                val pages = listOf(
                    DrawerPage("first", "First", panel),
                    DrawerPage("second", "Second", panel)
                )

                controller.setPeekContent(panel)
                controller.refreshAnchors()
                val minimumPeekHeight = requireNotNull(controller.currentState()).anchors.first().heightPx
                assertTrue(minimumPeekHeight >= (120f * activity.resources.displayMetrics.density).toInt())
                controller.setPeekContent(null)
                controller.setPeekContent(panel)
                controller.setExpandedContent(panel)
                controller.setExpandedContent(null)
                controller.setPages(pages, preserveSelection = false)
                assertTrue(controller.selectPage("second"))
                val tabs = findView(drawer) { it is TabLayout } as TabLayout
                assertEquals(1, tabs.selectedTabPosition)
                controller.setTitle("Controller contract")

                val hostContent = TextView(activity)
                drawer.setMainContent(hostContent)
                assertFalse(
                    drawer.onStartNestedScroll(
                        hostContent, hostContent, ViewCompat.SCROLL_AXIS_VERTICAL
                    )
                )

                controller.setAnchorStrategy(DrawerAnchorStrategies.threeStage())
                controller.setPresentationMode(DrawerPresentationMode.FULL_SCREEN)
                controller.setNavigationConfig(DrawerNavigationConfig())
                controller.setStyle(DrawerStyle())
                controller.setThemeMode(DrawerThemeMode.DARK)
                controller.setColors(DrawerColors(surface = 0xff202124.toInt()))
                controller.setDimensions(DrawerDimensions())
                controller.setTypography(DrawerTypography())
                controller.setMotion(DrawerMotion())
                controller.setShape(DrawerShape())

                controller.setDragEnabled(false)
                controller.setDragEnabled(true)
                controller.setNestedScrollEnabled(false)
                controller.setNestedScrollEnabled(true)
                controller.setEdgeToEdgeEnabled(true)
                controller.setEdgeToEdgeEnabled(false)
                controller.setDismissOnScrimTap(false)
                controller.setDismissOnScrimTap(true)
                controller.setDismissOnBack(false)
                controller.setDismissOnBack(true)
                controller.setDragHandleEnabled(false)
                controller.setDragHandleEnabled(true)
                controller.setAccessibilityEnabled(false)
                controller.setDragEnabled(false)
                controller.applyConfig(DrawerConfig.builder().build())
                val sheet = drawer.getChildAt(drawer.childCount - 1) as android.widget.LinearLayout
                val handleTarget = sheet.getChildAt(0)
                assertTrue(handleTarget.isEnabled)
                assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO, sheet.importantForAccessibility)

                val observedStates = mutableListOf<String>()
                val listener: (com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core.DrawerState) -> Unit = {
                    observedStates += it.currentAnchorId
                }
                controller.addStateListener(listener)
                controller.refreshAnchors()
                controller.moveToAnchor("peek", animated = false)
                controller.expand(animated = false)
                controller.collapse(animated = false)
                controller.removeStateListener(listener)

                controller.applyConfig(
                    DrawerConfig.builder()
                        .setAnchorStrategy(DrawerAnchorStrategies.existingHeight())
                        .setPresentationMode(DrawerPresentationMode.BOTTOM_SHEET)
                        .build()
                )
                assertNotNull(controller.currentState())
            }
        }
    }

    @Test
    fun expansionRequestedBeforeFirstLayoutIsAppliedAfterAnchorsResolve() {
        ActivityScenario.launch(DrawerTestHostActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val drawer = DrawerContainer(activity)
                drawer.controller.expand(animated = false)
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
                assertEquals(state.anchors.last().id, state.currentAnchorId)
            }
        }
    }

    @Test
    fun nestedScrollYieldsToScrollableContentBeforeCollapsingAndExpandsBeforeScrollingDown() {
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
                drawer.controller.setAnchorStrategy(DrawerAnchorStrategies.threeStage(0.5f, false))
                val scrollPanel = object : DrawerPanelContent {
                    override fun createView(): View = ScrollView(activity).apply {
                        addView(TextView(activity).apply {
                            text = (1..120).joinToString("\n") { "Scrollable row $it" }
                            minHeight = (1_600f * activity.resources.displayMetrics.density).toInt()
                        }, ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ))
                    }
                }
                drawer.controller.setExpandedContent(scrollPanel)
                drawer.measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                )
                drawer.layout(0, 0, width, height)
                drawer.refreshAnchors()
                drawer.controller.moveToAnchor("half", animated = false)
                drawer.measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                )
                drawer.layout(0, 0, width, height)

                val sheet = drawer.getChildAt(drawer.childCount - 1)
                val scroll = requireNotNull(findView(drawer) { it is ScrollView }) as ScrollView
                scroll.scrollTo(0, (120f * activity.resources.displayMetrics.density).toInt())
                assertTrue(scroll.canScrollVertically(-1))
                assertTrue(drawer.onStartNestedScroll(
                    sheet, scroll, ViewCompat.SCROLL_AXIS_VERTICAL
                ))
                assertFalse(drawer.onStartNestedScroll(
                    activity.content, scroll, ViewCompat.SCROLL_AXIS_VERTICAL
                ))

                val beforeChildTop = requireNotNull(drawer.controller.currentState()).currentHeightPx
                val childFirst = IntArray(2)
                drawer.onNestedPreScroll(scroll, 0, -24, childFirst, ViewCompat.TYPE_TOUCH)
                assertEquals(0, childFirst[1])
                assertEquals(beforeChildTop, requireNotNull(drawer.controller.currentState()).currentHeightPx)

                scroll.scrollTo(0, 0)
                val collapse = IntArray(2)
                drawer.onNestedPreScroll(scroll, 0, -24, collapse, ViewCompat.TYPE_TOUCH)
                val afterCollapse = requireNotNull(drawer.controller.currentState()).currentHeightPx
                assertTrue(collapse[1] < 0)
                assertTrue(afterCollapse < beforeChildTop)

                val expand = IntArray(2)
                drawer.onNestedPreScroll(scroll, 0, 24, expand, ViewCompat.TYPE_TOUCH)
                assertTrue(expand[1] > 0)
                assertTrue(requireNotNull(drawer.controller.currentState()).currentHeightPx > afterCollapse)
            }
        }
    }

    @Test
    fun savedStateRestoresAnchorAndSelectedPageWhenPagesArriveAfterRestore() {
        ActivityScenario.launch(DrawerTestHostActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val width = activity.resources.displayMetrics.widthPixels
                val height = activity.resources.displayMetrics.heightPixels
                val panel = object : DrawerPanelContent {
                    override fun createView(): View = TextView(activity).apply { text = "Restored page" }
                }
                val pages = listOf(
                    DrawerPage("first", "First", panel),
                    DrawerPage("second", "Second", panel)
                )
                fun attachAndLayout(drawer: DrawerContainer) {
                    activity.content.addView(
                        drawer,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                    drawer.measure(
                        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                    )
                    drawer.layout(0, 0, width, height)
                }

                val original = DrawerContainer(activity)
                original.controller.setAnchorStrategy(DrawerAnchorStrategies.threeStage(0.5f, false))
                original.controller.setNavigationConfig(DrawerNavigationConfig(enabled = false))
                attachAndLayout(original)
                original.controller.setPages(pages, preserveSelection = false)
                assertTrue(original.controller.selectPage("second"))
                original.controller.moveToAnchor("half", animated = false)
                val parcel = Parcel.obtain()
                val saved = try {
                    original.onSaveInstanceState().writeToParcel(parcel, 0)
                    parcel.setDataPosition(0)
                    DrawerContainer.SavedState.CREATOR.createFromParcel(parcel)
                } finally {
                    parcel.recycle()
                }

                activity.content.removeView(original)
                val restored = DrawerContainer(activity)
                restored.controller.setAnchorStrategy(DrawerAnchorStrategies.threeStage(0.5f, false))
                restored.controller.setNavigationConfig(DrawerNavigationConfig(enabled = false))
                restored.onRestoreInstanceState(saved)
                attachAndLayout(restored)
                restored.controller.setPages(pages, preserveSelection = false)

                assertEquals("second", restored.navigationHost.currentPage()?.id)
                assertEquals("half", restored.controller.currentState()?.currentAnchorId)
            }
        }
    }

    private fun findView(root: View, predicate: (View) -> Boolean): View? {
        if (predicate(root)) return root
        if (root is ViewGroup) {
            for (index in 0 until root.childCount) {
                findView(root.getChildAt(index), predicate)?.let { return it }
            }
        }
        return null
    }


}
