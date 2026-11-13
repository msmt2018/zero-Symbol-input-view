package com.zerostudio.idea.widget.editor.symbolinput.v2.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GenericSymbolTextActionsTest {

    @Test fun indentAndUnindentPreserveCaretOffset() {
        val document = FakeDocument("alpha", cursor = 3)
        val registry = GenericSymbolTextActions.createRegistry()
        assertTrue(registry.execute(SymbolActionId("builtin", "line.indent"), document, "  ")!!)
        assertEquals("  alpha", document.text)
        assertEquals(5, document.cursorOffset)
        assertTrue(registry.execute(SymbolActionId("builtin", "line.unindent"), document, "  ")!!)
        assertEquals("alpha", document.text)
        assertEquals(3, document.cursorOffset)
    }

    @Test fun registryExposesMoreThanThirtyEditorIndependentActions() {
        val registry = GenericSymbolTextActions.createRegistry()
        assertTrue(GenericSymbolTextActions.supportedCommandIds().size >= 30)
        assertTrue(registry.execute(SymbolActionId("builtin", "line.duplicate"), FakeDocument("one"), null)!!)
    }

    @Test fun lineActionsHandleLastLineAndLineBoundaries() {
        val doc = FakeDocument("one\ntwo", cursor = 5)
        assertTrue(run("line.duplicate", doc))
        assertEquals("one\ntwo\ntwo", doc.text)

        val last = FakeDocument("one\ntwo", cursor = 5)
        assertTrue(run("line.insertBelow", last))
        assertEquals("one\ntwo\n", last.text)

        val joined = FakeDocument("one\ntwo", cursor = 1)
        assertTrue(run("line.joinNext", joined))
        assertEquals("onetwo", joined.text)
    }

    @Test fun lineMoveAndDeletePreserveAdjacentLines() {
        val up = FakeDocument("one\ntwo\nthree", cursor = 5)
        assertTrue(run("line.moveUp", up))
        assertEquals("two\none\nthree", up.text)

        val down = FakeDocument("one\ntwo\nthree", cursor = 1)
        assertTrue(run("line.moveDown", down))
        assertEquals("two\none\nthree", down.text)

        val delete = FakeDocument("one\ntwo\nthree", cursor = 5)
        assertTrue(run("line.delete", delete))
        assertEquals("one\nthree", delete.text)
    }

    @Test fun selectionAndWordActionsUseDocumentOffsets() {
        val doc = FakeDocument("hello world", cursor = 7)
        assertTrue(run("cursor.selectWord", doc))
        assertEquals("world", doc.text.substring(doc.selectionStart, doc.selectionEnd))
        assertTrue(run("selection.wrapParentheses", doc))
        assertEquals("hello (world)", doc.text)

        val word = FakeDocument("alpha beta", cursor = 10)
        assertTrue(run("cursor.wordLeft", word))
        assertEquals(6, word.cursorOffset)
    }

    @Test fun transformsCommentsAndWhitespaceAreFunctional() {
        val upper = FakeDocument("hello  ", cursor = 2)
        assertTrue(run("line.uppercase", upper))
        assertEquals("HELLO  ", upper.text)
        assertTrue(run("line.trimTrailingWhitespace", upper))
        assertEquals("HELLO", upper.text)

        val comment = FakeDocument("  val x = 1", cursor = 3)
        assertTrue(run("line.toggleComment", comment))
        assertEquals("  //val x = 1", comment.text)
        assertTrue(run("line.toggleComment", comment))
        assertEquals("  val x = 1", comment.text)
    }

    @Test fun readOnlyDocumentsAllowNavigationButRejectEdits() {
        val doc = FakeDocument("alpha beta", cursor = 8, isEditable = false)
        assertTrue(run("cursor.wordLeft", doc))
        assertFalse(run("line.duplicate", doc))
    }

    private fun run(id: String, document: FakeDocument): Boolean =
        GenericSymbolTextActions.createRegistry().execute(
            SymbolActionId("builtin", id), document
        ) ?: false

    private class FakeDocument(
        var text: String,
        cursor: Int = 0,
        override val isEditable: Boolean = true
    ) : SymbolTextDocument {
        private var caret = cursor.coerceIn(0, text.length)
        override var selectionStart: Int = caret
            private set
        override var selectionEnd: Int = caret
            private set
        override val textLength: Int get() = text.length
        override val lineCount: Int get() = text.count { it == '\n' } + 1
        override val cursorOffset: Int get() = caret

        override fun lineForOffset(offset: Int): Int =
            text.take(offset.coerceIn(0, text.length)).count { it == '\n' }

        override fun lineStartOffset(line: Int): Int {
            require(line in 0 until lineCount)
            if (line == 0) return 0
            var currentLine = 0
            for (index in text.indices) {
                if (text[index] == '\n') {
                    currentLine++
                    if (currentLine == line) return index + 1
                }
            }
            return text.length
        }

        override fun lineEndOffset(line: Int): Int {
            val start = lineStartOffset(line)
            val newline = text.indexOf('\n', start)
            return if (newline < 0) text.length else newline
        }

        override fun lineText(line: Int): String =
            text.substring(lineStartOffset(line), lineEndOffset(line))

        override fun textBetween(start: Int, end: Int): String = text.substring(start, end)
        override fun charAt(offset: Int): Char = text[offset]

        override fun replace(start: Int, end: Int, replacement: String): Boolean {
            if (!isEditable) return false
            require(start in 0..text.length && end in start..text.length)
            text = text.substring(0, start) + replacement + text.substring(end)
            caret = start + replacement.length
            selectionStart = caret
            selectionEnd = caret
            return true
        }

        override fun setSelection(start: Int, end: Int) {
            val first = start.coerceIn(0, text.length)
            val second = end.coerceIn(0, text.length)
            selectionStart = minOf(first, second)
            selectionEnd = maxOf(first, second)
            caret = second
        }
    }
}
