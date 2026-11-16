/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 * Author: android_zero · https://github.com/msmt2018
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.compose

import androidx.compose.ui.unit.dp
import com.zerostudio.idea.widget.drawer.compose.DrawerState
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolDrawerComposeContractTest {
    @Test
    fun stateStartsCollapsedAndKeepsStableGroupId() {
        val state = SymbolDrawerState(initiallyExpanded = false, selectedGroupId = "common")
        assertFalse(state.isExpanded)
        assertEquals(0f, state.expansionFraction)
        state.selectGroup("editing")
        assertEquals("editing", state.selectedGroupId)
    }

    @Test
    fun stateRestoresExpandedIntent() {
        val state = SymbolDrawerState(initiallyExpanded = true, selectedGroupId = "common")
        assertTrue(state.isExpanded)
        assertEquals(1f, state.expansionFraction)
    }

    @Test
    fun windowWidthBreakpointsFollowMaterialAdaptiveWidths() {
        assertEquals(SymbolDrawerWindowWidthClass.COMPACT, resolveSymbolDrawerWindowWidthClass(599.dp))
        assertEquals(SymbolDrawerWindowWidthClass.MEDIUM, resolveSymbolDrawerWindowWidthClass(600.dp))
        assertEquals(SymbolDrawerWindowWidthClass.EXPANDED, resolveSymbolDrawerWindowWidthClass(840.dp))
    }

    @Test
    fun typedSymbolActionsRemainAvailableToComposeCallbacks() {
        val entry = SymbolEntry(
            id = 1L,
            label = "lambda",
            shortAction = SymbolAction.InsertText("->"),
            longAction = SymbolAction.ReplaceSelection("=>")
        )
        assertEquals(SymbolAction.InsertText("->"), entry.shortAction)
        assertEquals(SymbolAction.ReplaceSelection("=>"), entry.longAction)
    }
}
