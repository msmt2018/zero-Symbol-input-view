/*
 * This file is part of zero-Symbol-input-view-V2.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.drawer.action

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.ClipboardOperation
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.CursorTarget
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DeleteKind
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SelectionTransform
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolTextEditing
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolTextRange
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.GenericSymbolTextActions

/** Optional host bridge for clipboard operations on an editor-neutral document. */
fun interface SymbolTextClipboard {
    fun perform(document: SymbolTextDocument, operation: ClipboardOperation): Boolean
}

/**
 * Default SymbolActionHost implementation backed only by SymbolTextDocument.
 *
 * An editor adapter implements SymbolTextDocument once; this class supplies
 * common insert/selection/cursor/delete/transform/editor-command behavior without
 * importing any editor-core types. Clipboard and manager navigation are
 * intentionally injected because they belong to the host application.
 */
class SymbolTextDocumentActionHost(
    private val document: SymbolTextDocument,
    private val clipboard: SymbolTextClipboard = SymbolTextClipboard { _, _ -> false },
    private val openManagerAction: () -> Boolean = { false },
    private val extensionAction: (String, String?) -> Boolean = { _, _ -> false }
) : SymbolActionHost {

    private val genericCommands = GenericSymbolTextActions.createRegistry()

    override fun insertText(template: String): Boolean {
        if (!document.isEditable || document.lineCount <= 0) return false
        val range = normalizedSelectionRange()
        val start = range.start
        val end = range.end
        val rawCursor = document.cursorOffset.coerceIn(0, document.textLength)
        val cursor = if (start == end) start else rawCursor

        val selected = document.textBetween(start, end)
        val line = document.lineForOffset(cursor).coerceIn(0, document.lineCount - 1)
        val lineText = document.lineText(line)
        val lineStart = document.lineStartOffset(line).coerceIn(0, document.textLength)
        val lineEnd = document.lineEndOffset(line).coerceIn(lineStart, document.textLength)
        val separator = when {
            lineEnd < document.textLength && document.charAt(lineEnd) == '\r' &&
                lineEnd + 1 < document.textLength && document.charAt(lineEnd + 1) == '\n' -> "\r\n"
            lineEnd > lineStart && lineEnd < document.textLength &&
                document.charAt(lineEnd) == '\n' &&
                document.charAt(lineEnd - 1) == '\r' -> "\r\n"
            lineEnd < document.textLength && document.charAt(lineEnd) == '\r' -> "\r"
            lineEnd < document.textLength && document.charAt(lineEnd) == '\n' -> "\n"
            lineStart >= 2 && document.charAt(lineStart - 2) == '\r' &&
                document.charAt(lineStart - 1) == '\n' -> "\r\n"
            lineStart > 0 && document.charAt(lineStart - 1) == '\r' -> "\r"
            lineStart > 0 && document.charAt(lineStart - 1) == '\n' -> "\n"
            else -> "\n"
        }
        val word = currentWordAtCursor(cursor)
        val indent = lineText.takeWhile { it == ' ' || it == '\t' }
        val expansion = SymbolTextEditing.expandMacro(
            template = template,
            selectedText = selected,
            currentLineText = lineText,
            currentWordText = word,
            lineSeparator = separator,
            currentIndent = indent
        )
        if (!document.replace(start, end, expansion.text)) return false
        document.setSelection(
            start + expansion.insertionOffset,
            start + expansion.selectionEndOffset
        )
        return true
    }

    private fun currentWordAtCursor(cursorOffset: Int): String {
        val length = document.textLength
        if (length == 0) return ""

        fun codePointAt(offset: Int): Int {
            val first = document.charAt(offset)
            if (Character.isHighSurrogate(first) && offset + 1 < length) {
                val second = document.charAt(offset + 1)
                if (Character.isLowSurrogate(second)) {
                    return Character.toCodePoint(first, second)
                }
            }
            return first.code
        }

        fun previousCodePointStart(offset: Int): Int {
            var start = offset - 1
            if (start > 0 && Character.isLowSurrogate(document.charAt(start)) &&
                Character.isHighSurrogate(document.charAt(start - 1))
            ) {
                start--
            }
            return start
        }

        // Preserve the original macro word contract (letter/digit/underscore),
        // but evaluate the full Unicode code point rather than an individual
        // UTF-16 Char so supplementary-plane letters are not split or discarded.
        fun isWord(codePoint: Int): Boolean =
            Character.isLetterOrDigit(codePoint) || codePoint == '_'.code

        var pivot = cursorOffset.coerceIn(0, length)
        // A host may place the caret between the two UTF-16 units of a
        // supplementary character. Normalize it to that code point's start.
        if (pivot > 0 && pivot < length &&
            Character.isHighSurrogate(document.charAt(pivot - 1)) &&
            Character.isLowSurrogate(document.charAt(pivot))
        ) {
            pivot--
        }

        if (pivot == length || !isWord(codePointAt(pivot))) {
            if (pivot == 0) return ""
            val previousStart = previousCodePointStart(pivot)
            if (!isWord(codePointAt(previousStart))) return ""
            pivot = previousStart
        }

        var start = pivot
        while (start > 0) {
            val previousStart = previousCodePointStart(start)
            if (!isWord(codePointAt(previousStart))) break
            start = previousStart
        }

        var end = pivot + Character.charCount(codePointAt(pivot))
        while (end < length) {
            val codePoint = codePointAt(end)
            if (!isWord(codePoint)) break
            end += Character.charCount(codePoint)
        }
        return document.textBetween(start, end)
    }

    override fun replaceSelection(text: String): Boolean {
        if (!document.isEditable) return false
        val range = normalizedSelectionRange()
        val start = range.start
        val end = range.end
        if (!document.replace(start, end, text)) return false
        val caret = start + text.length
        document.setSelection(caret, caret)
        return true
    }

    override fun surroundSelection(prefix: String, suffix: String): Boolean {
        if (!document.isEditable) return false
        val range = normalizedSelectionRange()
        val start = range.start
        val end = range.end
        if (start == end) return false
        val selected = document.textBetween(start, end)
        if (!document.replace(start, end, prefix + selected + suffix)) return false
        document.setSelection(start + prefix.length, start + prefix.length + selected.length)
        return true
    }

    override fun moveCursor(target: CursorTarget): Boolean {
        if (document.lineCount <= 0) return false
        val length = document.textLength.coerceAtLeast(0)
        val cursor = document.cursorOffset.coerceIn(0, length)
        val line = document.lineForOffset(cursor).coerceIn(0, document.lineCount - 1)
        val lineStart = document.lineStartOffset(line).coerceIn(0, length)
        val lineEnd = document.lineEndOffset(line).coerceIn(lineStart, length)
        val targetOffset = when (target) {
            CursorTarget.LEFT -> adjacentCodePointRange(cursor, backwards = true)?.start ?: cursor
            CursorTarget.RIGHT -> adjacentCodePointRange(cursor, backwards = false)?.end ?: cursor
            CursorTarget.LINE_START -> lineStart
            CursorTarget.LINE_END -> lineEnd
            CursorTarget.DOCUMENT_START -> 0
            CursorTarget.DOCUMENT_END -> length
            CursorTarget.UP, CursorTarget.DOWN -> {
                val targetLine = (line + if (target == CursorTarget.UP) -1 else 1)
                    .coerceIn(0, document.lineCount - 1)
                val targetStart = document.lineStartOffset(targetLine).coerceIn(0, length)
                val targetEnd = document.lineEndOffset(targetLine).coerceIn(targetStart, length)
                normalizeCodePointBoundary(
                    (targetStart + (cursor - lineStart)).coerceAtMost(targetEnd),
                    preferForward = false
                )
            }
        }
        document.setSelection(targetOffset, targetOffset)
        return true
    }

    override fun delete(kind: DeleteKind): Boolean {
        if (!document.isEditable) return false
        if (document.lineCount <= 0) return false
        val length = document.textLength
        val selection = normalizedSelectionRange()
        val selectionStart = selection.start
        val selectionEnd = selection.end
        val cursor = document.cursorOffset.coerceIn(0, length)
        val range = when {
            selectionStart != selectionEnd -> SymbolTextRange(selection.start, selection.end)
            kind == DeleteKind.BACKWARD -> adjacentCodePointRange(cursor, backwards = true)
            kind == DeleteKind.FORWARD -> adjacentCodePointRange(cursor, backwards = false)
            else -> {
                val line = document.lineForOffset(cursor).coerceIn(0, document.lineCount - 1)
                val start = document.lineStartOffset(line).coerceIn(0, length)
                val end = document.lineEndOffset(line).coerceIn(start, length)
                when (kind) {
                    DeleteKind.CURRENT_LINE -> {
                        val deleteEnd = if (line < document.lineCount - 1) {
                            document.lineStartOffset(line + 1).coerceIn(end, length)
                        } else end
                        val previousSeparatorLength = if (line > 0 && start >= 2 &&
                            document.textBetween(start - 2, start) == "\r\n"
                        ) 2 else 1
                        val deleteStart = if (line == document.lineCount - 1 && line > 0) {
                            (start - previousSeparatorLength).coerceAtLeast(0)
                        } else start
                        SymbolTextRange(deleteStart, deleteEnd)
                    }
                    DeleteKind.TO_LINE_START -> SymbolTextRange(start, cursor)
                    DeleteKind.TO_LINE_END -> SymbolTextRange(cursor, end)
                    else -> null
                }
            }
        } ?: return false
        if (range.start == range.end) return false
        if (!document.replace(range.start, range.end, "")) return false
        document.setSelection(range.start, range.start)
        return true
    }

    override fun transformSelection(transform: SelectionTransform): Boolean {
        if (!document.isEditable) return false
        val range = normalizedSelectionRange()
        val start = range.start
        val end = range.end
        if (start == end) return false
        val selected = document.textBetween(start, end)
        val transformed = when (transform) {
            SelectionTransform.UPPERCASE -> selected.uppercase()
            SelectionTransform.LOWERCASE -> selected.lowercase()
            SelectionTransform.TOGGLE_CASE -> buildString {
                var offset = 0
                while (offset < selected.length) {
                    val codePoint = selected.codePointAt(offset)
                    val toggled = when {
                        Character.isUpperCase(codePoint) -> Character.toLowerCase(codePoint)
                        Character.isLowerCase(codePoint) -> Character.toUpperCase(codePoint)
                        else -> codePoint
                    }
                    appendCodePoint(toggled)
                    offset += Character.charCount(codePoint)
                }
            }
        }
        if (!document.replace(start, end, transformed)) return false
        document.setSelection(start, start + transformed.length)
        return true
    }

    /**
     * Expands non-collapsed selection endpoints that split a surrogate pair.
     * A collapsed caret inside a pair is moved to its start, preserving the
     * established insertion semantics without replacing the character.
     */
    private fun normalizedSelectionRange(): TextDocumentRange =
        document.codePointSafeSelectionRange()

    /**
     * Reads at most two UTF-16 code units around the caret. Cursor movement and
     * backspace/delete therefore remain O(1) and never materialize the document.
     */
    private fun normalizeCodePointBoundary(offset: Int, preferForward: Boolean): Int {
        val safe = offset.coerceIn(0, document.textLength)
        if (safe <= 0 || safe >= document.textLength) return safe
        val splitsPair = Character.isHighSurrogate(document.charAt(safe - 1)) &&
            Character.isLowSurrogate(document.charAt(safe))
        if (!splitsPair) return safe
        return if (preferForward) safe + 1 else safe - 1
    }

    private fun adjacentCodePointRange(cursorOffset: Int, backwards: Boolean): SymbolTextRange? {
        val cursor = cursorOffset.coerceIn(0, document.textLength)
        // Repair an invalid caret in the middle of a surrogate pair as one
        // complete code point. Both backspace and forward-delete must never
        // remove only one UTF-16 unit.
        if (cursor > 0 && cursor < document.textLength &&
            Character.isHighSurrogate(document.charAt(cursor - 1)) &&
            Character.isLowSurrogate(document.charAt(cursor))
        ) {
            return SymbolTextRange(cursor - 1, cursor + 1)
        }
        if (backwards) {
            if (cursor == 0) return null
            var start = cursor - 1
            if (Character.isLowSurrogate(document.charAt(start)) &&
                start > 0 && Character.isHighSurrogate(document.charAt(start - 1))
            ) start--
            return SymbolTextRange(start, cursor)
        }
        if (cursor >= document.textLength) return null
        var end = cursor + 1
        if (Character.isHighSurrogate(document.charAt(cursor)) &&
            end < document.textLength && Character.isLowSurrogate(document.charAt(end))
        ) end++
        return SymbolTextRange(cursor, end)
    }

    override fun executeEditorCommand(commandId: String, payload: String?): Boolean =
        genericCommands.execute(SymbolActionId("builtin", commandId), document, payload) == true

    override fun performClipboard(operation: ClipboardOperation): Boolean =
        clipboard.perform(document, operation)

    override fun openManager(): Boolean = openManagerAction()

    override fun executeExtension(extensionId: String, payload: String?): Boolean =
        extensionAction(extensionId, payload)
}
