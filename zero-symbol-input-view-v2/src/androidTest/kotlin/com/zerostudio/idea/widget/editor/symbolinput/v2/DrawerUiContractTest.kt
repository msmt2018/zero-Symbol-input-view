package com.zerostudio.idea.widget.editor.symbolinput.v2

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.manager.SymbolDrawerManagerScreen
import com.zerostudio.idea.widget.editor.symbolinput.v2.manager.CappedScrollView
import com.zerostudio.idea.widget.editor.symbolinput.v2.manager.resolveEditorFormMaxHeightPx
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SettingsLoadResult
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettingsRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettingsScreen
import com.zerostudio.idea.widget.editor.symbolinput.v2.testing.DrawerTestHostActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DrawerUiContractTest {
    private class Groups(initial: List<SymbolGroup>) : SymbolGroupRepository {
        var value = initial
        override fun loadGroups() = value
        override fun saveGroups(groups: List<SymbolGroup>) { value = groups.toList() }
    }

    private class Settings : SymbolDrawerSettingsRepository {
        var value = SymbolDrawerSettings()
        override fun load() = SettingsLoadResult(value)
        override fun save(settings: SymbolDrawerSettings) { value = settings }
        override fun reset() { value = SymbolDrawerSettings() }
        override fun hasStoredSettings() = true
    }

    private fun sample() = listOf(
        SymbolGroup("one", "One", listOf(SymbolEntry(1L, "a", SymbolAction.InsertText("a")))),
        SymbolGroup("two", "Two", listOf(SymbolEntry(2L, "b", SymbolAction.InsertText("b"))))
    )

    @Test fun advancedDrawerInflatesAndAcceptsNegativeSpacing() {
        ActivityScenario.launch(DrawerTestHostActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val drawer = SymbolDrawerView(activity)
                activity.content.addView(drawer, FrameLayout.LayoutParams(-1, 480))
                val value = SymbolDrawerSettings(
                    cellHorizontalSpacingDp = -10,
                    cellVerticalSpacingDp = -10,
                    cellHeightDp = 10,
                    maxExpandedHeightDp = 110
                )
                drawer.applySettings(value)
                drawer.submitGroups(sample())
                assertEquals(-10, drawer.getSettings().cellHorizontalSpacingDp)
                assertEquals(-10, drawer.getSettings().cellVerticalSpacingDp)
            }
        }
    }


    @Test fun settingsEntryStaysPinnedBesideScrollableGroupTabs() {
        ActivityScenario.launch(DrawerTestHostActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val drawer = SymbolDrawerView(activity)
                var opened = 0
                drawer.onOpenSettingsListener = { opened++ }
                activity.content.addView(drawer, FrameLayout.LayoutParams(-1, 480))
                drawer.submitGroups(sample())
                drawer.setExpanded(true, false)

                val width = (420f * activity.resources.displayMetrics.density).toInt()
                val height = (480f * activity.resources.displayMetrics.density).toInt()
                drawer.measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                )
                drawer.layout(0, 0, width, height)

                val settings = find(drawer) { it.contentDescription == "符号设置" }
                assertEquals(width, (settings.parent as View).right)
                assertTrue(settings.performClick())
                assertEquals(1, opened)
            }
        }
    }

    @Test fun drawerCanDetachAndReattachWithoutLosingStableUiState() {
        ActivityScenario.launch(DrawerTestHostActivity::class.java).use { scenario ->
            lateinit var drawer: SymbolDrawerView
            val committed = CountDownLatch(1)
            scenario.onActivity { activity ->
                drawer = SymbolDrawerView(activity)
                activity.content.addView(drawer, FrameLayout.LayoutParams(-1, 480))
                drawer.submitGroups(sample()) { committed.countDown() }
            }
            assertTrue("Group submission did not commit", committed.await(5, TimeUnit.SECONDS))
            scenario.onActivity { activity ->
                drawer.selectGroup(1, false)
                drawer.setExpanded(true, false)

                activity.content.removeView(drawer)
                activity.content.addView(drawer, FrameLayout.LayoutParams(-1, 480))

                assertEquals(1, drawer.getCurrentGroupIndex())
                assertTrue(drawer.isExpanded())
            }
        }
    }

    @Test fun managerScreenInflatesTabsAndBackupImportRoundTrips() {
        ActivityScenario.launch(DrawerTestHostActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val repository = Groups(sample())
                val screen = SymbolDrawerManagerScreen(activity, repository)
                activity.content.addView(screen, FrameLayout.LayoutParams(-1, -1))
                val tabs = find(screen) { it is TabLayout } as TabLayout
                assertEquals(2, tabs.tabCount)
                val backup = screen.exportBundle()
                screen.importBundle(backup)
                assertEquals(sample(), repository.value)
            }
        }
    }

    @Test fun managerSymbolCardsReflowAcrossWindowSizeClasses() {
        ActivityScenario.launch(DrawerTestHostActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val screen = SymbolDrawerManagerScreen(activity, Groups(sample()))
                activity.content.addView(screen, FrameLayout.LayoutParams(-1, -1))
                val density = activity.resources.displayMetrics.density
                val wideWidth = (1000f * density).toInt()
                val height = (800f * density).toInt()
                screen.measure(
                    View.MeasureSpec.makeMeasureSpec(wideWidth, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                )
                screen.layout(0, 0, wideWidth, height)
                val list = find(screen) { it is RecyclerView } as RecyclerView
                assertEquals(2, (list.layoutManager as GridLayoutManager).spanCount)

                val compactWidth = (500f * density).toInt()
                screen.measure(
                    View.MeasureSpec.makeMeasureSpec(compactWidth, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                )
                screen.layout(0, 0, compactWidth, height)
                assertEquals(1, (list.layoutManager as GridLayoutManager).spanCount)

                // A landscape/split-height change must not alter the column
                // decision: the policy is based on the app window width.
                val landscapeHeight = (360f * density).toInt()
                screen.measure(
                    View.MeasureSpec.makeMeasureSpec(compactWidth, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(landscapeHeight, View.MeasureSpec.EXACTLY)
                )
                screen.layout(0, 0, compactWidth, landscapeHeight)
                assertEquals(1, (list.layoutManager as GridLayoutManager).spanCount)
            }
        }
    }

    @Test fun settingsScreenInflatesAllSectionsAndReloadsCompactValues() {
        ActivityScenario.launch(DrawerTestHostActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val settings = Settings()
                val screen = SymbolDrawerSettingsScreen(activity, settings, Groups(sample()))
                activity.content.addView(screen, FrameLayout.LayoutParams(-1, -1))
                val tabs = find(screen) { it is TabLayout } as TabLayout
                assertTrue(tabs.tabCount >= 5)
                tabs.getTabAt(2)?.select()
                val density = activity.resources.displayMetrics.density
                val expandedWidth = (1000f * density).toInt()
                val expandedHeight = (800f * density).toInt()
                screen.measure(
                    View.MeasureSpec.makeMeasureSpec(expandedWidth, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(expandedHeight, View.MeasureSpec.EXACTLY)
                )
                screen.layout(0, 0, expandedWidth, expandedHeight)
                val reflowedTabs = find(screen) { it is TabLayout } as TabLayout
                assertEquals(2, reflowedTabs.selectedTabPosition)

                val compactWidth = (500f * density).toInt()
                screen.measure(
                    View.MeasureSpec.makeMeasureSpec(compactWidth, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(expandedHeight, View.MeasureSpec.EXACTLY)
                )
                screen.layout(0, 0, compactWidth, expandedHeight)
                val compactTabs = find(screen) { it is TabLayout } as TabLayout
                assertEquals(2, compactTabs.selectedTabPosition)

                settings.save(SymbolDrawerSettings(cellHorizontalSpacingDp = -10, cellVerticalSpacingDp = -8))
                screen.reload()
                assertEquals(-10, screen.getSettings().cellHorizontalSpacingDp)
                assertEquals(-8, screen.getSettings().cellVerticalSpacingDp)
            }
        }
    }



    @Test fun settingsTypographyReflowsWithWindowSizeClass() {
        ActivityScenario.launch(DrawerTestHostActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val screen = SymbolDrawerSettingsScreen(activity, Settings(), Groups(sample()))
                val density = activity.resources.displayMetrics.density
                screen.layout(0, 0, (500f * density).toInt(), (800f * density).toInt())
                val compactTitle = find(screen) {
                    it is TextView && it.text == "符号抽屉设置"
                } as TextView
                val compactTextSize = compactTitle.textSize

                screen.layout(0, 0, (1000f * density).toInt(), (800f * density).toInt())
                val expandedTitle = find(screen) {
                    it is TextView && it.text == "符号抽屉设置"
                } as TextView
                assertTrue(expandedTitle.textSize > compactTextSize)
            }
        }
    }

    @Test fun symbolEditorFormScrollViewIsCappedToAvailableWindowHeight() {
        ActivityScenario.launch(DrawerTestHostActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val cap = (420f * activity.resources.displayMetrics.density).toInt()
                val scroll = CappedScrollView(activity, cap)
                val content = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    repeat(30) {
                        addView(View(activity), LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            (48f * activity.resources.displayMetrics.density).toInt()
                        ))
                    }
                }
                scroll.addView(content)
                scroll.measure(
                    View.MeasureSpec.makeMeasureSpec(
                        (600f * activity.resources.displayMetrics.density).toInt(),
                        View.MeasureSpec.AT_MOST
                    ),
                    View.MeasureSpec.makeMeasureSpec(
                        (900f * activity.resources.displayMetrics.density).toInt(),
                        View.MeasureSpec.AT_MOST
                    )
                )
                assertTrue("Long edit forms must not exceed the available height cap", scroll.measuredHeight <= cap)
                assertTrue("Long edit forms must remain scrollable", scroll.getChildAt(0).measuredHeight > scroll.measuredHeight)
            }
        }
    }


    @Test fun editorFormHeightNeverUsesAnOversizedMinimumInTinyWindows() {
        ActivityScenario.launch(DrawerTestHostActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val density = activity.resources.displayMetrics.density
                val windowHeight = (60f * density).toInt()
                val reservedChrome = (96f * density).toInt()
                val cap = resolveEditorFormMaxHeightPx(windowHeight, reservedChrome)
                assertEquals(1, cap)
                assertTrue("The editor form cap must never exceed the available window height", cap <= windowHeight)
            }
        }
    }

    @Test fun cappedEditorFormAlsoHonorsATighterParentMeasureConstraint() {
        ActivityScenario.launch(DrawerTestHostActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val density = activity.resources.displayMetrics.density
                val cap = (500f * density).toInt()
                val parentLimit = (160f * density).toInt()
                val scroll = CappedScrollView(activity, cap)
                val content = TextView(activity).apply {
                    text = (1..80).joinToString("\\n") { "Scrollable editor field $it" }
                }
                scroll.addView(content, ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (900f * density).toInt()
                ))
                scroll.measure(
                    View.MeasureSpec.makeMeasureSpec(
                        (400f * density).toInt(),
                        View.MeasureSpec.AT_MOST
                    ),
                    View.MeasureSpec.makeMeasureSpec(parentLimit, View.MeasureSpec.AT_MOST)
                )
                scroll.layout(0, 0, scroll.measuredWidth, scroll.measuredHeight)
                assertTrue(
                    "The capped editor form must respect its tighter parent height",
                    scroll.measuredHeight <= parentLimit
                )
                assertTrue(
                    "The content must remain scrollable inside the capped form",
                    scroll.canScrollVertically(1)
                )
            }
        }
    }

    private fun find(root: View, predicate: (View) -> Boolean): View {
        if (predicate(root)) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val result = runCatching { find(root.getChildAt(i), predicate) }.getOrNull()
                if (result != null) return result
            }
        }
        error("Required view not found")
    }
}
