package com.zerostudio.idea.widget.editor.symbolinput.v2.action

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.ClipboardOperation
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.CursorTarget
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DeleteKind
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SelectionTransform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistryBackedSymbolActionHostTest {
    private class FakeHost : SymbolActionHost {
        val fallbackCalls = mutableListOf<String>()
        override fun insertText(template: String) = false
        override fun replaceSelection(text: String) = false
        override fun surroundSelection(prefix: String, suffix: String) = false
        override fun moveCursor(target: CursorTarget) = false
        override fun delete(kind: DeleteKind) = false
        override fun transformSelection(transform: SelectionTransform) = false
        override fun executeEditorCommand(commandId: String, payload: String?) = false
        override fun performClipboard(operation: ClipboardOperation) = false
        override fun openManager() = false
        override fun executeExtension(extensionId: String, payload: String?): Boolean {
            fallbackCalls += "${extensionId}:${payload}"
            return true
        }
    }

    @Test fun registeredExtensionRunsBeforeHostFallback() {
        val delegate = FakeHost()
        val registry = SymbolActionRegistry<SymbolActionHost>()
        val calls = mutableListOf<String>()
        registry.register(SymbolActionId.extension("sample.insert")) { host, payload ->
            calls += "${payload}:${host === delegate}"
            true
        }

        val host = RegistryBackedSymbolActionHost(delegate, registry)
        assertTrue(host.executeExtension("sample.insert", "hello"))
        assertEquals(listOf("hello:true"), calls)
        assertTrue(delegate.fallbackCalls.isEmpty())
    }

    @Test fun registeredFalseDoesNotRunFallbackTwice() {
        val delegate = FakeHost()
        val registry = SymbolActionRegistry<SymbolActionHost>()
        registry.register(SymbolActionId.extension("sample.decline")) { _, _ -> false }

        assertFalse(RegistryBackedSymbolActionHost(delegate, registry)
            .executeExtension("sample.decline", null))
        assertTrue(delegate.fallbackCalls.isEmpty())
    }

    @Test fun unknownExtensionFallsBackToOriginalHost() {
        val delegate = FakeHost()
        val host = RegistryBackedSymbolActionHost(delegate, SymbolActionRegistry())

        assertTrue(host.executeExtension("plugin.unknown", "data"))
        assertEquals(listOf("plugin.unknown:data"), delegate.fallbackCalls)
    }

    @Test fun legacyNumericRegistrationResolvesWithoutChangingV1Id() {
        val delegate = FakeHost()
        val registry = SymbolActionRegistry<SymbolActionHost>()
        val calls = mutableListOf<Int>()
        registry.register(947) { host, payload ->
            assertTrue(host === delegate)
            calls += payload?.toInt() ?: -1
            true
        }

        assertTrue(RegistryBackedSymbolActionHost(delegate, registry)
            .executeExtension("legacy.action.947", "947"))
        assertEquals(listOf(947), calls)
        assertTrue(delegate.fallbackCalls.isEmpty())
    }

    @Test fun unknownLegacyNumericIdRetainsOriginalFallback() {
        val delegate = FakeHost()
        assertTrue(RegistryBackedSymbolActionHost(
            delegate,
            SymbolActionRegistry()
        ).executeExtension("legacy.action.948", "legacy"))
        assertEquals(listOf("legacy.action.948:legacy"), delegate.fallbackCalls)
    }
}
