package com.zerostudio.idea.widget.editor.symbolinput.v2.action

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.CursorTarget
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SelectionTransform
import org.junit.Assert.*
import org.junit.Test

class SymbolTextDocumentActionHostTest {
    private class Document(var value: String) : SymbolTextDocument {
        override var isEditable: Boolean = true
        override val textLength get() = value.length
        override val lineCount get() = value.split("\n").size
        override var cursorOffset: Int = 0
            private set
        override var selectionStart: Int = 0
            private set
        override var selectionEnd: Int = 0
            private set

        override fun lineForOffset(offset: Int): Int =
            value.substring(0, offset.coerceIn(0, value.length)).count { it == '\n' }

        override fun lineStartOffset(line: Int): Int {
            if (line <= 0) return 0
            var current = 0
            repeat(line) {
                val next = value.indexOf('\n', current)
                if (next < 0) return value.length
                current = next + 1
            }
            return current
        }

        override fun lineEndOffset(line: Int): Int {
            val start = lineStartOffset(line)
            val end = value.indexOf('\n', start)
            return if (end < 0) value.length else end
        }

        override fun lineText(line: Int): String =
            value.substring(lineStartOffset(line), lineEndOffset(line))

        override fun textBetween(start: Int, end: Int): String = value.substring(start, end)
        override fun charAt(offset: Int): Char = value[offset]

        override fun replace(start: Int, end: Int, replacement: String): Boolean {
            if (!isEditable || start !in 0..value.length || end !in start..value.length) return false
            value = value.substring(0, start) + replacement + value.substring(end)
            return true
        }

        override fun setSelection(start: Int, end: Int) {
            selectionStart = start.coerceIn(0, value.length)
            selectionEnd = end.coerceIn(0, value.length)
            cursorOffset = selectionEnd
        }
    }

    @Test fun insertTextExpandsMacroAndPlacesSelection() {
        val document = Document("selected")
        document.setSelection(0, 8)
        val host = SymbolTextDocumentActionHost(document)
        assertTrue(host.insertText("before(\$S\$T\$E)"))
        assertEquals("before(selected)", document.value)
        assertEquals(7, document.selectionStart)
        assertEquals(15, document.selectionEnd)
    }

    @Test fun insertTextUsesCurrentLineWordIndentAndDocumentNewlineContext() {
        val document = Document("    val value = 1")
        document.setSelection(10, 10)
        val host = SymbolTextDocumentActionHost(document)
        assertTrue(host.insertText("[\$W]\$N\$TAB\$I\$W"))
        assertEquals("    val va[value]\n\t    valuelue = 1", document.value)
    }

    @Test fun currentLineMacroPreservesItsLeadingIndentation() {
        val document = Document("    val value = 1")
        document.setSelection(10, 10)
        val host = SymbolTextDocumentActionHost(document)
        assertTrue(host.insertText("\$L"))
        assertEquals("    val va    val value = 1lue = 1", document.value)
    }

    @Test fun middleLineInsertionUsesCrLfSeparator() {
        val document = Document("first\r\nlast")
        document.setSelection(2, 2)
        val host = SymbolTextDocumentActionHost(document)
        assertTrue(host.insertText("\$N"))
        assertEquals("fi\r\nrst\r\nlast", document.value)
    }

    @Test fun finalLineInsertionPreservesPreviousCrLfSeparator() {
        val document = Document("first\r\nlast")
        document.setSelection(document.value.length, document.value.length)
        val host = SymbolTextDocumentActionHost(document)
        assertTrue(host.insertText("x\$N"))
        assertEquals("first\r\nlastx\r\n", document.value)
    }

    @Test fun surroundSelectionKeepsInnerTextSelected() {
        val document = Document("hello")
        document.setSelection(1, 4)
        val host = SymbolTextDocumentActionHost(document)
        assertTrue(host.surroundSelection("<", ">"))
        assertEquals("h<ell>o", document.value)
        assertEquals(2, document.selectionStart)
        assertEquals(5, document.selectionEnd)
    }

    @Test fun genericEditorCommandUsesBuiltInTextRegistry() {
        val document = Document("one")
        document.setSelection(3, 3)
        val host = SymbolTextDocumentActionHost(document)
        assertTrue(host.executeEditorCommand("line.duplicate", null))
        assertEquals("one\none", document.value)
    }

    @Test fun selectionTransformUsesDocumentAdapter() {
        val document = Document("hello")
        document.setSelection(1, 4)
        val host = SymbolTextDocumentActionHost(document)
        assertTrue(host.transformSelection(SelectionTransform.UPPERCASE))
        assertEquals("hELLo", document.value)
    }

    @Test fun cursorMovementUsesGenericOffsets() {
        val document = Document("abc")
        document.setSelection(2, 2)
        val host = SymbolTextDocumentActionHost(document)
        assertTrue(host.moveCursor(CursorTarget.LEFT))
        assertEquals(1, document.cursorOffset)
    }
    @Test fun backwardDeleteRemovesWholeSupplementaryCodePoint() {
        val document = Document("a😀b")
        document.setSelection(3, 3)
        val host = SymbolTextDocumentActionHost(document)
        assertTrue(host.delete(com.zerostudio.idea.widget.editor.symbolinput.v2.model.DeleteKind.BACKWARD))
        assertEquals("ab", document.value)
    }

    @Test fun deletingFinalCrLfLineRemovesWholeSeparator() {
        val document = Document("first\r\nlast")
        document.setSelection(document.value.length, document.value.length)
        val host = SymbolTextDocumentActionHost(document)
        assertTrue(host.delete(com.zerostudio.idea.widget.editor.symbolinput.v2.model.DeleteKind.CURRENT_LINE))
        assertEquals("first", document.value)
    }

}
