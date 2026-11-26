/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.drawer.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerNavigationStateEngineTest {
    private fun page(id: String) = DrawerPageDescriptor(id, "Page $id")

    @Test
    fun selectsFirstPageAndPreservesSelectionByStableIdAcrossReordering() {
        val engine = DrawerNavigationStateEngine()
        engine.submitPages(listOf(page("alpha"), page("beta")))
        assertEquals("alpha", engine.snapshot().selectedPageId)
        assertTrue(engine.selectPage("beta"))

        engine.submitPages(listOf(page("beta"), page("alpha")))
        assertEquals("beta", engine.snapshot().selectedPageId)
        assertEquals(0, engine.selectedIndex())
    }

    @Test
    fun fallsBackToFirstPageWhenSelectedPageIsRemoved() {
        val engine = DrawerNavigationStateEngine()
        engine.submitPages(listOf(page("alpha"), page("beta"), page("gamma")))
        engine.selectPage("beta")

        engine.submitPages(listOf(page("gamma"), page("alpha")))
        assertEquals("gamma", engine.snapshot().selectedPageId)
        assertEquals(0, engine.selectedIndex())
    }

    @Test
    fun canExplicitlyResetSelectionWhenReplacingPages() {
        val engine = DrawerNavigationStateEngine()
        engine.submitPages(listOf(page("alpha"), page("beta")))
        engine.selectPage("beta")

        engine.submitPages(listOf(page("beta"), page("gamma")), preserveSelection = false)
        assertEquals("beta", engine.snapshot().selectedPageId)
    }

    @Test
    fun emptyPageSetClearsSelectionAndReportsNoSelectedIndex() {
        val engine = DrawerNavigationStateEngine()
        engine.submitPages(listOf(page("alpha")))
        engine.submitPages(emptyList())

        assertNull(engine.selectedPage())
        assertEquals(-1, engine.selectedIndex())
        assertEquals(DrawerNavigationSnapshot(null, emptyList()), engine.snapshot())
    }

    @Test
    fun rejectsDuplicateIdsWithoutMutatingPreviouslyPublishedState() {
        val engine = DrawerNavigationStateEngine()
        engine.submitPages(listOf(page("stable"), page("other")))
        engine.selectPage("other")
        val before = engine.snapshot()

        assertThrows(IllegalArgumentException::class.java) {
            engine.submitPages(listOf(page("duplicate"), page("duplicate")))
        }
        assertEquals(before, engine.snapshot())
    }

    @Test
    fun rejectsUnknownIdsAndOutOfRangeIndices() {
        val engine = DrawerNavigationStateEngine()
        engine.submitPages(listOf(page("alpha")))

        assertFalse(engine.selectPage("missing"))
        assertFalse(engine.selectPage(-1))
        assertFalse(engine.selectPage(1))
        assertEquals("alpha", engine.snapshot().selectedPageId)
    }

    @Test
    fun snapshotPageIdsAreImmutable() {
        val engine = DrawerNavigationStateEngine()
        engine.submitPages(listOf(page("alpha"), page("beta")))

        assertThrows(UnsupportedOperationException::class.java) {
            (engine.snapshot().pageIds as MutableList<String>).clear()
        }
        assertEquals(listOf("alpha", "beta"), engine.snapshot().pageIds)
    }
}
