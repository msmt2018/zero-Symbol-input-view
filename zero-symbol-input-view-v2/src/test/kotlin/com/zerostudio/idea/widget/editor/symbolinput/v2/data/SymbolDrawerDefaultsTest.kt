package com.zerostudio.idea.widget.editor.symbolinput.v2.data

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.CursorTarget
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolDrawerDefaultsTest {
    @Test fun preservesCompleteV1FallbackSymbolSet() {
        val group = SymbolDrawerDefaults.createGroups().single()
        assertEquals("常用", group.title)
        assertEquals(39, group.symbols.size)
        assertEquals(SymbolAction.MoveCursor(CursorTarget.LINE_START), group.symbols[0].longAction)
        assertEquals(SymbolAction.MoveCursor(CursorTarget.DOCUMENT_START), group.symbols[1].longAction)
        assertEquals(SymbolAction.MoveCursor(CursorTarget.LINE_END), group.symbols[2].longAction)
        assertEquals(SymbolAction.MoveCursor(CursorTarget.DOCUMENT_END), group.symbols[11].longAction)
        assertEquals(SymbolAction.InsertText("()"), group.symbols[25].longAction)
        assertEquals(SymbolAction.OpenManager, group.symbols.last().shortAction)
    }

    @Test fun newlineTabAndDollarDefaultsKeepTheirActualCharacters() {
        val symbols = SymbolDrawerDefaults.createGroups().single().symbols
        assertEquals("\n", (symbols[35].shortAction as SymbolAction.InsertText).template)
        assertEquals("\t", (symbols[36].shortAction as SymbolAction.InsertText).template)
        assertEquals("\$\$", (symbols[31].shortAction as SymbolAction.InsertText).template)
    }
}
