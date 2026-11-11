package com.zerostudio.idea.widget.editor.symbolinput.v2.action

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.*
import org.junit.Assert.assertEquals
import org.junit.Test

class SymbolActionJsonCodecTest {
    @Test fun roundTripsNestedActions() {
        val action = SymbolAction.Composite(listOf(
            SymbolAction.InsertText("fun "),
            SymbolAction.SurroundSelection("{", "}"),
            SymbolAction.MoveCursor(CursorTarget.LINE_END)
        ))
        assertEquals(action, SymbolActionJsonCodec.decode(SymbolActionJsonCodec.encode(action)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnknownDiscriminator() {
        SymbolActionJsonCodec.decode(com.google.gson.JsonParser.parseString("""{"kind":"unknown"}""").asJsonObject)
    }
}
