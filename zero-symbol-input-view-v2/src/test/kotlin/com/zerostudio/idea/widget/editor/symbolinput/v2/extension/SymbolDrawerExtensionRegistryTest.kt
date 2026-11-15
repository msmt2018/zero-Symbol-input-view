package com.zerostudio.idea.widget.editor.symbolinput.v2.extension

import com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionHost
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionId
import org.junit.Assert.*
import org.junit.Test

class SymbolDrawerExtensionRegistryTest {
    private class Host : SymbolActionHost {
        var last: Pair<String, String?>? = null
        override fun insertText(template: String) = true
        override fun replaceSelection(text: String) = true
        override fun surroundSelection(prefix: String, suffix: String) = true
        override fun moveCursor(target: com.zerostudio.idea.widget.editor.symbolinput.v2.model.CursorTarget) = true
        override fun delete(kind: com.zerostudio.idea.widget.editor.symbolinput.v2.model.DeleteKind) = true
        override fun transformSelection(transform: com.zerostudio.idea.widget.editor.symbolinput.v2.model.SelectionTransform) = true
        override fun executeEditorCommand(commandId: String, payload: String?) = true
        override fun performClipboard(operation: com.zerostudio.idea.widget.editor.symbolinput.v2.model.ClipboardOperation) = true
        override fun openManager() = true
        override fun executeExtension(extensionId: String, payload: String?): Boolean {
            last = extensionId to payload
            return true
        }
    }

    @Test fun registeredActionIsDispatchedAndHandleUnregistersIt() {
        val registry = SymbolDrawerExtensionRegistry()
        val host = Host()
        var called = false
        val handle = registry.registerAction(
            SymbolDrawerActionExtension(
                id = "sample.insert",
                title = "Sample",
                handler = { _, payload -> called = payload == "hello"; true }
            )
        )
        val wrapped = registry.wrapActionHost(host)
        assertTrue(wrapped.executeExtension("sample.insert", "hello"))
        assertTrue(called)
        handle.close()
        assertFalse(wrapped.executeExtension("sample.insert", "fallback"))
        assertEquals("sample.insert", host.last?.first)
    }

    @Test fun customActionReplacesByIdWithoutOldHandleRemovingReplacement() {
        val registry = SymbolDrawerExtensionRegistry()
        var first = false
        var second = false
        val firstHandle = registry.registerAction(
            SymbolDrawerActionExtension("app.command", "First", handler = { _, _ -> first = true; true })
        )
        registry.registerAction(
            SymbolDrawerActionExtension("app.command", "Second", handler = { _, _ -> second = true; true })
        )
        firstHandle.close()
        registry.wrapActionHost(Host()).executeExtension("app.command", null)
        assertFalse(first)
        assertTrue(second)
    }

    @Test fun builtInVisibilityDefaultsToVisibleAndCanBeRestored() {
        val registry = SymbolDrawerExtensionRegistry()
        assertTrue(registry.isBuiltInVisible("builtin.search"))
        registry.setBuiltInVisible("builtin.search", false)
        assertFalse(registry.isBuiltInVisible("builtin.search"))
        registry.setBuiltInVisible("builtin.search", true)
        assertTrue(registry.isBuiltInVisible("builtin.search"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidExtensionIdIsRejected() {
        SymbolDrawerExtensionRegistry().registerAction(
            SymbolDrawerActionExtension("bad id", "Bad", handler = { _, _ -> true })
        )
    }
}
