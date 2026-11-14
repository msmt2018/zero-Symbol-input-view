/*
 * This file is part of ZeroStudio.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.data

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import org.junit.Assert.*
import org.junit.Test

class SymbolGroupManagerContractTest {
    private class MemoryRepository(initial: List<SymbolGroup>) : SymbolGroupRepository {
        var data = initial
        var failWrites = false
        var writes = 0
        override fun loadGroups() = data
        override fun saveGroups(groups: List<SymbolGroup>) {
            check(!failWrites) { "simulated write failure" }
            data = groups.toList()
            writes++
        }
    }

    private fun symbol(id: Long, label: String = "s$id") =
        SymbolEntry(id, label, SymbolAction.InsertText(label))

    @Test fun groupCrudDuplicateAndReorderPersistStableIdentities() {
        val repo = MemoryRepository(listOf(SymbolGroup("a", "A", listOf(symbol(1)))))
        var groupCounter = 0
        var symbolCounter = 10L
        val manager = SymbolGroupManager(repo, newId = { "g" + (++groupCounter) }, newSymbolId = { ++symbolCounter })
        val added = manager.addGroup("  B  ")
        assertEquals("B", added.title)
        manager.renameGroup(added.id, "Renamed")
        val duplicate = manager.duplicateGroup("a")
        assertNotEquals("a", duplicate.id)
        assertNotEquals(1L, duplicate.symbols.single().id)
        manager.moveGroup(0, 2)
        assertEquals(listOf(added.id, duplicate.id, "a"), manager.groups.map { it.id })
        manager.removeGroup(added.id)
        assertEquals(listOf(duplicate.id, "a"), repo.data.map { it.id })
    }

    @Test fun symbolCrudCopyMoveAndBatchRemovePreserveActions() {
        val first = symbol(1, "alpha")
        val repo = MemoryRepository(listOf(
            SymbolGroup("a", "A", listOf(first, symbol(2, "beta"))),
            SymbolGroup("b", "B", emptyList())
        ))
        var generated = 100L
        val manager = SymbolGroupManager(repo, newSymbolId = { ++generated })
        manager.addSymbol("a", symbol(0, "gamma"))
        assertTrue(manager.groups.first().symbols.any { it.label == "gamma" && it.id != 0L })
        manager.updateSymbol("a", first.copy(label = "changed"))
        val copy = manager.copySymbol("a", 1L, "b")
        assertNotEquals(1L, copy.id)
        assertEquals(first.shortAction, copy.shortAction)
        manager.moveSymbolToGroup("a", 2L, "b")
        assertFalse(manager.groups.first().symbols.any { it.id == 2L })
        assertTrue(manager.groups.last().symbols.any { it.id == 2L })
        assertEquals(1, manager.removeSymbols("b", setOf(copy.id)))
        manager.removeSymbol("a", 1L)
        assertFalse(manager.groups.first().symbols.any { it.id == 1L })
    }

    @Test fun staleBatchSelectionAndInvalidOrderDoNotPartiallyWrite() {
        val repo = MemoryRepository(listOf(SymbolGroup("a", "A", listOf(symbol(1)))))
        val manager = SymbolGroupManager(repo)
        val writes = repo.writes
        assertThrows(IllegalArgumentException::class.java) { manager.removeSymbols("a", setOf(1L, 99L)) }
        assertThrows(IllegalArgumentException::class.java) { manager.reorderSymbols("a", listOf(99L)) }
        assertEquals(writes, repo.writes)
        assertEquals(listOf(1L), manager.groups.single().symbols.map { it.id })
    }

    @Test fun failedPersistenceDoesNotPublishUncommittedSnapshot() {
        val initial = listOf(SymbolGroup("a", "A", emptyList()))
        val repo = MemoryRepository(initial)
        val manager = SymbolGroupManager(repo)
        repo.failWrites = true
        assertThrows(IllegalStateException::class.java) { manager.addGroup("B") }
        assertEquals(initial, manager.groups)
        assertEquals(initial, repo.data)
    }

    @Test fun invalidSourceTargetAndSameGroupMovesAreRejected() {
        val manager = SymbolGroupManager(MemoryRepository(listOf(SymbolGroup("a", "A", listOf(symbol(1))))))
        assertThrows(IllegalArgumentException::class.java) { manager.moveSymbolToGroup("a", 1L, "a") }
        assertThrows(IllegalStateException::class.java) { manager.copySymbol("missing", 1L, "a") }
        assertThrows(IllegalStateException::class.java) { manager.copySymbol("a", 1L, "missing") }
    }
}
