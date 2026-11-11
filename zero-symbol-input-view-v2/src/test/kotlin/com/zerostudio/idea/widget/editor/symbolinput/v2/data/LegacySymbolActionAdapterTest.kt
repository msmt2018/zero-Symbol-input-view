package com.zerostudio.idea.widget.editor.symbolinput.v2.data

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacySymbolActionAdapterTest {
    @Test fun convertsKnownInsertActionToTypedAction() {
        assertEquals(
            SymbolAction.InsertText("abc"),
            LegacySymbolActionAdapter.convert(LegacySymbolAction(0, "abc"))
        )
    }

    @Test fun convertsManagerActionToDedicatedAction() {
        assertEquals(
            SymbolAction.OpenManager,
            LegacySymbolActionAdapter.convert(LegacySymbolAction(22))
        )
    }

    @Test fun retainsUnknownActionAsExtension() {
        val result = LegacySymbolActionAdapter.convert(LegacySymbolAction(987, "data"))
        assertTrue(result is SymbolAction.Extension)
        assertEquals("legacy.action.987", (result as SymbolAction.Extension).extensionId)
    }
}
