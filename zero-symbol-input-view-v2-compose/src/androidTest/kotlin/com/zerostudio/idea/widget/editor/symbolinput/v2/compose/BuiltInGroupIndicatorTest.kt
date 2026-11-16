/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.GroupIndicatorStyle
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
import org.junit.Rule
import org.junit.Test

class BuiltInGroupIndicatorTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun everyBuiltInStyleRendersAGroupIndicator() {
        GroupIndicatorStyle.values().forEach { style ->
            composeRule.setContent {
                MaterialTheme {
                    BuiltInGroupIndicator(
                        groups = listOf(SymbolGroup("one", "One", emptyList()), SymbolGroup("two", "Two", emptyList())),
                        selectedIndex = 0,
                        settings = SymbolDrawerSettings(indicatorHeightDp = 32, indicatorStyle = style),
                        onSelect = {},
                        modifier = Modifier.fillMaxWidth().height(32.dp)
                    )
                }
            }
            composeRule.onNodeWithContentDescription("Group 1 of 2").assertIsDisplayed()
        }
    }

    @Test
    fun canvasIndicatorTapSelectsTheCorrespondingGroup() {
        composeRule.setContent {
            MaterialTheme {
                var selected by remember { mutableIntStateOf(0) }
                BuiltInGroupIndicator(
                    groups = listOf(SymbolGroup("one", "One", emptyList()), SymbolGroup("two", "Two", emptyList())),
                    selectedIndex = selected,
                    settings = SymbolDrawerSettings(indicatorHeightDp = 32, indicatorStyle = GroupIndicatorStyle.DOTS),
                    onSelect = { selected = it },
                    modifier = Modifier.fillMaxWidth().height(32.dp)
                )
            }
        }
        composeRule.onNodeWithContentDescription("Group 1 of 2").performTouchInput {
            click(Offset(300f, 16f))
        }
        composeRule.onNodeWithContentDescription("Group 2 of 2").assertIsDisplayed()
    }
}
