package com.zerostudio.idea.widget.editor.symbolinput.v2.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolActionRegistryTest {
    @Test fun numericIdsReverseResolveToRegisteredFunctions() {
        val registry = SymbolActionRegistry<String>()
        val calls = mutableListOf<String>()
        val handle = registry.register(54) { context, payload ->
            calls += "$context:paste:$payload"
            true
        }

        val resolved = registry.get(54)
        assertSame(resolved, registry.get(SymbolActionId.legacy(54)))
        assertTrue(requireNotNull(registry.execute(54, "editor-1", "clipboard")))
        assertEquals(listOf("editor-1:paste:clipboard"), calls)

        handle.close()
        handle.close()
        assertNull(registry.get(54))
        assertNull(registry.execute(54, "editor-1"))
    }

    @Test fun namespacesAllowSameValueWithoutCollisions() {
        val registry = SymbolActionRegistry<Unit>()
        registry.register(SymbolActionId("builtin", "paste")) { _, _ -> true }
        registry.register(SymbolActionId("plugin", "paste")) { _, _ -> false }

        assertEquals(true, registry.execute(SymbolActionId("builtin", "paste"), Unit))
        assertEquals(false, registry.execute(SymbolActionId("plugin", "paste"), Unit))
        assertEquals(2, registry.size)
    }

    @Test fun closingReplacedRegistrationDoesNotRemoveNewHandler() {
        val registry = SymbolActionRegistry<Unit>()
        val id = SymbolActionId.extension("sample.action")
        val old = registry.register(id) { _, _ -> false }
        registry.register(id) { _, _ -> true }

        old.close()
        assertTrue(requireNotNull(registry.execute(id, Unit)))
        assertTrue(registry.unregister(id))
        assertFalse(registry.unregister(id))
    }

    @Test fun namespacedContextIdsCanUseUriLikeStableIdentifiers() {
        val id = SymbolActionId.extension("com.example/editor:paste")
        assertEquals("com.example/editor:paste", id.value)
    }

    @Test fun invalidIdsAreRejected() {
        assertTrue(runCatching { SymbolActionId("", "x") }.isFailure)
        assertTrue(runCatching { SymbolActionId("plugin", "has space") }.isFailure)
    }
}
