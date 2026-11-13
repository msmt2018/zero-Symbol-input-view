package com.zerostudio.idea.widget.editor.symbolinput.v2

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.manager.SymbolDrawerManagerScreen
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

    @Test fun drawerCanDetachAndReattachWithoutLosingStableUiState() {
        ActivityScenario.launch(DrawerTestHostActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val drawer = SymbolDrawerView(activity)
                activity.content.addView(drawer, FrameLayout.LayoutParams(-1, 480))
                drawer.submitGroups(sample())
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
                settings.save(SymbolDrawerSettings(cellHorizontalSpacingDp = -10, cellVerticalSpacingDp = -8))
                screen.reload()
                assertEquals(-10, screen.getSettings().cellHorizontalSpacingDp)
                assertEquals(-8, screen.getSettings().cellVerticalSpacingDp)
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
