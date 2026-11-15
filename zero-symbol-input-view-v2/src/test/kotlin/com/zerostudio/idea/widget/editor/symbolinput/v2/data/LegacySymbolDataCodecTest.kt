package com.zerostudio.idea.widget.editor.symbolinput.v2.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LegacySymbolDataCodecTest {
    @Test
    fun decodeAllocatesSymbolIdsAcrossAllGroupsAndPreservesLegacyActions() {
        val json = """
            [
              {"n":"First","d":[{"a":0,"b":"A","c":"alpha"},{"a":3,"b":"B"}]},
              {"n":"Second","d":[{"a":7,"b":"C","c":"payload"}]}
            ]
        """.trimIndent()

        val groups = LegacySymbolDataCodec.decode(json)

        assertEquals(listOf(1L, 2L), groups[0].symbols.map { it.id })
        assertEquals(listOf(3L), groups[1].symbols.map { it.id })
        assertEquals(listOf(1L, 2L, 3L), groups.flatMap { it.symbols }.map { it.id })
        assertEquals(0, groups[0].symbols[0].legacyShortActionId)
        assertEquals("alpha", groups[0].symbols[0].legacyShortText)
        assertEquals(3, groups[0].symbols[1].legacyShortActionId)
        assertEquals(7, groups[1].symbols[0].legacyShortActionId)
        assertEquals("payload", groups[1].symbols[0].legacyShortText)
    }

    @Test fun historicalClipboardActionIdsRoundTripWithoutRenumbering() {
        val legacy = """[{"n":"剪贴板","d":[{"a":0,"b":"复制","d":1},{"a":0,"b":"剪切","d":2}]}]"""
        val decoded = LegacySymbolDataCodec.decode(legacy)
        val roundTrip = LegacySymbolDataCodec.decode(LegacySymbolDataCodec.encode(decoded))
        assertEquals(1, roundTrip[0].symbols[0].legacyLongActionId)
        assertEquals(2, roundTrip[0].symbols[1].legacyLongActionId)
    }

    @Test fun v1CompactExportPreservesGroupAndSymbolActions() {
        val json = """[{"n":"常用","d":[{"a":0,"b":"括号","c":"()","d":0,"e":"[]"}]}]"""
        val decoded = LegacySymbolDataCodec.decode(json)
        assertEquals(json, LegacySymbolDataCodec.encode(decoded))
    }

    @Test(expected = IllegalStateException::class)
    fun v1CompactExportRejectsV2OnlyCompositeAction() {
        val group = com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup(
            id = "custom",
            title = "Custom",
            symbols = listOf(
                com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry(
                    id = 1L,
                    label = "Composite",
                    shortAction = com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction.Composite(
                        listOf(com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction.InsertText("a"))
                    )
                )
            )
        )
        LegacySymbolDataCodec.encode(listOf(group))
    }

    @Test fun historicalLineDeleteIdKeepsItsV1DeleteSemantics() {
        val legacy = """[{"n":"行","d":[{"a":34,"b":"删除行"}]}]"""
        val decoded = LegacySymbolDataCodec.decode(legacy)
        assertEquals(
            com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction.Delete(
                com.zerostudio.idea.widget.editor.symbolinput.v2.model.DeleteKind.CURRENT_LINE
            ),
            decoded[0].symbols[0].shortAction
        )
        assertEquals(legacy, LegacySymbolDataCodec.encode(decoded))
    }
}
