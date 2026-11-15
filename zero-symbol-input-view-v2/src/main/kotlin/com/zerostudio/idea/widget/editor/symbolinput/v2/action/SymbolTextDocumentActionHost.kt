/*
 * This file is part of ZeroStudio.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.action

import com.zerostudio.idea.widget.drawer.action.SymbolTextDocument as PublicSymbolTextDocument

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.ClipboardOperation
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.CursorTarget
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DeleteKind
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SelectionTransform

/** Optional host bridge for clipboard operations on an editor-neutral document. */
fun interface SymbolTextClipboard {
    fun perform(document: PublicSymbolTextDocument, operation: ClipboardOperation): Boolean
}

/**
 * Default SymbolActionHost implementation backed only by PublicSymbolTextDocument.
 *
 * An editor adapter implements PublicSymbolTextDocument once; this class supplies
 * common insert/selection/cursor/delete/transform/editor-command behavior without
 * importing any editor-core types. Clipboard and manager navigation are
 * intentionally injected because they belong to the host application.
 */
class SymbolTextDocumentActionHost(
    private val document: PublicSymbolTextDocument,
    private val clipboard: SymbolTextClipboard = SymbolTextClipboard { _, _ -> false },
    private val openManagerAction: () -> Boolean = { false },
    private val extensionAction: (String, String?) -> Boolean = { _, _ -> false }
) : SymbolActionHost {

    private val genericCommands = GenericSymbolTextActions.createRegistry()

    override fun insertText(template: String): Boolean {
        if (!document.isEditable) return false
        val start = minOf(document.selectionStart, document.selectionEnd).coerceIn(0, document.textLength)
        val end = maxOf(document.selectionStart, document.selectionEnd).coerceIn(start, document.textLength)
        val selected = document.textBetween(start, end)
        val expansion = SymbolTextEditing.expandMacro(template, selected)
        if (!document.replace(start, end, expansion.text)) return false
        document.setSelection(
            start + expansion.insertionOffset,
            start + expansion.selectionEndOffset
        )
        return true
    }

    override fun replaceSelection(text: String): Boolean {
        if (!document.isEditable) return false
        val start = minOf(document.selectionStart, document.selectionEnd).coerceIn(0, document.textLength)
        val end = maxOf(document.selectionStart, document.selectionEnd).coerceIn(start, document.textLength)
        if (!document.replace(start, end, text)) return false
        val caret = start + text.length
        document.setSelection(caret, caret)
        return true
    }

    override fun surroundSelection(prefix: String, suffix: String): Boolean {
        if (!document.isEditable) return false
        val start = minOf(document.selectionStart, document.selectionEnd).coerceIn(0, document.textLength)
        val end = maxOf(document.selectionStart, document.selectionEnd).coerceIn(start, document.textLength)
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
                (targetStart + (cursor - lineStart)).coerceAtMost(targetEnd)
            }
        }
        document.setSelection(targetOffset, targetOffset)
        return true
    }

    override fun delete(kind: DeleteKind): Boolean {
        if (!document.isEditable) return false
        if (document.lineCount <= 0) return false
        val length = document.textLength
        val selectionStart = minOf(document.selectionStart, document.selectionEnd).coerceIn(0, length)
        val selectionEnd = maxOf(document.selectionStart, document.selectionEnd).coerceIn(selectionStart, length)
        val cursor = document.cursorOffset.coerceIn(0, length)
        val range = when {
            selectionStart != selectionEnd -> SymbolTextRange(selectionStart, selectionEnd)
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
                        } else if (line > 0) end else end
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
        val start = minOf(document.selectionStart, document.selectionEnd).coerceIn(0, document.textLength)
        val end = maxOf(document.selectionStart, document.selectionEnd).coerceIn(start, document.textLength)
        if (start == end) return false
        val selected = document.textBetween(start, end)
        val transformed = when (transform) {
            SelectionTransform.UPPERCASE -> selected.uppercase()
            SelectionTransform.LOWERCASE -> selected.lowercase()
            SelectionTransform.TOGGLE_CASE -> selected.map { char ->
                when {
                    char.isUpperCase() -> char.lowercaseChar()
                    char.isLowerCase() -> char.uppercaseChar()
                    else -> char
                }
            }.joinToString("")
        }
        if (!document.replace(start, end, transformed)) return false
        document.setSelection(start, start + transformed.length)
        return true
    }

    /**
     * Reads at most two UTF-16 code units around the caret. Cursor movement and
     * backspace/delete therefore remain O(1) and never materialize the document.
     */
    private fun adjacentCodePointRange(cursorOffset: Int, backwards: Boolean): SymbolTextRange? {
        val cursor = cursorOffset.coerceIn(0, document.textLength)
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
