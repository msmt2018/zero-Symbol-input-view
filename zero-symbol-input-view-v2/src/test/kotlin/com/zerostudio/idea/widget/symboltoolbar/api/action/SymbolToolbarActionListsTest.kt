package com.zerostudio.idea.widget.symboltoolbar.api.action

import com.zerostudio.idea.widget.symboltoolbar.api.model.SymbolToolbarInvocation
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolToolbarActionListsTest {
    private class Host : SymbolToolbarActionHost {
        val operations = mutableListOf<String>()
        override suspend fun insertText(text: String): Boolean {
            operations += "insert:$text"
            return true
        }
        override suspend fun moveCursor(relativeOffset: Int): Boolean {
            operations += "cursor:$relativeOffset"
            return true
        }
    }

    @Test
    fun clickAndLongPressListsAreIndependent() {
        val bindings = symbolItemActions(
            itemId = "arrow",
            click = { step("click") { insert("->") } },
            longPress = { step("long") { insert("=>") } }
        )
        val host = Host()
        val tap = SymbolToolbarInvocation(
            "common", "arrow", com.zerostudio.idea.widget.symboltoolbar.api.action.SymbolToolbarAction("noop"),
            SymbolToolbarInvocation.Gesture.TAP, 1L
        )
        val long = tap.copy(gesture = SymbolToolbarInvocation.Gesture.LONG_PRESS)
        runBlocking {
            bindings.click.execute(tap, host)
            assertEquals(listOf("insert:->"), host.operations)
            host.operations.clear()
            bindings.longPress.execute(long, host)
            assertEquals(listOf("insert:=>"), host.operations)
        }
    }

    @Test
    fun sequenceStopsAtFirstNonSuccess() {
        val host = Host()
        val list = SymbolToolbarActionListBuilder().apply {
            step("first") { insert("one") }
            step("reject") { SymbolToolbarActionResult.Rejected("stop") }
            step("never") { insert("three") }
        }.build()
        val invocation = SymbolToolbarInvocation(
            "common", "item", SymbolToolbarAction("test"),
            SymbolToolbarInvocation.Gesture.TAP, 1L
        )
        val result = runBlocking { list.execute(invocation, host) }
        assertTrue(result is SymbolToolbarActionResult.Rejected)
        assertEquals(listOf("insert:one"), host.operations)
    }

    @Test
    fun registryCanReplaceAndDisposeBindings() {
        val registry = SymbolToolbarActionListRegistry()
        val first = symbolItemActions("item", click = { step("first") { insert("1") } })
        val handle = registry.register(first)
        assertEquals(first, registry.find("item"))
        val second = symbolItemActions("item", longPress = { step("second") { insert("2") } })
        registry.replace(second)
        assertEquals(second, registry.find("item"))
        handle.close()
        assertEquals(null, registry.find("item"))
    }
}
