package io.github.rosemoe.sora.app

import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.SelectionMovement
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionHost
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.ClipboardOperation
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.CursorTarget
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DeleteKind
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SelectionTransform

/** Sora CodeEditor adapter for the V2 typed symbol-action contract. */
class SoraCodeEditorSymbolActionHost(
    private val editor: CodeEditor,
    private val onOpenManager: () -> Unit
) : SymbolActionHost {
    /**
     * V1-compatible insertion macros: $S = insertion cursor, $E = selection end,
     * $T = selected text and $ = a literal dollar sign.
     */
    override fun insertText(template: String): Boolean {
        if (!editor.isEditable) return false
        val selected = selectedText()
        val escapedDollar = "\\u0000"
        val normalized = template.replace("$", escapedDollar)
        val startMarker = normalized.indexOf("\\$S").takeIf { it >= 0 }
        val endMarker = normalized.indexOf("\\$E").takeIf { it >= 0 }
        val output = normalized.replace("\\$S", "")
            .replace("\\$E", "")
            .replace("\\$T", selected)
            .replace(escapedDollar, "$")
        val insertionCursor = startMarker?.coerceIn(0, output.length) ?: output.length
        val selectionEnd = endMarker?.coerceIn(0, output.length) ?: insertionCursor
        editor.insertText(output, insertionCursor)
        if (selectionEnd != insertionCursor) {
            val caret = editor.cursor.left().index
            val target = (caret + selectionEnd - insertionCursor).coerceIn(0, editor.text.length)
            val from = editor.text.indexer.getCharPosition(minOf(caret, target))
            val to = editor.text.indexer.getCharPosition(maxOf(caret, target))
            editor.setSelectionRegion(from.line, from.column, to.line, to.column)
        }
        return true
    }

    override fun replaceSelection(text: String): Boolean {
        if (!editor.isEditable) return false
        editor.commitText(text)
        return true
    }

    override fun surroundSelection(prefix: String, suffix: String): Boolean {
        if (!editor.isEditable) return false
        val selectionStart = editor.cursor.left().index
        val selected = selectedText()
        editor.commitText(prefix + selected + suffix)

        // Keep the original text selected after wrapping. With an empty
        // selection, place the caret between the inserted delimiters.
        val innerStart = selectionStart + prefix.length
        val innerEnd = innerStart + selected.length
        val start = editor.text.indexer.getCharPosition(innerStart)
        val end = editor.text.indexer.getCharPosition(innerEnd)
        editor.setSelectionRegion(
            start.line,
            start.column,
            end.line,
            end.column,
            false
        )
        return true
    }

    override fun moveCursor(target: CursorTarget): Boolean {
        val movement = when (target) {
            CursorTarget.LEFT -> SelectionMovement.LEFT
            CursorTarget.RIGHT -> SelectionMovement.RIGHT
            CursorTarget.UP -> SelectionMovement.UP
            CursorTarget.DOWN -> SelectionMovement.DOWN
            CursorTarget.LINE_START -> SelectionMovement.LINE_START
            CursorTarget.LINE_END -> SelectionMovement.LINE_END
            CursorTarget.DOCUMENT_START -> SelectionMovement.TEXT_START
            CursorTarget.DOCUMENT_END -> SelectionMovement.TEXT_END
        }
        editor.moveSelection(movement)
        return true
    }

    override fun delete(kind: DeleteKind): Boolean {
        if (!editor.isEditable) return false
        val text = editor.text
        val cursor = editor.cursor
        var start = cursor.left().index
        var end = cursor.right().index
        if (kind == DeleteKind.CURRENT_LINE) return deleteCurrentLine()
        if (start == end) {
            when (kind) {
                DeleteKind.BACKWARD -> start = (start - 1).coerceAtLeast(0)
                DeleteKind.FORWARD -> end = (end + 1).coerceAtMost(text.length)
                DeleteKind.CURRENT_LINE, DeleteKind.TO_LINE_START, DeleteKind.TO_LINE_END -> {
                    val line = cursor.leftLine
                    val lineStart = text.indexer.getCharPosition(line, 0).index
                    val lineEnd = text.indexer.getCharPosition(line, text.getColumnCount(line)).index
                    when (kind) {
                        DeleteKind.CURRENT_LINE -> {
                            start = lineStart
                            end = if (line < text.lineCount - 1) lineEnd + 1 else lineEnd
                        }
                        DeleteKind.TO_LINE_START -> start = lineStart
                        DeleteKind.TO_LINE_END -> end = lineEnd
                        else -> Unit
                    }
                }
            }
        }
        if (start >= end) return false
        val from = text.indexer.getCharPosition(start)
        val to = text.indexer.getCharPosition(end)
        text.delete(from.line, from.column, to.line, to.column)
        return true
    }

    override fun transformSelection(transform: SelectionTransform): Boolean {
        if (!editor.isEditable || !editor.cursor.isSelected) return false
        val selected = selectedText()
        val replacement = when (transform) {
            SelectionTransform.UPPERCASE -> selected.uppercase()
            SelectionTransform.LOWERCASE -> selected.lowercase()
            SelectionTransform.TOGGLE_CASE -> selected.map {
                if (it.isLowerCase()) it.uppercaseChar() else it.lowercaseChar()
            }.joinToString("")
        }
        editor.commitText(replacement)
        return true
    }

    override fun executeEditorCommand(commandId: String, payload: String?): Boolean {
        if (!editor.isEditable && commandId !in setOf("select_all", "copy", "undo", "redo")) return false
        when (commandId) {
            "undo", "history.undo" -> editor.undo()
            "redo", "history.redo" -> editor.redo()
            "select_all", "selection.selectAll" -> editor.selectAll()
            "indent", "editor.indent" -> editor.indentOrCommitTab()
            "unindent", "editor.unindent" -> editor.unindentSelection()
            "format", "editor.format" -> editor.formatCodeAsync()
            "line.clear" -> return clearCurrentLine()
            "editor.toggleLineComment" -> return toggleLineComment()
            "line.duplicate" -> return duplicateCurrentLine()
            "line.replace" -> return replaceCurrentLine(payload)
            "insert_payload" -> editor.commitText(payload.orEmpty())
            else -> return false
        }
        return true
    }

    /** Clear line contents but preserve its line separator. */
    private fun clearCurrentLine(): Boolean {
        if (!editor.isEditable) return false
        val line = editor.cursor.leftLine
        editor.text.delete(line, 0, line, editor.text.getColumnCount(line))
        return true
    }

    /** Delete the current line and its separator, matching the V1 implementation. */
    private fun deleteCurrentLine(): Boolean {
        if (!editor.isEditable) return false
        val text = editor.text
        val line = editor.cursor.leftLine
        text.delete(line, 0, line, text.getColumnCount(line))
        if (line < text.lineCount - 1) {
            text.delete(line, text.getColumnCount(line), line + 1, 0)
        } else if (line > 0) {
            text.delete(line - 1, text.getColumnCount(line - 1), line, 0)
        }
        return true
    }

    private fun duplicateCurrentLine(): Boolean {
        if (!editor.isEditable) return false
        val text = editor.text
        val line = editor.cursor.leftLine
        val content = text.getLineString(line)
        val insertionLine = (line + 1).coerceAtMost(text.lineCount)
        text.insert(insertionLine, 0, content + "\\n")
        return true
    }

    private fun replaceCurrentLine(replacement: String?): Boolean {
        if (!editor.isEditable) return false
        val text = editor.text
        val line = editor.cursor.leftLine
        text.delete(line, 0, line, text.getColumnCount(line))
        text.insert(line, 0, replacement.orEmpty())
        return true
    }

    private fun toggleLineComment(): Boolean {
        if (!editor.isEditable) return false
        val text = editor.text
        val startLine = editor.cursor.leftLine
        val endLine = editor.cursor.rightLine
        val marker = "//"
        text.beginBatchEdit()
        try {
            for (line in startLine..endLine) {
                val content = text.getLineString(line)
                val trimmed = content.trimStart()
                if (trimmed.startsWith(marker)) {
                    val start = content.indexOf(marker)
                    text.delete(line, start, line, start + marker.length)
                } else {
                    text.insert(line, content.length - trimmed.length, marker)
                }
            }
        } finally {
            text.endBatchEdit()
        }
        return true
    }

    override fun performClipboard(operation: ClipboardOperation): Boolean {
        when (operation) {
            ClipboardOperation.COPY -> editor.copyText()
            ClipboardOperation.CUT -> {
                if (!editor.isEditable) return false
                editor.cutText()
            }
            ClipboardOperation.PASTE -> {
                if (!editor.isEditable) return false
                editor.pasteText()
            }
        }
        return true
    }

    override fun openManager(): Boolean {
        onOpenManager()
        return true
    }

    override fun executeExtension(extensionId: String, payload: String?): Boolean = false

    private fun selectedText(): String =
        if (editor.cursor.isSelected) editor.text.subSequence(
            editor.cursor.left().index, editor.cursor.right().index
        ).toString() else ""
}
