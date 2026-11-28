package com.zerostudio.idea.widget.editor.symbolinput.v2

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.AdaptiveWindowObserver
import com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.AdaptiveWindowController
import com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.WindowAdaptiveInfo
import com.zerostudio.idea.widget.drawer.adaptive.rememberAdaptiveWindowSnapshot
import com.zerostudio.idea.widget.drawer.DrawerContainer
import com.zerostudio.idea.widget.editor.symbolinput.v2.manager.SymbolDrawerManagerScreen
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.ObservableSymbolGroupRepository
import com.zerostudio.idea.widget.drawer.core.DrawerAnchorInput
import com.zerostudio.idea.widget.drawer.core.DrawerStateHolder
import com.zerostudio.idea.widget.drawer.core.ExistingHeightAnchorStrategy
import com.zerostudio.idea.widget.drawer.core.rememberDrawerStateSnapshot
import com.zerostudio.idea.widget.drawer.rememberDrawerContainerState
import com.zerostudio.idea.widget.drawer.navigation.DrawerPage
import com.zerostudio.idea.widget.drawer.navigation.DrawerPageHost
import com.zerostudio.idea.widget.drawer.navigation.DrawerNavigationController
import com.zerostudio.idea.widget.drawer.navigation.rememberDrawerNavigationSnapshot
import com.zerostudio.idea.widget.drawer.panel.DrawerPanelContent
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.GroupIndicatorStyle
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettingsScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.util.concurrent.CopyOnWriteArrayList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeMigrationContractTest {
    @get:Rule val compose = createComposeRule()

    private fun groups() = listOf(
        SymbolGroup(
            id = "first",
            title = "First",
            symbols = listOf(SymbolEntry(1L, "@", SymbolAction.InsertText("@")))
        ),
        SymbolGroup(
            id = "second",
            title = "Second",
            symbols = listOf(SymbolEntry(2L, "#", SymbolAction.InsertText("#")))
        )
    )

    @Test fun symbolDrawerRendersGroupsAndDispatchesComposeSymbolClick() {
        var clicks = 0
        compose.setContent {
            MaterialTheme {
                SymbolDrawer(
                    groups = groups(),
                    state = rememberSymbolDrawerState(initiallyExpanded = true),
                    settings = SymbolDrawerSettings(indicatorStyle = GroupIndicatorStyle.TEXT_TABS),
                    onSymbolClick = { clicks++ }
                )
            }
        }
        compose.onNodeWithText("First").assertIsDisplayed()
        compose.onNodeWithText("@").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test fun symbolDrawerDoesNotRenderRedundantManagerEntryPoint() {
        compose.setContent {
            MaterialTheme {
                SymbolDrawer(
                    groups = groups(),
                    state = rememberSymbolDrawerState(initiallyExpanded = true),
                    onSymbolClick = {},
                    onOpenManager = { error("Manager must not be exposed as a drawer header button") }
                )
            }
        }
        compose.onNodeWithText("@").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(0, compose.onAllNodesWithText("Symbol manager").fetchSemanticsNodes().size)
        }
    }

    @Test fun drawerContainerUsesCompositionOwnedExpansionState() {
        var state: com.zerostudio.idea.widget.drawer.DrawerContainerState? = null
        compose.setContent {
            val drawerState = rememberDrawerContainerState()
            state = drawerState
            DrawerContainer(
                state = drawerState,
                title = "Compose drawer",
                content = { Text("Editor content") }
            )
        }
        compose.onNodeWithText("Editor content").assertIsDisplayed()
        compose.runOnIdle {
            state!!.expand()
            assertTrue(state!!.expanded)
            state!!.collapse()
            assertTrue(!state!!.expanded)
        }
    }

    @Test fun pageHostComposesPageSlotsAndChangesPagesBySwipe() {
        var selected by mutableStateOf("first")
        val pages = listOf(
            DrawerPage("first", "First", DrawerPanelContent { Text("Page one") }),
            DrawerPage("second", "Second", DrawerPanelContent { Text("Page two") })
        )
        compose.setContent {
            DrawerPageHost(
                pages = pages,
                selectedPageId = selected,
                onPageSelected = { selected = it.id }
            )
        }
        compose.onNodeWithText("Page one").assertIsDisplayed()
        compose.onRoot().performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithText("Page two").assertIsDisplayed()
    }

    @Test fun adaptiveWindowControllerPublishesThroughCompositionAdapter() {
        val controller = AdaptiveWindowController(WindowAdaptiveInfo(widthDp = 500f, heightDp = 700f))
        compose.setContent {
            val snapshot = rememberAdaptiveWindowSnapshot(controller)
            Text("adaptive-controller-width-${snapshot.widthDp.toInt()}")
        }
        compose.onNodeWithText("adaptive-controller-width-500").assertIsDisplayed()
        compose.runOnIdle {
            controller.update(WindowAdaptiveInfo(widthDp = 900f, heightDp = 700f))
        }
        compose.onNodeWithText("adaptive-controller-width-900").assertIsDisplayed()
    }

    @Test fun navigationControllerPublishesThroughCompositionAdapter() {
        val controller = DrawerNavigationController()
        controller.submitPages(
            listOf(
                DrawerPage("first", "First", DrawerPanelContent { Text("Page one") }),
                DrawerPage("second", "Second", DrawerPanelContent { Text("Page two") })
            )
        )
        compose.setContent {
            val snapshot = rememberDrawerNavigationSnapshot(controller)
            Text("navigation-page-${snapshot.selectedPageId}")
        }
        compose.onNodeWithText("navigation-page-first").assertIsDisplayed()
        compose.runOnIdle { controller.selectPage("second") }
        compose.onNodeWithText("navigation-page-second").assertIsDisplayed()
    }

    @Test fun pureDrawerStateEnginePublishesThroughCompositionAdapter() {
        val holder = DrawerStateHolder(
            initialInput = DrawerAnchorInput(
                minHeightPx = 100,
                maxHeightPx = 300,
                viewportHeightPx = 300,
                contentHeightPx = 300,
                existingExpandedHeightPx = 300
            ),
            initialStrategy = ExistingHeightAnchorStrategy(),
            initialAnchorId = "peek"
        )
        compose.setContent {
            val snapshot = rememberDrawerStateSnapshot(holder)
            Text("engine-height-${snapshot.currentHeightPx}")
        }
        compose.onNodeWithText("engine-height-100").assertIsDisplayed()
        compose.runOnIdle { holder.snapToAnchor("expanded") }
        compose.onNodeWithText("engine-height-300").assertIsDisplayed()
    }

    @Test fun adaptiveObserverReportsComposeLayoutBounds() {
        compose.setContent {
            Box(Modifier.size(width = 320.dp, height = 400.dp)) {
                AdaptiveWindowObserver(Modifier.fillMaxSize()) { info ->
                    Text("window-width-" + info.widthDp.toInt())
                }
            }
        }
        compose.onNodeWithText("window-width-320").assertIsDisplayed()
    }

    @Test fun managerScreenMarshalsBackgroundRepositoryUpdatesIntoCompositionScope() {
        val repository = ObservableTestRepository(groups().take(1))
        compose.setContent {
            MaterialTheme {
                SymbolDrawerManagerScreen(repository = repository)
            }
        }
        compose.onNodeWithText("@").assertIsDisplayed()

        val writer = Thread {
            repository.publish(
                listOf(
                    SymbolGroup(
                        id = "external",
                        title = "External update",
                        symbols = listOf(
                            SymbolEntry(3L, "!", SymbolAction.InsertText("!"))
                        )
                    )
                )
            )
        }
        writer.start()
        writer.join()

        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("!").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("!").assertIsDisplayed()
    }

    @Test fun settingsScreenRendersMaterial3Sections() {
        compose.setContent {
            MaterialTheme {
                SymbolDrawerSettingsScreen(
                    settings = SymbolDrawerSettings(),
                    onSettingsChange = {}
                )
            }
        }
        compose.onNodeWithText("Symbol drawer settings").assertIsDisplayed()
        assertEquals(2, compose.onAllNodesWithText("Appearance").fetchSemanticsNodes().size)

        // Use the same localized resource as the production tab row. CI runners
        // may inherit a non-English system locale, so a hard-coded English tab
        // title can make the instrumentation test fail before exercising the
        // settings page itself.
        val performanceTabTitle = androidx.test.platform.app.InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .getString(com.zerostudio.idea.widget.editor.symbolinput.v2.compose.R.string.compose_ui_settings_tab_performance)
        compose.onNodeWithText(performanceTabTitle).performScrollTo().performClick()
        compose.onNodeWithText(performanceTabTitle).assertIsDisplayed()
        val cacheLabel = if (
            compose.onAllNodesWithText("Page cache limit", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        ) {
            "Page cache limit"
        } else {
            "页面缓存数量"
        }
        // IntSetting appends the current numeric value to the label, so use
        // substring matching rather than an exact-text semantics query.
        compose.onNodeWithText(cacheLabel, substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }
    @Test fun stableDrawerApiFacadeRendersComposeContent() {
        compose.setContent {
            MaterialTheme {
                com.zerostudio.idea.widget.drawer.DrawerContainer(
                    config = com.zerostudio.idea.widget.drawer.DrawerConfig(),
                    title = "Public drawer API",
                    content = { Text("Public API content") }
                )
            }
        }
        compose.onNodeWithText("Public API content").assertIsDisplayed()
    }

}
private class ObservableTestRepository(initial: List<SymbolGroup>) : ObservableSymbolGroupRepository {
    @Volatile private var snapshot: List<SymbolGroup> = initial
    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    override fun loadGroups(): List<SymbolGroup> = snapshot

    override fun saveGroups(groups: List<SymbolGroup>) {
        publish(groups)
    }

    override fun observeGroups(listener: () -> Unit): AutoCloseable {
        listeners += listener
        return AutoCloseable { listeners -= listener }
    }

    fun publish(groups: List<SymbolGroup>) {
        snapshot = groups
        listeners.forEach { it() }
    }
}
