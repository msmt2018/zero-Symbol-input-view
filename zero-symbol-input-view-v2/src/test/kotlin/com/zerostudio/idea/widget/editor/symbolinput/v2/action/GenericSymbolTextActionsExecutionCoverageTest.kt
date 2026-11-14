package com.zerostudio.idea.widget.editor.symbolinput.v2.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Executes every production generic handler against a mutable document adapter. */
class GenericSymbolTextActionsExecutionCoverageTest {

    @Test
    fun everyRegisteredGenericActionIsInvokedAndReturnsAResult() {
        val registry = GenericSymbolTextActions.createRegistry()
        val ids = GenericSymbolTextActions.supportedCommandIds()
        assertEquals(
            "Every generic command must have a catalog entry",
            ids,
            BuiltinSymbolActionCatalog.options.map { it.commandId }.filter { it in ids }.toSet()
        )

        ids.sorted().forEach { commandId ->
            val document = TestDocument(
                text = "  alpha beta  \r\nsecond line\nthird line",
                cursor = 8,
                selectionStart = 2,
                selectionEnd = 7
            )
            val payload = when (commandId) {
                "line.replace" -> "replacement"
                "line.toggleComment", "editor.toggleLineComment", "selection.comment",
                "document.toggleComment" -> "//"
                "line.indent", "selection.indentLines" -> "  "
                "selection.wrapHtmlTag" -> "span"
                "document.ensureFinalNewline" -> "\r\n"
                else -> null
            }

            val result = try {
                registry.execute(SymbolActionId("builtin", commandId), document, payload)
            } catch (error: Throwable) {
                throw AssertionError(
                    "Built-in action '${commandId}' threw. " +
                        "Input text='${document.textBeforeInvocation}', cursor=8, selection=[2,7), payload=${payload}",
                    error
                )
            }

            assertNotNull("Built-in action '${commandId}' was not registered", result)
            assertTrue(
                "Built-in action '${commandId}' corrupted document bounds: " +
                    "text='${document.text}', cursor=${document.cursorOffset}, " +
                    "selection=[${document.selectionStart},${document.selectionEnd})",
                document.hasValidState()
            )
        }
    }

    @Test
    fun representativeActionsMustProduceTheirDocumentSideEffects() {
        assertAction("line.replace", "replacement", "  replacement  \r\nsecond line\nthird line")
        assertAction("selection.wrapParentheses", null, "  (alpha) beta  \r\nsecond line\nthird line")
        assertAction("selection.uppercase", null, "  ALPHA beta  \r\nsecond line\nthird line")
        assertAction(
            "document.ensureFinalNewline",
            "\r\n",
            "  alpha beta  \r\nsecond line\nthird line\r\n"
        )
        assertAction("cursor.documentStart", null, "  alpha beta  \r\nsecond line\nthird line", expectedCursor = 0)
        assertAction(
            "document.selectAll",
            null,
            "  alpha beta  \r\nsecond line\nthird line",
            expectedSelection = 0 to "  alpha beta  \r\nsecond line\nthird line".length
        )
    }

    @Test
    fun readOnlyDocumentsRejectMutatingActionsWithoutChangingText() {
        val registry = GenericSymbolTextActions.createRegistry()
        val document = TestDocument("alpha\nbeta", 2, 0, 5, isEditable = false)
        val before = document.text
        val result = registry.execute(SymbolActionId("builtin", "selection.wrapBraces"), document)
        assertEquals(false, result)
        assertEquals(before, document.text)
        assertTrue(document.hasValidState())
    }

    @Test
    fun invalidHtmlTagPayloadIsRejectedWithoutMutatingTheDocument() {
        val registry = GenericSymbolTextActions.createRegistry()
        val document = TestDocument("alpha", 2, 0, 5)
        val before = document.text
        val result = registry.execute(SymbolActionId("builtin", "selection.wrapHtmlTag"), document, "../script")
        assertEquals(false, result)
        assertEquals(before, document.text)
    }

