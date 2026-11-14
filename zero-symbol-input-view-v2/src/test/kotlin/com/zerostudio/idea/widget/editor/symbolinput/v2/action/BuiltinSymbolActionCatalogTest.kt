package com.zerostudio.idea.widget.editor.symbolinput.v2.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltinSymbolActionCatalogTest {
    @Test fun everyGenericActionIsAvailableAndEveryHostActionHasAHostContract() {
        val genericIds = GenericSymbolTextActions.supportedCommandIds()
        val catalogIds = BuiltinSymbolActionCatalog.options.map { it.commandId }
        val hostCommandIds = setOf("undo", "redo", "indent", "unindent", "format", "insert_payload")
        val registry = GenericSymbolTextActions.createRegistry()

        assertEquals("Built-in action IDs must be unique", catalogIds.size, catalogIds.toSet().size)
        assertEquals(
            "Every catalog entry must resolve to a generic handler or an explicit host command",
            hostCommandIds,
            catalogIds.toSet() - genericIds
        )
        genericIds.forEach { id ->
            assertNotNull("Generic action '$id' must have a reverse-lookup handler",
                registry.get(SymbolActionId("builtin", id)))
        }
        assertEquals(60, genericIds.size)
        assertEquals(66, catalogIds.size)
    }
}
