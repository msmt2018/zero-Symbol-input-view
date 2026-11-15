package com.zerostudio.idea.widget.editor.symbolinput.v2.action

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.ClipboardOperation
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.CursorTarget
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DeleteKind
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SelectionTransform
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
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


    @Test fun negativeAndOverflowLegacyIdsRemainSafeExtensionFallbacks() {
        val delegate = FakeHost()
        val host = RegistryBackedSymbolActionHost(delegate, SymbolActionRegistry())

        assertTrue(host.executeExtension("legacy.action.-1", "negative"))
        assertTrue(host.executeExtension("legacy.action.2147483648", "overflow"))

        assertEquals(
            listOf(
                "legacy.action.-1:negative",
                "legacy.action.2147483648:overflow"
            ),
            delegate.fallbackCalls
        )
    }

    @Test fun executorDispatchesExtensionThroughRegistryBackedHostExactlyOnce() {
        val delegate = FakeHost()
        val registry = SymbolActionRegistry<SymbolActionHost>()
        val calls = mutableListOf<String>()
        registry.register(SymbolActionId.extension("sample.executor")) { host, payload ->
            assertTrue(host === delegate)
            calls += "handler:$payload"
            true
        }

        val result = SymbolActionExecutor().execute(
            SymbolAction.Extension("sample.executor", "from-executor"),
            RegistryBackedSymbolActionHost(delegate, registry)
        )

        assertEquals(SymbolActionResult.Executed, result)
        assertEquals(listOf("handler:from-executor"), calls)
        assertTrue(delegate.fallbackCalls.isEmpty())
    }

    @Test fun executorPreservesLegacyNumericActionDispatchAndDeclineSemantics() {
        val delegate = FakeHost()
        val registry = SymbolActionRegistry<SymbolActionHost>()
        var calls = 0
        registry.register(947) { _, payload ->
            calls++
            assertEquals("legacy-payload", payload)
            false
        }

        val result = SymbolActionExecutor().execute(
            SymbolAction.Extension("legacy.action.947", "legacy-payload"),
            RegistryBackedSymbolActionHost(delegate, registry)
        )

        assertEquals(SymbolActionResult.Unsupported(SymbolAction.Extension(
            "legacy.action.947",
            "legacy-payload"
        )), result)
        assertEquals(1, calls)
        assertTrue(delegate.fallbackCalls.isEmpty())
    }

    @Test fun customFallbackPreservesExistingAppExtensionDispatch() {
        val delegate = FakeHost()
        val registry = SymbolActionRegistry<SymbolActionHost>()
        val fallbackCalls = mutableListOf<String>()
        val host = RegistryBackedSymbolActionHost(delegate, registry) { id, payload ->
            fallbackCalls += "${id}:${payload}"
            true
        }

        assertTrue(host.executeExtension("app.previous", "payload"))
        assertEquals(listOf("app.previous:payload"), fallbackCalls)
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
