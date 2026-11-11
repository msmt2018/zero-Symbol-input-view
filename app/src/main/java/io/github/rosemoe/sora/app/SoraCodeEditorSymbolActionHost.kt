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
    override fun insertText(template: String): Boolean {
        if (!editor.isEditable) return false
        editor.commitText(template)
        return true
    }

    override fun replaceSelection(text: String): Boolean {
        if (!editor.isEditable) return false
        editor.commitText(text)
        return true
    }

    override fun surroundSelection(prefix: String, suffix: String): Boolean {
        if (!editor.isEditable) return false
        editor.commitText(prefix + selectedText() + suffix)
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
            "undo" -> editor.undo()
            "redo" -> editor.redo()
            "select_all" -> editor.selectAll()
            "indent" -> editor.indentOrCommitTab()
            "unindent" -> editor.unindentSelection()
            "format" -> editor.formatCodeAsync()
            "insert_payload" -> editor.commitText(payload.orEmpty())
            else -> return false
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
