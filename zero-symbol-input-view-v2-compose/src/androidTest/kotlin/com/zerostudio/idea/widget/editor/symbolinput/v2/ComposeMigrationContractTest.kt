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
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.AdaptiveWindowObserver
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.DrawerContainer
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.rememberDrawerContainerState
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerPage
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerPageHost
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.panel.DrawerPanelContent
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.GroupIndicatorStyle
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettingsScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        compose.onNodeWithText("First").assertExists()
        compose.onNodeWithText("@").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test fun drawerContainerUsesCompositionOwnedExpansionState() {
        var state: com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.DrawerContainerState? = null
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
        compose.onNodeWithText("Page one").assertIsDisplayed().performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithText("Page two").assertExists()
    }

    @Test fun adaptiveObserverReportsComposeLayoutBounds() {
        compose.setContent {
            Box(Modifier.size(width = 500.dp, height = 400.dp)) {
                AdaptiveWindowObserver(Modifier.fillMaxSize()) { info ->
                    Text("window-width-\${info.widthDp.toInt()}")
                }
            }
        }
        compose.onNodeWithText("window-width-500").assertExists()
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
        compose.onNodeWithText("Symbol drawer settings").assertExists()
        compose.onNodeWithText("Appearance").assertExists()
        compose.onNodeWithText("Performance").assertExists()
    }
}
