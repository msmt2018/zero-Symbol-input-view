package com.zerostudio.idea.widget.editor.symbolinput.v2.data

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Direct execution coverage for every public SymbolGroupManager command. */
class SymbolGroupManagerMethodExecutionTest {
    private class Repository(initial: List<SymbolGroup>) : SymbolGroupRepository {
        var stored = initial
        var saveCount = 0
        var failNextSave = false
        override fun loadGroups(): List<SymbolGroup> = stored
        override fun saveGroups(groups: List<SymbolGroup>) {
            if (failNextSave) {
                failNextSave = false
                error("synthetic persistence failure")
            }
            stored = groups.toList()
            saveCount++
        }
    }

    private fun entry(id: Long, label: String = "symbol-$id") =
        SymbolEntry(id, label, SymbolAction.InsertText(label))

    private fun initial() = listOf(
        SymbolGroup("a", "Alpha", listOf(entry(1), entry(2))),
        SymbolGroup("b", "Beta", listOf(entry(3))),
        SymbolGroup("c", "Gamma", emptyList())
    )

    @Test
    fun reloadAndReplaceAllInvokeRepositoryAndPublishSnapshots() {
        val repository = Repository(initial())
        val manager = SymbolGroupManager(repository)
        val external = listOf(SymbolGroup("external", "External", listOf(entry(9))))
        repository.stored = external

        manager.reload()
        assertEquals(external, manager.groups)

        val replacement = listOf(SymbolGroup("replacement", "Replacement", listOf(entry(10))))
        manager.replaceAll(replacement)
        assertEquals(replacement, manager.groups)
        assertEquals(replacement, repository.stored)
        assertEquals(1, repository.saveCount)
    }

    @Test
    fun moveGroupMoveSymbolAndReorderSymbolsChangePersistedOrder() {
        val repository = Repository(initial())
        val manager = SymbolGroupManager(repository)

        manager.moveGroup(0, 2)
        assertEquals(listOf("b", "c", "a"), manager.groups.map { it.id })

        manager.moveSymbol("a", 0, 1)
        assertEquals(listOf(2L, 1L), manager.groups.single { it.id == "a" }.symbols.map { it.id })

        manager.reorderSymbols("a", listOf(1L, 2L))
        assertEquals(listOf(1L, 2L), manager.groups.single { it.id == "a" }.symbols.map { it.id })
        assertEquals(manager.groups, repository.stored)
        assertEquals(3, repository.saveCount)
    }

    @Test
    fun copyAndMoveMultipleSymbolsAreAtomicAndPreserveActionPayloads() {
        var nextId = 100L
        val repository = Repository(initial())
        val manager = SymbolGroupManager(repository, newSymbolId = { ++nextId })

        val copies = manager.copySymbols("a", linkedSetOf(1L, 2L), "b")
        assertEquals(listOf("symbol-1", "symbol-2"), copies.map { it.label })
        assertEquals(listOf(101L, 102L), copies.map { it.id })
        assertTrue(copies.zip(listOf(entry(1), entry(2))).all { (copy, source) ->
            copy.shortAction == source.shortAction
        })

        manager.moveSymbolsToGroup("a", setOf(2L), "c")
        assertEquals(listOf(1L), manager.groups.single { it.id == "a" }.symbols.map { it.id })
        assertEquals(listOf(2L), manager.groups.single { it.id == "c" }.symbols.map { it.id })
        assertEquals(listOf(3L, 101L, 102L), manager.groups.single { it.id == "b" }.symbols.map { it.id })
        assertEquals(manager.groups, repository.stored)
        assertEquals(2, repository.saveCount)
    }

    @Test
    fun duplicateIdsAndInvalidOrdersAreRejectedBeforePersistence() {
        val repository = Repository(initial())
        val manager = SymbolGroupManager(repository)
        val before = manager.groups
        val writesBefore = repository.saveCount

        assertTrue(runCatching {
            manager.replaceAll(listOf(
                SymbolGroup("duplicate", "One", emptyList()),
                SymbolGroup("duplicate", "Two", emptyList())
            ))
        }.isFailure)
        assertTrue(runCatching { manager.moveGroup(-1, 0) }.isFailure)
        assertTrue(runCatching { manager.moveSymbol("a", 0, 99) }.isFailure)
        assertTrue(runCatching { manager.reorderSymbols("a", listOf(1L, 1L)) }.isFailure)
        assertTrue(runCatching { manager.reorderSymbols("a", listOf(1L)) }.isFailure)

        assertEquals(before, manager.groups)
        assertEquals(before, repository.stored)
        assertEquals(writesBefore, repository.saveCount)
    }

