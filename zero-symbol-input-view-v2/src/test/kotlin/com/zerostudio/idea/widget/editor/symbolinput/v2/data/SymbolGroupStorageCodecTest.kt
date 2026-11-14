package com.zerostudio.idea.widget.editor.symbolinput.v2.data

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import org.junit.Assert.assertEquals
import org.junit.Test

class SymbolGroupStorageCodecTest {
    @Test
    fun decodeRepairsDuplicateIdsAcrossGroupsWithoutChangingActionsOrStableIds() {
        val groups = listOf(
            SymbolGroup("one", "One", listOf(
                SymbolEntry(10L, "A", SymbolAction.InsertText("a")),
                SymbolEntry(11L, "B", SymbolAction.InsertText("b"))
            )),
            SymbolGroup("two", "Two", listOf(
                SymbolEntry(10L, "C", SymbolAction.InsertText("c")),
                SymbolEntry(1L, "Existing low ID", SymbolAction.InsertText("reserved")),
                SymbolEntry(12L, "D", SymbolAction.InsertText("d"))
            ))
        )

        val decoded = SymbolGroupStorageCodec.decode(SymbolGroupStorageCodec.encode(groups))

        assertEquals(listOf(10L, 11L), decoded[0].symbols.map { it.id })
        assertEquals(listOf(2L, 1L, 12L), decoded[1].symbols.map { it.id })
        assertEquals(
            listOf("a", "b", "c", "reserved", "d"),
            decoded.flatMap { it.symbols }.map { (it.shortAction as SymbolAction.InsertText).template }
        )
        assertEquals(5, decoded.flatMap { it.symbols }.map { it.id }.distinct().size)
    }

    @Test
    fun decodeKeepsAllAlreadyUniqueIdsUnchanged() {
        val groups = listOf(
            SymbolGroup("one", "One", listOf(SymbolEntry(41L, "A", SymbolAction.InsertText("a")))),
            SymbolGroup("two", "Two", listOf(SymbolEntry(99L, "B", SymbolAction.InsertText("b"))))
        )

        val decoded = SymbolGroupStorageCodec.decode(SymbolGroupStorageCodec.encode(groups))

        assertEquals(listOf(41L, 99L), decoded.flatMap { it.symbols }.map { it.id })
    }
}
