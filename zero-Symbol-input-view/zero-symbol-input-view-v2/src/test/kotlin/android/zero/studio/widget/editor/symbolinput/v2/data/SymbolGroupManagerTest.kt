package android.zero.studio.widget.editor.symbolinput.v2.data

import android.zero.studio.widget.editor.symbolinput.v2.model.SymbolAction
import android.zero.studio.widget.editor.symbolinput.v2.model.SymbolEntry
import android.zero.studio.widget.editor.symbolinput.v2.model.SymbolGroup
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
        val symbol = SymbolEntry(7L, "{}", SymbolAction(0, "{}"))
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
