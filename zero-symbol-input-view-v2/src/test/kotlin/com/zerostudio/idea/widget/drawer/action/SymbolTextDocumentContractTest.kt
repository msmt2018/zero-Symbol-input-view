package com.zerostudio.idea.widget.drawer.action

import org.junit.Assert.*
import org.junit.Test

class SymbolTextDocumentContractTest {
    private class Buffer(var value: String) : SymbolTextDocument {
        private var caret = 0
        override val isEditable = true
        override val textLength get() = value.length
        override val lineCount get() = value.split("\n").size
        override val cursorOffset get() = caret
        override val selectionStart get() = caret
        override val selectionEnd get() = caret
        private fun starts(): List<Int> = buildList {
            add(0)
            value.forEachIndexed { index, char -> if (char == '\n') add(index + 1) }
        }
        override fun lineForOffset(offset: Int): Int =
            starts().indexOfLast { it <= offset.coerceIn(0, value.length) }.coerceAtLeast(0)
        override fun lineStartOffset(line: Int) = starts()[line]
        override fun lineEndOffset(line: Int): Int {
            val start = lineStartOffset(line)
            val next = starts().getOrNull(line + 1) ?: value.length
            return if (next > start && value[next - 1] == '\n') next - 1 else next
        }
        override fun lineText(line: Int) = textBetween(lineStartOffset(line), lineEndOffset(line))
        override fun textBetween(start: Int, end: Int) = value.substring(start, end)
        override fun charAt(offset: Int) = value[offset]
        override fun replace(start: Int, end: Int, replacement: String): Boolean {
            value = value.replaceRange(start, end, replacement)
            caret = start + replacement.length
            return true
        }
        override fun setSelection(start: Int, end: Int) { caret = end }
    }

    @Test fun convertsBetweenUtf16OffsetsAndLineColumn() {
        val document = Buffer("abc\nxy")
        assertEquals(TextDocumentPosition(1, 1), document.positionForOffset(5))
        assertEquals(5, document.offsetForPosition(TextDocumentPosition(1, 1)))
        assertEquals(6, document.offsetForPosition(TextDocumentPosition(1, 99)))
    }

    @Test fun appliesNonOverlappingEditsFromRightToLeft() {
        val document = Buffer("abcdef")
        assertTrue(document.applyEdits(listOf(
            TextDocumentEdit(1, 2, "XX"),
            TextDocumentEdit(4, 6, "!")
        )))
        assertEquals("aXXcd!", document.value)
    }

    @Test fun rejectsOverlappingEditsBeforeMutatingDocument() {
        val document = Buffer("abcdef")
        assertFalse(document.applyEdits(listOf(
            TextDocumentEdit(1, 4, "x"),
            TextDocumentEdit(3, 5, "y")
        )))
        assertEquals("abcdef", document.value)
    }
}