    private fun assertAction(
        commandId: String,
        payload: String?,
        expectedText: String,
        expectedCursor: Int? = null,
        expectedSelection: Pair<Int, Int>? = null
    ) {
        val registry = GenericSymbolTextActions.createRegistry()
        val document = TestDocument(
            "  alpha beta  \r\nsecond line\nthird line",
            8,
            2,
            7
        )
        assertEquals(
            "Action '${commandId}' should report successful execution",
            true,
            registry.execute(SymbolActionId("builtin", commandId), document, payload)
        )
        assertEquals("Unexpected text after '${commandId}': expected=<${expectedText}> actual=<${document.text}>", expectedText, document.text)
        expectedCursor?.let { assertEquals("Unexpected cursor after '${commandId}'", it, document.cursorOffset) }
        expectedSelection?.let {
            assertEquals(it.first, document.selectionStart)
            assertEquals(it.second, document.selectionEnd)
        }
        assertTrue("Invalid selection after '${commandId}'", document.hasValidState())
    }

    private class TestDocument(
        text: String,
        cursor: Int,
        selectionStart: Int,
        selectionEnd: Int,
        override val isEditable: Boolean = true
    ) : SymbolTextDocument {
        var text: String = text
            private set
        val textBeforeInvocation: String = text
        override var cursorOffset: Int = cursor
            private set
        override var selectionStart: Int = selectionStart
            private set
        override var selectionEnd: Int = selectionEnd
            private set
        override val textLength: Int get() = text.length
        override val lineCount: Int get() = lineStarts().size

        override fun lineForOffset(offset: Int): Int {
            val safe = offset.coerceIn(0, text.length)
            return lineStarts().indexOfLast { it <= safe }.coerceAtLeast(0)
        }

        override fun lineStartOffset(line: Int): Int =
            lineStarts().getOrElse(line) { throw IndexOutOfBoundsException("line=${line}") }

        override fun lineEndOffset(line: Int): Int {
            var end = lineStartOffset(line)
            while (end < text.length && text[end] != '\r' && text[end] != '\n') end++
            return end
        }

        override fun lineText(line: Int): String = text.substring(lineStartOffset(line), lineEndOffset(line))

        override fun textBetween(start: Int, end: Int): String {
            require(start in 0..text.length && end in start..text.length)
            return text.substring(start, end)
        }

        override fun charAt(offset: Int): Char = text[offset]

        override fun replace(start: Int, end: Int, replacement: String): Boolean {
            if (!isEditable || start !in 0..text.length || end !in start..text.length) return false
            text = text.substring(0, start) + replacement + text.substring(end)
            val delta = replacement.length - (end - start)
            cursorOffset = adjustOffset(cursorOffset, start, end, delta, replacement.length)
            selectionStart = adjustOffset(selectionStart, start, end, delta, replacement.length)
            selectionEnd = adjustOffset(selectionEnd, start, end, delta, replacement.length)
            return true
        }

        override fun setSelection(start: Int, end: Int) {
            require(start in 0..text.length && end in 0..text.length)
            selectionStart = minOf(start, end)
            selectionEnd = maxOf(start, end)
            cursorOffset = end
        }

        fun hasValidState(): Boolean =
            cursorOffset in 0..text.length &&
                selectionStart in 0..text.length &&
                selectionEnd in selectionStart..text.length &&
                lineCount >= 1 &&
                (0 until lineCount).all { line ->
                    lineStartOffset(line) <= lineEndOffset(line) &&
                        lineEndOffset(line) <= text.length
                }

        private fun lineStarts(): List<Int> {
            val starts = ArrayList<Int>()
            starts += 0
            var index = 0
            while (index < text.length) {
                when (text[index]) {
                    '\r' -> {
                        index += if (index + 1 < text.length && text[index + 1] == '\n') 2 else 1
                        starts += index
                    }
                    '\n' -> {
                        index++
                        starts += index
                    }
                    else -> index++
                }
            }
            return starts
        }

        private fun adjustOffset(
            value: Int,
            start: Int,
            end: Int,
            delta: Int,
            replacementLength: Int
        ): Int = when {
            value < start -> value
            value >= end -> (value + delta).coerceIn(0, text.length)
            else -> (start + replacementLength).coerceIn(0, text.length)
        }
    }
}
