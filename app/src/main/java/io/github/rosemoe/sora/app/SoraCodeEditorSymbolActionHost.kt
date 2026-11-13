package io.github.rosemoe.sora.app

import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.SelectionMovement
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.GenericSymbolTextActions
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionHost
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionId
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionRegistry
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolTextDocument
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolTextEditing
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.ClipboardOperation
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.CursorTarget
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DeleteKind
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SelectionTransform

/** Host callback for application-registered symbol action extensions. */
fun interface SymbolActionExtensionHandler {
    fun execute(payload: String?): Boolean
}

/** Sora CodeEditor adapter for the V2 typed symbol-action contract. */
class SoraCodeEditorSymbolActionHost(
    private val editor: CodeEditor,
    private val onOpenManager: () -> Unit
) : SymbolActionHost {
    private val extensionHandlers = SymbolActionRegistry<SoraCodeEditorSymbolActionHost>()
    private val extensionRegistrations = LinkedHashMap<String, AutoCloseable>()
    private val genericTextActions = GenericSymbolTextActions.createRegistry()

    /** Register an app-defined extension action. Returns false for a blank ID. */
    fun registerExtensionHandler(
        extensionId: String,
        handler: SymbolActionExtensionHandler
    ): Boolean {
        if (extensionId.isBlank()) return false
        extensionRegistrations.remove(extensionId)?.close()
        extensionRegistrations[extensionId] = extensionHandlers.register(
            SymbolActionId.extension(extensionId)
        ) { _, payload -> handler.execute(payload) }
        return true
    }

    fun unregisterExtensionHandler(extensionId: String): Boolean =
        extensionRegistrations.remove(extensionId)?.let {
            it.close()
            true
        } ?: false
    /**
     * V1-compatible insertion macros: $S = insertion cursor, $E = selection end,
     * $T = selected text and $ = a literal dollar sign.
     */
    override fun insertText(template: String): Boolean {
        if (!editor.isEditable) return false
        val expansion = SymbolTextEditing.expandMacro(template, selectedText())
        val output = expansion.text
        val insertionCursor = expansion.insertionOffset
        val selectionEnd = expansion.selectionEndOffset
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
        if (kind == DeleteKind.CURRENT_LINE) {
            return genericTextActions.execute(
                SymbolActionId("builtin", "line.delete"),
                SoraSymbolTextDocument(editor)
            ) ?: false
        }
        if (start == end) {
            when (kind) {
                DeleteKind.BACKWARD -> {
                    val range = SymbolTextEditing.codePointDeletionRange(text, start, backwards = true)
                        ?: return false
                    start = range.start
                    end = range.end
                }
                DeleteKind.FORWARD -> {
                    val range = SymbolTextEditing.codePointDeletionRange(text, end, backwards = false)
                        ?: return false
                    start = range.start
                    end = range.end
                }
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
        val document = SoraSymbolTextDocument(editor)
        genericTextActions.execute(SymbolActionId("builtin", commandId), document, payload)?.let {
            return it
        }
        val readOnlySafeCommands = setOf(
            "select_all", "selection.selectAll", "copy",
            "undo", "history.undo", "redo", "history.redo"
        )
        if (!editor.isEditable && commandId !in readOnlySafeCommands) return false
        when (commandId) {
            "undo", "history.undo" -> editor.undo()
            "redo", "history.redo" -> editor.redo()
            "select_all", "selection.selectAll" -> editor.selectAll()
            "indent", "editor.indent" -> editor.indentOrCommitTab()
            "unindent", "editor.unindent" -> editor.unindentSelection()
            "format", "editor.format" -> editor.formatCodeAsync()
            "insert_payload" -> editor.commitText(payload.orEmpty())
            "line.cut" -> return cutCurrentLine()
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
        val insertion = SymbolTextEditing.duplicateLineInsertion(
            line = line,
            lineCount = text.lineCount,
            lineLength = text.getColumnCount(line),
            lineContent = content
        )
        text.insert(insertion.line, insertion.column, insertion.text)
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

    override fun executeExtension(extensionId: String, payload: String?): Boolean {
        genericTextActions.execute(SymbolActionId("builtin", extensionId), SoraSymbolTextDocument(editor), payload)
            ?.let { return it }
        return extensionHandlers.execute(SymbolActionId.extension(extensionId), this, payload) ?: false
    }

    private fun cutCurrentLine(): Boolean {
        if (!editor.isEditable) return false
        val line = editor.cursor.leftLine
        val endColumn = editor.text.getColumnCount(line)
        if (endColumn > 0) {
            editor.setSelectionRegion(line, 0, line, endColumn)
            editor.copyText()
        }
        return genericTextActions.execute(
            SymbolActionId("builtin", "line.delete"),
            SoraSymbolTextDocument(editor)
        ) ?: false
    }

    private class SoraSymbolTextDocument(
        private val editor: CodeEditor
    ) : SymbolTextDocument {
        private val content get() = editor.text

        override val isEditable: Boolean get() = editor.isEditable
        override val textLength: Int get() = content.length
        override val lineCount: Int get() = content.lineCount
        override val cursorOffset: Int get() = editor.cursor.right().index
        override val selectionStart: Int get() = editor.cursor.left().index
        override val selectionEnd: Int get() = editor.cursor.right().index

        override fun lineForOffset(offset: Int): Int =
            content.indexer.getCharPosition(offset.coerceIn(0, content.length)).line

        override fun lineStartOffset(line: Int): Int =
            content.indexer.getCharPosition(line, 0).index

        override fun lineEndOffset(line: Int): Int =
            content.indexer.getCharPosition(line, content.getColumnCount(line)).index

        override fun lineText(line: Int): String = content.getLineString(line)

        override fun textBetween(start: Int, end: Int): String =
            content.subSequence(start, end).toString()

        override fun charAt(offset: Int): Char =
            content.subSequence(offset, offset + 1)[0]

        override fun replace(start: Int, end: Int, replacement: String): Boolean {
            if (!editor.isEditable) return false
            val safeStart = start.coerceIn(0, content.length)
            val safeEnd = end.coerceIn(safeStart, content.length)
            val from = content.indexer.getCharPosition(safeStart)
            val to = content.indexer.getCharPosition(safeEnd)
            content.beginBatchEdit()
            try {
                if (safeStart < safeEnd) content.delete(from.line, from.column, to.line, to.column)
                if (replacement.isNotEmpty()) {
                    val insertion = content.indexer.getCharPosition(safeStart)
                    content.insert(insertion.line, insertion.column, replacement)
                }
            } finally {
                content.endBatchEdit()
            }
            return true
        }

        override fun setSelection(start: Int, end: Int) {
            val safeStart = start.coerceIn(0, content.length)
            val safeEnd = end.coerceIn(0, content.length)
            val from = content.indexer.getCharPosition(safeStart)
            val to = content.indexer.getCharPosition(safeEnd)
            editor.setSelectionRegion(from.line, from.column, to.line, to.column)
        }
    }

    private fun selectedText(): String =
        if (editor.cursor.isSelected) editor.text.subSequence(
            editor.cursor.left().index, editor.cursor.right().index
        ).toString() else ""
}
