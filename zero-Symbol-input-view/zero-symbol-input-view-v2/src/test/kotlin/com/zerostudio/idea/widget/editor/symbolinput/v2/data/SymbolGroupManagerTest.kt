package com.zerostudio.idea.widget.editor.symbolinput.v2.data

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolGroupManagerTest {
    private class MemoryRepository(initial: List<SymbolGroup>) : SymbolGroupRepository {
        var stored = initial
        override fun loadGroups() = stored
        override fun saveGroups(groups: List<SymbolGroup>) { stored = groups }
    }

    @Test
    fun moveSymbolBetweenGroupsPreservesSymbolIdentityAndAction() {
        val symbol = SymbolEntry(7L, "{}", SymbolAction.InsertText("{}"))
        val repository = MemoryRepository(
            listOf(
                SymbolGroup("a", "A", listOf(symbol)),
                SymbolGroup("b", "B", emptyList())
            )
        )
        val manager = SymbolGroupManager(repository)

        manager.moveSymbolToGroup("a", 7L, "b")

        assertTrue(manager.groups[0].symbols.isEmpty())
        assertEquals(symbol, manager.groups[1].symbols.single())
    }

    @Test
    fun groupReorderingPersistsNewOrder() {
        val repository = MemoryRepository(
            listOf(
                SymbolGroup("a", "A", emptyList()),
                SymbolGroup("b", "B", emptyList())
            )
        )
        val manager = SymbolGroupManager(repository)

        manager.moveGroup(0, 1)

        assertEquals(listOf("b", "a"), repository.stored.map { it.id })
    }
}
