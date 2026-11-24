/*
 * This file is part of zero-Symbol-input-view-V2.
 *
 * Copyright (C) 2026 android_zero
 *
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 *
 * License Information:
 *
 * This file is distributed under the following licenses, which apply
 * cumulatively to the same work:
 *
 * 1. GNU Affero General Public License v3.0 only (AGPL-3.0-only):
 *    Grants the rights to use, study, modify, and redistribute this work.
 *    When a modified version is used to provide network services to users,
 *    the corresponding source code must be made available under the
 *    applicable AGPL terms.
 *
 * 2. GNU General Public License v3.0 only (GPL-3.0-only):
 *    Grants the rights to use, study, modify, and redistribute this work.
 *    Distribution of covered binaries requires compliance with the source
 *    code, license notice, attribution, and other applicable GPL terms.
 *
 * 3. Apache License 2.0 (Apache-2.0):
 *    Grants copyright and applicable patent rights to use, modify, and
 *    distribute this work, subject to its license conditions, including
 *    preservation of notices, inclusion of the license, and marking changes.
 *
 * The SPDX operator "AND" means that all listed license terms apply
 * cumulatively. It does not mean that recipients may choose only one
 * of the listed licenses.
 *
 * You must comply with the applicable terms of all three licenses.
 * This notice is a summary and does not replace the full license texts.
 *
 * Full license texts:
 * - GNU AGPL v3.0: https://www.gnu.org/licenses/agpl-3.0.html
 * - GNU GPL v3.0:  https://www.gnu.org/licenses/gpl-3.0.html
 * - Apache 2.0:    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Author: android_zero
 * GitHub: https://github.com/msmt2018
 * WeChat: android_zero
 */
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

    @Test fun lineActionsPreserveCrLfSeparators() {
        val duplicate = FakeDocument("one\r\ntwo", cursor = 6)
        assertTrue(run("line.duplicate", duplicate))
        assertEquals("one\r\ntwo\r\ntwo", duplicate.text)

        val above = FakeDocument("one\r\ntwo", cursor = 6)
        assertTrue(run("line.insertAbove", above))
        assertEquals("one\r\n\r\ntwo", above.text)

        val below = FakeDocument("one\r\ntwo", cursor = 1)
        assertTrue(run("line.insertBelow", below))
        assertEquals("one\r\n\r\ntwo", below.text)

        val joined = FakeDocument("one\r\ntwo", cursor = 1)
        assertTrue(run("line.joinNext", joined))
        assertEquals("onetwo", joined.text)

        val deleted = FakeDocument("one\r\ntwo", cursor = 6)
        assertTrue(run("line.delete", deleted))
        assertEquals("one", deleted.text)

        val movedUp = FakeDocument("one\r\ntwo\r\nthree", cursor = 6)
        assertTrue(run("line.moveUp", movedUp))
        assertEquals("two\r\none\r\nthree", movedUp.text)

        val movedDown = FakeDocument("one\r\ntwo\r\nthree", cursor = 1)
        assertTrue(run("line.moveDown", movedDown))
        assertEquals("two\r\none\r\nthree", movedDown.text)
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


    @Test fun newLineAndWhitespaceActionsAreEditorIndependent() {
        val duplicate = FakeDocument("  alpha\nbeta", cursor = 4)
        assertTrue(run("line.duplicateAbove", duplicate))
        assertEquals("  alpha\n  alpha\nbeta", duplicate.text)

        val trim = FakeDocument("    alpha", cursor = 7)
        assertTrue(run("line.trimLeadingWhitespace", trim))
        assertEquals("alpha", trim.text)
        assertEquals(3, trim.cursorOffset)

        val document = FakeDocument("  alpha\n    beta", cursor = 10)
        assertTrue(run("document.trimLeadingWhitespace", document))
        assertEquals("alpha\nbeta", document.text)
        assertEquals(6, document.cursorOffset)
    }

    @Test fun wrappersAndCursorExtensionActionsWork() {
        val quote = FakeDocument("hello", cursor = 5).apply { setSelection(0, 5) }
        assertTrue(run("selection.wrapSingleQuotes", quote))
        assertEquals("'hello'", quote.text)
        assertEquals(1, quote.selectionStart)
        assertEquals(6, quote.selectionEnd)

        val tag = FakeDocument("value", cursor = 5).apply { setSelection(0, 5) }
        assertTrue(GenericSymbolTextActions.createRegistry().execute(
            SymbolActionId("builtin", "selection.wrapHtmlTag"), tag, "span"
        )!!)
        assertEquals("<span>value</span>", tag.text)
        val invalidTag = FakeDocument("x").apply { setSelection(0, 1) }
        assertFalse(GenericSymbolTextActions.createRegistry().execute(
            SymbolActionId("builtin", "selection.wrapHtmlTag"), invalidTag, "bad tag"
        )!!)

        val cursor = FakeDocument("alpha\nbeta", cursor = 7)
        assertTrue(run("cursor.selectToLineStart", cursor))
        assertEquals(6, cursor.selectionStart)
        assertEquals(7, cursor.selectionEnd)
        assertTrue(run("cursor.selectToDocumentStart", cursor))
        assertEquals(0, cursor.selectionStart)
        assertEquals(7, cursor.selectionEnd)
    }

    @Test fun selectedLineOperationsAndFinalNewlineAreStable() {
        val indented = FakeDocument("one\ntwo\nthree", cursor = 0).apply { setSelection(0, 7) }
        assertTrue(run("selection.indentLines", indented))
        assertEquals("\tone\n\ttwo\nthree", indented.text)
        assertTrue(run("selection.unindentLines", indented))
        assertEquals("one\ntwo\nthree", indented.text)

        val reversed = FakeDocument("c\na\nb", cursor = 0).apply { setSelection(0, 5) }
        assertTrue(run("selection.reverseLines", reversed))
        assertEquals("b\na\nc", reversed.text)

        val finalNewline = FakeDocument("alpha", cursor = 5)
        assertTrue(GenericSymbolTextActions.createRegistry().execute(
            SymbolActionId("builtin", "document.ensureFinalNewline"), finalNewline
        )!!)
        assertEquals("alpha\n", finalNewline.text)
        assertFalse(run("document.ensureFinalNewline", finalNewline))
    }

    @Test fun supplementaryUnicodeWordsRemainWholeDuringNavigationAndDeletion() {
        val value = "a𐐀b x"

        val selected = FakeDocument(value, cursor = 2)
        assertTrue(run("cursor.selectWord", selected))
        assertEquals("a𐐀b", selected.text.substring(selected.selectionStart, selected.selectionEnd))
        assertEquals(0, selected.selectionStart)
        assertEquals(4, selected.selectionEnd)

        val left = FakeDocument(value, cursor = 4)
        assertTrue(run("cursor.wordLeft", left))
        assertEquals(0, left.cursorOffset)

        val right = FakeDocument(value, cursor = 0)
        assertTrue(run("cursor.wordRight", right))
        assertEquals(4, right.cursorOffset)

        val backward = FakeDocument(value, cursor = 4)
        assertTrue(run("cursor.deleteWordBackward", backward))
        assertEquals(" x", backward.text)

        val forward = FakeDocument(value, cursor = 0)
        assertTrue(run("cursor.deleteWordForward", forward))
        assertEquals(" x", forward.text)
    }

    @Test fun toggleCaseHandlesSupplementaryPlaneLetters() {
        val document = FakeDocument("𐐀x", cursor = 2).apply { setSelection(0, 2) }
        assertTrue(run("selection.toggleCase", document))
        assertEquals("𐐨x", document.text)
        assertEquals(0, document.selectionStart)
        assertEquals(2, document.selectionEnd)
    }


    @Test fun selectionActionsNeverSplitSupplementaryCodePoints() {
        val source = "a𐐀b"

        val deleted = FakeDocument(source).apply { setSelection(2, 3) }
        assertTrue(run("selection.delete", deleted))
        assertEquals("ab", deleted.text)

        val duplicated = FakeDocument(source).apply { setSelection(2, 3) }
        assertTrue(run("selection.duplicate", duplicated))
        assertEquals("a𐐀𐐀b", duplicated.text)

        val wrapped = FakeDocument(source).apply { setSelection(2, 3) }
        assertTrue(run("selection.wrapParentheses", wrapped))
        assertEquals("a(𐐀)b", wrapped.text)
        assertEquals(2, wrapped.selectionStart)
        assertEquals(4, wrapped.selectionEnd)

        val payload = FakeDocument(source).apply { setSelection(2, 3) }
        assertTrue(GenericSymbolTextActions.createRegistry().execute(
            SymbolActionId("builtin", "insert_payload"), payload, "X"
        )!!)
        assertEquals("aXb", payload.text)
    }

    @Test
    fun removeBlankLinesBatchesMiddleAndTrailingRunsWithoutBreakingSeparators() {
        val lf = FakeDocument("alpha\n\n\nbeta\n\n", cursor = 4)
        assertTrue(run("document.removeBlankLines", lf))
        assertEquals("alpha\nbeta", lf.text)
        assertEquals(6, lf.cursorOffset)

        val crlf = FakeDocument("alpha\r\n\r\nbeta\r\n", cursor = 2)
        assertTrue(run("document.removeBlankLines", crlf))
        assertEquals("alpha\r\nbeta", crlf.text)
        assertEquals(7, crlf.cursorOffset)

        val allBlank = FakeDocument("\n\n", cursor = 1)
        assertTrue(run("document.removeBlankLines", allBlank))
        assertEquals("", allBlank.text)

        val singleBlank = FakeDocument("   ")
        assertFalse(run("document.removeBlankLines", singleBlank))
        assertEquals("   ", singleBlank.text)
    }

    @Test
    fun removeBlankLinesRollsBackWhenASeparatedRunIsRejected() {
        val document = FakeDocument(
            "alpha\n\nbeta\n\ngamma",
            cursor = 5,
            failOnReplaceCall = 2
        ).apply { setSelection(1, 14) }

        assertFalse(run("document.removeBlankLines", document))
        assertEquals("alpha\n\nbeta\n\ngamma", document.text)
        assertEquals(1, document.selectionStart)
        assertEquals(14, document.selectionEnd)
    }

    @Test
    fun multiLineCommandsRollBackWhenAnEditIsRejected() {
        val comment = FakeDocument(
            "alpha\nbeta",
            cursor = 3,
            failOnReplaceCall = 2
        ).apply { setSelection(0, 10) }
        assertFalse(run("selection.comment", comment))
        assertEquals("alpha\nbeta", comment.text)
        assertEquals(0, comment.selectionStart)
        assertEquals(10, comment.selectionEnd)

        val indented = FakeDocument(
            "one\ntwo\nthree",
            cursor = 0,
            failOnReplaceCall = 2
        ).apply { setSelection(0, 7) }
        assertFalse(run("selection.indentLines", indented))
        assertEquals("one\ntwo\nthree", indented.text)
        assertEquals(0, indented.selectionStart)
        assertEquals(7, indented.selectionEnd)

        val reordered = FakeDocument(
            "c\na\nb",
            cursor = 0,
            failOnReplaceCall = 2
        ).apply { setSelection(0, 5) }
        assertFalse(run("selection.reverseLines", reordered))
        assertEquals("c\na\nb", reordered.text)
        assertEquals(0, reordered.selectionStart)
        assertEquals(5, reordered.selectionEnd)
    }

    private fun run(id: String, document: FakeDocument): Boolean =
        GenericSymbolTextActions.createRegistry().execute(
            SymbolActionId("builtin", id), document
        ) ?: false

    private class FakeDocument(
        var text: String,
        cursor: Int = 0,
        override val isEditable: Boolean = true,
        private val failOnReplaceCall: Int? = null
    ) : SymbolTextDocument {
        private var replaceCalls = 0
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
            return if (newline < 0) text.length
            else if (newline > start && text[newline - 1] == '\r') newline - 1
            else newline
        }

        override fun lineText(line: Int): String =
            text.substring(lineStartOffset(line), lineEndOffset(line))

        override fun textBetween(start: Int, end: Int): String = text.substring(start, end)
        override fun charAt(offset: Int): Char = text[offset]

        override fun replace(start: Int, end: Int, replacement: String): Boolean {
            replaceCalls++
            if (!isEditable || replaceCalls == failOnReplaceCall) return false
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
