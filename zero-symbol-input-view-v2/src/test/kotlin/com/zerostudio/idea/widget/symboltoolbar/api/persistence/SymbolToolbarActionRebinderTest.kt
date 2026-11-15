package com.zerostudio.idea.widget.symboltoolbar.api.persistence

import com.google.gson.JsonObject
import com.zerostudio.idea.widget.symboltoolbar.api.action.SymbolToolbarAction
import com.zerostudio.idea.widget.symboltoolbar.api.action.SymbolToolbarActionHost
import com.zerostudio.idea.widget.symboltoolbar.api.action.SymbolToolbarActionResult
import com.zerostudio.idea.widget.symboltoolbar.api.model.SymbolToolbarInvocation
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolToolbarActionRebinderTest {
    private class Host : SymbolToolbarActionHost {
        val inserted = mutableListOf<String>()
        override suspend fun insertText(text: String): Boolean {
            inserted += text
            return true
        }
    }

    @Test
    fun persistedActionRebindsToPluginCode() {
        val rebinder = SymbolToolbarActionRebinder()
        val handle = rebinder.register("plugin.insert") { arguments ->
            insert(arguments.asJsonObject.get("text").asString)
        }
        val persisted = PersistedSymbolItemActions(
            itemId = "arrow",
            click = listOf(
                PersistedSymbolAction("step-1", "plugin.insert", JsonObject().apply {
                    addProperty("text", " -> ")
                })
            )
        )
        val list = rebinder.bind(persisted, SymbolToolbarInvocation.Gesture.TAP)
        val host = Host()
        val invocation = SymbolToolbarInvocation(
            "symbols", "arrow", SymbolToolbarAction("noop"),
            SymbolToolbarInvocation.Gesture.TAP, 12L
        )
        val result = runBlocking { list.execute(invocation, host) }
        assertEquals(SymbolToolbarActionResult.Success, result)
        assertEquals(listOf(" -> "), host.inserted)
        handle.close()
    }

    @Test
    fun missingPluginActionIsUnsupported() {
        val rebinder = SymbolToolbarActionRebinder()
        val persisted = PersistedSymbolItemActions(
            itemId = "arrow",
            click = listOf(PersistedSymbolAction("missing", "missing.plugin.action"))
        )
        val list = rebinder.bind(persisted, SymbolToolbarInvocation.Gesture.TAP)
        val result = runBlocking {
            list.execute(
                SymbolToolbarInvocation(
                    "symbols", "arrow", SymbolToolbarAction("noop"),
                    SymbolToolbarInvocation.Gesture.TAP, 1L
                ),
                Host()
            )
        }
        assertTrue(result is SymbolToolbarActionResult.Unsupported)
    }
}