    @Test
    fun addDuplicateRenameAndRemoveGroupExecuteAndPersist() {
        var groupId = 0
        var symbolId = 10L
        val repository = Repository(initial())
        val manager = SymbolGroupManager(
            repository,
            newId = { "created-${++groupId}" },
            newSymbolId = { ++symbolId }
        )

        val added = manager.addGroup("  Delta  ", index = 1)
        assertEquals("created-1", added.id)
        assertEquals("Delta", added.title)
        assertEquals(listOf("a", "created-1", "b", "c"), manager.groups.map { it.id })

        val duplicated = manager.duplicateGroup("a", index = 0)
        assertEquals("created-2", duplicated.id)
        assertEquals("Alpha copy", duplicated.title)
        assertEquals(listOf(11L, 12L), duplicated.symbols.map { it.id })
        assertEquals(listOf("created-2", "a", "created-1", "b", "c"), manager.groups.map { it.id })

        manager.renameGroup("created-1", "  Delta renamed  ")
        assertEquals("Delta renamed", manager.groups.single { it.id == "created-1" }.title)

        manager.removeGroup("created-2")
        assertFalse(manager.groups.any { it.id == "created-2" })
        assertEquals(manager.groups, repository.stored)
        assertEquals(4, repository.saveCount)
    }

    @Test
    fun addUpdateRemoveAndCopySymbolExecuteWithStableIds() {
        var nextId = 40L
        val repository = Repository(initial())
        val manager = SymbolGroupManager(repository, newSymbolId = { ++nextId })

        val generated = entry(0, "generated")
        manager.addSymbol("a", generated, index = 1)
        val inserted = manager.groups.single { it.id == "a" }.symbols[1]
        assertEquals(41L, inserted.id)
        assertEquals("generated", inserted.label)

        val changed = inserted.copy(label = "renamed", shortAction = SymbolAction.ReplaceSelection("new"))
        manager.updateSymbol("a", changed)
        assertEquals(changed, manager.groups.single { it.id == "a" }.symbols[1])

        val copied = manager.copySymbol("a", 1L, "b")
        assertEquals(42L, copied.id)
        assertEquals("symbol-1", copied.label)
        assertEquals(SymbolAction.InsertText("symbol-1"), copied.shortAction)

        manager.removeSymbol("a", 2L)
        assertFalse(manager.groups.single { it.id == "a" }.symbols.any { it.id == 2L })
        assertTrue(manager.groups.single { it.id == "b" }.symbols.any { it.id == copied.id })
        assertEquals(manager.groups, repository.stored)
        assertEquals(4, repository.saveCount)
    }

    @Test
    fun failedRepositoryWriteDoesNotPublishInMemoryChanges() {
        val repository = Repository(initial())
        val manager = SymbolGroupManager(repository, newId = { "never-published" })
        val before = manager.groups
        repository.failNextSave = true

        assertTrue(runCatching { manager.addGroup("Will fail") }.isFailure)
        assertEquals(before, manager.groups)
        assertEquals(before, repository.stored)
        assertEquals(0, repository.saveCount)
    }

    @Test
    fun invalidCommandsAreRejectedWithoutPartialWrites() {
        val repository = Repository(initial())
        val manager = SymbolGroupManager(repository)
        val before = manager.groups

        assertTrue(runCatching { manager.addGroup("  ") }.isFailure)
        assertTrue(runCatching { manager.renameGroup("a", " ") }.isFailure)
        assertTrue(runCatching { manager.renameGroup("missing", "Valid") }.isFailure)
        assertTrue(runCatching { manager.duplicateGroup("missing") }.isFailure)
        assertTrue(runCatching { manager.updateSymbol("a", entry(999)) }.isFailure)
        assertTrue(runCatching { manager.removeSymbol("a", 999) }.isFailure)
        assertTrue(runCatching { manager.copySymbols("a", emptySet(), "b") }.isFailure)
        assertTrue(runCatching { manager.removeSymbols("a", emptySet()) }.isFailure)
        assertTrue(runCatching { manager.moveSymbolsToGroup("a", setOf(1L), "a") }.isFailure)
        assertTrue(runCatching { manager.moveSymbolToGroup("a", 999L, "b") }.isFailure)

        assertEquals(before, manager.groups)
        assertEquals(before, repository.stored)
        assertEquals(0, repository.saveCount)
    }

    @Test
    fun removeUnknownGroupIsAnIdempotentPersistedCommand() {
        val repository = Repository(initial())
        val manager = SymbolGroupManager(repository)
        manager.removeGroup("not-present")
        assertEquals(initial(), manager.groups)
        assertEquals(1, repository.saveCount)
        assertFalse(manager.groups.isEmpty())
    }
}
