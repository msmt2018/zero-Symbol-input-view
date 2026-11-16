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
import kotlinx.coroutines.runBlocking
import org.junit.Test

class SymbolDrawerComposeContractTest {
    @Test
    fun stateStartsCollapsedAndKeepsStableGroupId() {
        val state = SymbolDrawerState(initiallyExpanded = false, selectedGroupId = "common")
        assertFalse(state.isExpanded)
        assertEquals(0f, state.expansionFraction, 0f)
        state.selectGroup("editing")
        assertEquals("editing", state.selectedGroupId)
    }

    @Test
    fun selectingGroupPublishesObservableStateValue() {
        val state = SymbolDrawerState(initiallyExpanded = false, selectedGroupId = null)
        state.selectGroup("editing")
        assertEquals("editing", state.selectedGroupId)
        state.selectGroup("symbols")
        assertEquals("symbols", state.selectedGroupId)
    }

    @Test
    fun stateRestoresExpandedIntent() {
        val state = SymbolDrawerState(initiallyExpanded = true, selectedGroupId = "common")
        assertTrue(state.isExpanded)
        assertEquals(1f, state.expansionFraction, 0f)
    }

    @Test
    fun dragUpdatesExpansionAndTargetAtTheHalfwayBoundary() = runBlocking {
        val state = SymbolDrawerState(initiallyExpanded = false, selectedGroupId = null)
        state.dragBy(0.49f)
        assertFalse(state.isExpanded)
        assertEquals(0.49f, state.expansionFraction, 0.001f)

        state.dragBy(0.02f)
        assertTrue(state.isExpanded)
        assertEquals(0.51f, state.expansionFraction, 0.001f)
    }

    @Test
    fun dragClampsExpansionToItsNormalizedRange() = runBlocking {
        val state = SymbolDrawerState(initiallyExpanded = false, selectedGroupId = null)
        state.dragBy(2f)
        assertEquals(1f, state.expansionFraction, 0f)
        state.dragBy(-3f)
        assertEquals(0f, state.expansionFraction, 0f)
    }


    @Test
    fun zeroDurationExpandAndCollapseDoNotRequireAnimationFrameClock() = runBlocking {
        val state = SymbolDrawerState(initiallyExpanded = false, selectedGroupId = null)

        state.expand(animationDurationMillis = 0)
        assertTrue(state.isExpanded)
        assertEquals(1f, state.expansionFraction, 0f)

        state.collapse(animationDurationMillis = 0)
        assertFalse(state.isExpanded)
        assertEquals(0f, state.expansionFraction, 0f)
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
    @Test
    fun nestedScrollExpandsBeforeChildAndConsumesOnlyAvailableRange() {
        assertEquals(-80f, drawerNestedPreScrollConsumption(-120f, 0.2f, 100f), 0f)
        assertEquals(0f, drawerNestedPreScrollConsumption(-20f, 1f, 100f), 0f)
        assertEquals(0f, drawerNestedPreScrollConsumption(40f, 0.2f, 100f), 0f)
    }

    @Test
    fun nestedScrollCollapsesOnlyUnconsumedDownwardDistance() {
        assertEquals(30f, drawerNestedPostScrollConsumption(80f, 0.3f, 100f), 0.001f)
        assertEquals(0f, drawerNestedPostScrollConsumption(40f, 0f, 100f), 0f)
        assertEquals(0f, drawerNestedPostScrollConsumption(-20f, 0.5f, 100f), 0f)
    }

    @Test
    fun nestedScrollRejectsInvalidMeasurements() {
        assertEquals(0f, drawerNestedPreScrollConsumption(Float.NaN, 0.5f, 100f), 0f)
        assertEquals(0f, drawerNestedPostScrollConsumption(20f, 0.5f, 0f), 0f)
    }


}
