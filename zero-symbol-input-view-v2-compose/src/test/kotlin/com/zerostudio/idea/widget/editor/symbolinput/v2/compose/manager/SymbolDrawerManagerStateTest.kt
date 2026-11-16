package com.zerostudio.idea.widget.editor.symbolinput.v2.compose.manager

import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolDrawerManagerStateTest {

    @Test
    fun commandsPublishOnlyCommittedRepositorySnapshots() {
        val repository = InMemoryRepository()
        val state = SymbolDrawerManagerState(
            com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupManager(repository)
        )

        val group = state.addGroup("Kotlin")
        assertNotNull(group)
        assertEquals("Kotlin", state.groups.single().title)
        assertEquals(state.groups, repository.groups)

        val symbol = SymbolEntry(7L, "lambda", SymbolAction.InsertText("->"))
        assertTrue(state.addSymbol(group!!.id, symbol))
        assertEquals(symbol, state.groups.single().symbols.single())
        assertEquals(state.groups, repository.groups)
    }

    @Test
    fun bundleImportAndExportRoundTripTypedActions() {
        val repository = InMemoryRepository(
            listOf(
                SymbolGroup(
                    "g",
                    "Code",
                    listOf(SymbolEntry(42L, "lambda", SymbolAction.InsertText("->")))
                )
            )
        )
        val state = SymbolDrawerManagerState(
            com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupManager(repository)
        )
        val json = state.exportJson()
        assertNotNull(json)

        val targetRepository = InMemoryRepository()
        val target = SymbolDrawerManagerState(
            com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupManager(targetRepository)
        )
        assertTrue(target.importJson(json!!))
        assertEquals(state.groups, target.groups)
        assertEquals(target.groups, targetRepository.groups)
    }

    @Test
    fun batchCopyMoveRemoveAndStableReorderUseSingleStateFacade() {
        val source = SymbolGroup(
            "source", "Source",
            listOf(
                SymbolEntry(10L, "alpha", SymbolAction.InsertText("a")),
                SymbolEntry(11L, "beta", SymbolAction.InsertText("b"))
            )
        )
        val target = SymbolGroup("target", "Target", emptyList())
        val repository = InMemoryRepository(listOf(source, target))
        val state = SymbolDrawerManagerState(
            com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupManager(repository)
        )

        val copies = state.copySymbols("source", setOf(10L, 11L), "target")
        assertEquals(2, copies?.size)
        assertEquals(listOf(10L, 11L), state.groups.first().symbols.map { it.id })
        assertEquals(2, state.groups.last().symbols.size)
        assertTrue(state.groups.last().symbols.none { it.id == 10L || it.id == 11L })

        assertTrue(state.moveSymbolsToGroup("source", setOf(10L), "target"))
        assertEquals(listOf(11L), state.groups.first().symbols.map { it.id })
        assertEquals(3, state.groups.last().symbols.size)

        val targetIds = state.groups.last().symbols.map { it.id }
        assertTrue(state.reorderSymbols("target", targetIds.reversed()))
        assertEquals(targetIds.reversed(), state.groups.last().symbols.map { it.id })

        assertEquals(2, state.removeSymbols("target", copies!!.map { it.id }.toSet()))
        assertEquals(1, state.groups.last().symbols.size)
        assertEquals(state.groups, repository.groups)
    }

    @Test
    fun invalidImportKeepsLastGoodSnapshotAndReportsError() {
        val repository = InMemoryRepository(
            listOf(SymbolGroup("existing", "Existing", emptyList()))
        )
        val state = SymbolDrawerManagerState(
            com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupManager(repository)
        )
        val before = state.groups

        assertFalse(state.importJson("{not-json"))
        assertEquals(before, state.groups)
        assertEquals(before, repository.groups)
        assertNotNull(state.lastError)
    }

    @Test
    fun failedDurableWriteDoesNotPublishNewSnapshot() {
        val repository = InMemoryRepository().apply { failWrites = true }
        val state = SymbolDrawerManagerState(
            com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupManager(repository)
        )

        assertNull(state.addGroup("Must not publish"))
        assertTrue(state.groups.isEmpty())
        assertNotNull(state.lastError)
    }

    private class InMemoryRepository(
        initial: List<SymbolGroup> = emptyList()
    ) : SymbolGroupRepository {
        var groups: List<SymbolGroup> = initial
        var failWrites: Boolean = false

        override fun loadGroups(): List<SymbolGroup> = groups

        override fun saveGroups(groups: List<SymbolGroup>) {
            check(!failWrites) { "Simulated storage failure" }
            this.groups = groups
        }
    }
}
