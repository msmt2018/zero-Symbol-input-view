/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 * Author: android_zero · https://github.com/msmt2018
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SymbolDrawerComposeUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersSymbolLabelsUsingCompose() {
        composeRule.setContent {
            MaterialTheme {
                Box(Modifier.fillMaxWidth().height(240.dp)) {
                    SymbolDrawer(
                        groups = listOf(
                            SymbolGroup(
                                id = "common",
                                title = "Common",
                                symbols = listOf(
                                    SymbolEntry(
                                        id = 1L,
                                        label = "lambda",
                                        shortAction = SymbolAction.InsertText("->")
                                    )
                                )
                            )
                        ),
                        state = rememberSymbolDrawerState(),
                        onSymbolClick = {}
                    )
                }
            }
        }
        composeRule.onNodeWithText("lambda").assertIsDisplayed()
    }

    @Test
    fun managerRendersSymbolsAndDispatchesEditCommands() {
        var editedSymbolId: Long? = null
        val symbol = SymbolEntry(
            id = 42L,
            label = "lambda",
            shortAction = SymbolAction.InsertText("->")
        )
        composeRule.setContent {
            MaterialTheme {
                com.zerostudio.idea.widget.editor.symbolinput.v2.compose.manager.SymbolDrawerManagerScreen(
                    groups = listOf(SymbolGroup("common", "Common", listOf(symbol))),
                    onSelectGroup = {},
                    onAddGroup = {},
                    onRenameGroup = {},
                    onDeleteGroup = {},
                    selectedGroupId = "common",
                    onEditSymbol = { _, selected -> editedSymbolId = selected.id }
                )
            }
        }
        composeRule.onNodeWithText("lambda").assertIsDisplayed()
        composeRule.onNodeWithText("Edit").performClick()
        composeRule.runOnIdle { assertEquals(42L, editedSymbolId) }
    }

    @Test
    fun drawerUsesTheSuppliedMultiDetentStateForInitialPresentation() {
        val detentState = SymbolDrawerDetentState(
            initialDetentId = SymbolDrawerDetent.HalfExpanded.id,
            selectedGroupId = "common"
        )
        composeRule.setContent {
            MaterialTheme {
                Box(Modifier.fillMaxWidth().height(240.dp)) {
                    SymbolDrawer(
                        groups = listOf(
                            SymbolGroup(
                                id = "common",
                                title = "Common",
                                symbols = listOf(
                                    SymbolEntry(
                                        id = 7L,
                                        label = "compose-detent",
                                        shortAction = SymbolAction.InsertText("compose")
                                    )
                                )
                            )
                        ),
                        state = rememberSymbolDrawerState(),
                        detentState = detentState,
                        onSymbolClick = {}
                    )
                }
            }
        }

        composeRule.onNodeWithText("compose-detent").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals("half-expanded", detentState.targetDetentId)
            assertEquals(0.5f, detentState.expansionFraction, 0f)
            assertEquals("common", detentState.selectedGroupId)
        }
    }
    @Test
    fun upwardGridGestureExpandsCollapsedDrawerBeforeGridConsumesScroll() {
        lateinit var drawerState: SymbolDrawerState
        val symbols = List(60) { index ->
            SymbolEntry(
                id = index.toLong(),
                label = "scroll-symbol-$index",
                shortAction = SymbolAction.InsertText("symbol-$index")
            )
        }
        composeRule.setContent {
            MaterialTheme {
                Box(Modifier.fillMaxWidth().height(280.dp)) {
                    drawerState = rememberSymbolDrawerState()
                    SymbolDrawer(
                        groups = listOf(SymbolGroup("scroll", "Scroll", symbols)),
                        state = drawerState,
                        settings = com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings(
                            columns = 5,
                            collapsedRows = 1,
                            indicatorHeightDp = 0,
                            animationDurationMs = 0,
                            minimumTouchTargetDp = 40
                        ),
                        onSymbolClick = {}
                    )
                }
            }
        }

        composeRule.runOnIdle {
            assertEquals(0f, drawerState.expansionFraction, 0f)
        }
        composeRule.onNodeWithText("scroll-symbol-0")
            .performTouchInput { swipeUp() }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertTrue(
                "An upward gesture starting in the symbol grid should expand the drawer first",
                drawerState.expansionFraction > 0f
            )
        }
    }

}
