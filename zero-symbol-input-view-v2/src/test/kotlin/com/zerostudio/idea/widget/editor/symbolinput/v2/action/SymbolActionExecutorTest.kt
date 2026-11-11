package com.zerostudio.idea.widget.editor.symbolinput.v2.action

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolActionExecutorTest {
    private open class RecordingHost : SymbolActionHost {
        val calls = mutableListOf<String>()
        override fun insertText(template: String): Boolean { calls += "insert:$template"; return true }
        override fun replaceSelection(text: String) = true
        override fun surroundSelection(prefix: String, suffix: String) = true
        override fun moveCursor(target: CursorTarget) = true
        override fun delete(kind: DeleteKind) = true
        override fun transformSelection(transform: SelectionTransform) = true
        override fun executeEditorCommand(commandId: String, payload: String?) = true
        override fun performClipboard(operation: ClipboardOperation) = true
        override fun openManager() = true
        override fun executeExtension(extensionId: String, payload: String?) = true
    }

    @Test fun executesCompositeInOrder() {
        val host = RecordingHost()
        val result = SymbolActionExecutor().execute(
            SymbolAction.Composite(listOf(SymbolAction.InsertText("a"), SymbolAction.InsertText("b"))), host
        )
        assertEquals(SymbolActionResult.Executed, result)
        assertEquals(listOf("insert:a", "insert:b"), host.calls)
    }

    @Test fun rejectsInvalidCommandIds() {
        val result = SymbolActionExecutor().execute(SymbolAction.EditorCommand("../unsafe"), RecordingHost())
        assertTrue(result is SymbolActionResult.Invalid)
    }

    @Test fun validatesCompositeBeforeExecutingAnyStep() {
        val host = RecordingHost()
        val result = SymbolActionExecutor().execute(
            SymbolAction.Composite(listOf(
                SymbolAction.InsertText("must-not-run"),
                SymbolAction.EditorCommand("../invalid")
            )), host
        )
        assertTrue(result is SymbolActionResult.Invalid)
        assertTrue(host.calls.isEmpty())
    }

    @Test fun rejectsOversizedEditorPayload() {
        val result = SymbolActionExecutor(maxTextLength = 3).execute(
            SymbolAction.EditorCommand("editor.custom", "four"), RecordingHost()
        )
        assertTrue(result is SymbolActionResult.Invalid)
    }

    @Test fun reportsPartialCompositeFailure() {
        val host = object : RecordingHost() {
            override fun insertText(template: String): Boolean {
                calls += template
                return template != "bad"
            }
        }
        val result = SymbolActionExecutor().execute(
            SymbolAction.Composite(listOf(
                SymbolAction.InsertText("ok"),
                SymbolAction.InsertText("bad"),
                SymbolAction.InsertText("never")
            )), host
        )
        assertTrue(result is SymbolActionResult.PartialFailure)
        assertEquals(1, (result as SymbolActionResult.PartialFailure).completedActions)
        assertEquals(listOf("ok", "bad"), host.calls)
    }
}
