package com.zerostudio.idea.widget.editor.symbolinput.v2.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltinSymbolActionCatalogTest {
    @Test fun everyGenericActionIsAvailableForTheManagerPicker() {
        val genericIds = GenericSymbolTextActions.supportedCommandIds()
        val catalogIds = BuiltinSymbolActionCatalog.options.map { it.commandId }.toSet()
        assertEquals(genericIds, catalogIds.intersect(genericIds))
        assertTrue(catalogIds.size >= 40)
        assertNotNull(BuiltinSymbolActionCatalog.find("line.duplicate"))
    }
}
