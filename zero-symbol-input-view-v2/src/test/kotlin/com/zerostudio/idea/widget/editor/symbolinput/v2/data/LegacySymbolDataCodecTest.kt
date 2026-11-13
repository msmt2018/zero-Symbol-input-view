package com.zerostudio.idea.widget.editor.symbolinput.v2.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LegacySymbolDataCodecTest {
    @Test fun historicalClipboardActionIdsRoundTripWithoutRenumbering() {
        val legacy = """[{"n":"剪贴板","d":[{"a":1,"b":"复制"},{"a":2,"b":"剪切"}]}]"""
        val decoded = LegacySymbolDataCodec.decode(legacy)
        val roundTrip = LegacySymbolDataCodec.decode(LegacySymbolDataCodec.encode(decoded))
        assertEquals(1, roundTrip[0].symbols[0].legacyShortActionId)
        assertEquals(2, roundTrip[0].symbols[1].legacyShortActionId)
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
