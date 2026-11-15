package com.zerostudio.idea.widget.symboltoolbar.api.action

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.legacy.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.legacy.SymbolItem
import com.zerostudio.idea.widget.symboltoolbar.api.model.SymbolToolbarInvocation
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolToolbarActionListsTest {
    private class Host : SymbolToolbarActionHost {
        val operations = mutableListOf<String>()
        override suspend fun insertText(text: String): Boolean { operations += "insert:$text"; return true }
        override suspend fun moveCursor(relativeOffset: Int): Boolean { operations += "cursor:$relativeOffset"; return true }
    }

    private fun invocation(item: SymbolItem, gesture: SymbolToolbarInvocation.Gesture) =
        SymbolToolbarInvocation(SymbolGroup("common", mutableListOf(item)), item, gesture, 1L)

    @Test fun clickAndLongPressListsAreIndependent() {
        val item = SymbolItem(shortAction = 11, display = "→", longAction = 12)
        val bindings = symbolItemActions(
            item = item,
            click = { step("click") { insert("->") } },
            longPress = { step("long") { insert("=>") } }
        )
        val host = Host()
        runBlocking {
            bindings.forGesture(SymbolToolbarInvocation.Gesture.TAP).execute(
                invocation(item, SymbolToolbarInvocation.Gesture.TAP), host
            )
            assertEquals(listOf("insert:->"), host.operations)
            host.operations.clear()
            bindings.forGesture(SymbolToolbarInvocation.Gesture.LONG_PRESS).execute(
                invocation(item, SymbolToolbarInvocation.Gesture.LONG_PRESS), host
            )
            assertEquals(listOf("insert:=>"), host.operations)
        }
    }

    @Test fun sequenceStopsAtFirstNonSuccess() {
        val item = SymbolItem(shortAction = 4, display = "x")
        val host = Host()
        val list = SymbolToolbarActionListBuilder().apply {
            step("first") { insert("one") }
            step("reject") { SymbolToolbarActionResult.Rejected("stop") }
            step("never") { insert("three") }
        }.build()
        val result = runBlocking { list.execute(invocation(item, SymbolToolbarInvocation.Gesture.TAP), host) }
        assertTrue(result is SymbolToolbarActionResult.Rejected)
        assertEquals(listOf("insert:one"), host.operations)
    }

    @Test fun registryUsesLegacyItemIdentityAndDisposesBindings() {
        val registry = SymbolToolbarActionListRegistry()
        val item = SymbolItem(shortAction = 3, display = "item")
        val first = symbolItemActions(item, click = { step("first") { insert("1") } })
        val handle = registry.register(first)
        assertEquals(first, registry.find(item))
        val second = symbolItemActions(item, longPress = { step("second") { insert("2") } })
        registry.replace(second)
        assertEquals(second, registry.find(item))
        handle.close()
        assertEquals(null, registry.find(item))
    }
}
