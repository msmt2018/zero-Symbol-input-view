/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 * Author: android_zero · https://github.com/msmt2018
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.manager

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolDrawerUsageStateTest {
    @Test fun favoritesToggleAndPruneRemoveDeletedIds() {
        val state = SymbolDrawerUsageState()
        state.toggleFavorite(1L)
        state.toggleFavorite(2L)
        state.prune(setOf(2L))
        assertEquals(setOf(2L), state.snapshot().favorites)
        state.toggleFavorite(2L)
        assertTrue(state.snapshot().favorites.isEmpty())
    }

    @Test fun recentHistoryIsUniqueNewestFirstAndBounded() {
        val state = SymbolDrawerUsageState(recentLimit = 2)
        state.recordRecentlyUsed(1L)
        state.recordRecentlyUsed(2L)
        state.recordRecentlyUsed(1L)
        state.recordRecentlyUsed(3L)
        assertEquals(listOf(3L, 1L), state.snapshot().recent)
    }

    @Test fun filtersSearchPayloadAndSortRecentByUsage() {
        val first = SymbolEntry(1L, "alpha", SymbolAction.InsertText("needle"))
        val second = SymbolEntry(2L, "beta", SymbolAction.InsertText("other"))
        val state = SymbolDrawerUsageState()
        state.recordRecentlyUsed(1L)
        state.recordRecentlyUsed(2L)
        state.recordRecentlyUsed(1L)

        assertEquals(listOf(first), state.filter(listOf(first, second), "needle", SymbolManagerFilter.ALL))
        assertTrue(state.filter(listOf(first, second), "needle", SymbolManagerFilter.ALL, false).isEmpty())
        assertEquals(listOf(first, second), state.filter(listOf(first, second), "", SymbolManagerFilter.RECENT))
        assertFalse(state.filter(listOf(first, second), "", SymbolManagerFilter.FAVORITES).isNotEmpty())
    }

    @Test fun restorePersistsNormalizedSnapshot() {
        val repository = RecordingUsageRepository()
        val state = SymbolDrawerUsageState(repository)
        state.restore(SymbolUsageSnapshot(setOf(0L, 4L), listOf(0L, 4L, 4L, 5L)))
        assertEquals(SymbolUsageSnapshot(setOf(4L), listOf(4L, 5L)), repository.saved)
    }

    private class RecordingUsageRepository : SymbolUsageRepository {
        var saved = SymbolUsageSnapshot()
        override fun load() = saved
        override fun save(snapshot: SymbolUsageSnapshot) { saved = snapshot }
    }
}
