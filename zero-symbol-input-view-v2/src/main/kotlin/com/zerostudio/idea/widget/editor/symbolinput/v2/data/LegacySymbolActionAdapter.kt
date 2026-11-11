package com.zerostudio.idea.widget.editor.symbolinput.v2.data

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.ClipboardOperation
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.CursorTarget
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DeleteKind
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.LegacySymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SelectionTransform
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction

/**
 * Compatibility boundary used only while importing first-generation data.
 * Unknown legacy IDs remain explicit host extensions instead of being silently
 * discarded or becoming V2's runtime action protocol.
 */
object LegacySymbolActionAdapter {
    fun convert(legacy: LegacySymbolAction): SymbolAction {
        val payload = legacy.payload
        return when (legacy.type) {
            0 -> SymbolAction.InsertText(payload.orEmpty())
            3, 34 -> SymbolAction.Delete(DeleteKind.CURRENT_LINE)
            4 -> SymbolAction.EditorCommand("line.clear")
            7 -> SymbolAction.TransformSelection(SelectionTransform.UPPERCASE)
            8 -> SymbolAction.TransformSelection(SelectionTransform.LOWERCASE)
            9 -> SymbolAction.EditorCommand("editor.indent")
            11 -> SymbolAction.EditorCommand("editor.toggleLineComment")
            16 -> SymbolAction.MoveCursor(CursorTarget.LINE_START)
            17 -> SymbolAction.MoveCursor(CursorTarget.LINE_END)
            18 -> SymbolAction.MoveCursor(CursorTarget.LEFT)
            19 -> SymbolAction.MoveCursor(CursorTarget.RIGHT)
            20 -> SymbolAction.MoveCursor(CursorTarget.UP)
            21 -> SymbolAction.MoveCursor(CursorTarget.DOWN)
            22 -> SymbolAction.OpenManager
            23 -> SymbolAction.MoveCursor(CursorTarget.DOCUMENT_START)
            24 -> SymbolAction.MoveCursor(CursorTarget.DOCUMENT_END)
            25 -> SymbolAction.EditorCommand("selection.selectAll")
            28, 52 -> SymbolAction.Clipboard(ClipboardOperation.COPY)
            29, 53 -> SymbolAction.Clipboard(ClipboardOperation.CUT)
            30, 54 -> SymbolAction.Clipboard(ClipboardOperation.PASTE)
            32 -> SymbolAction.EditorCommand("editor.format")
            33, 35 -> SymbolAction.EditorCommand("line.duplicate")
            36 -> SymbolAction.EditorCommand("line.replace", payload)
            37 -> SymbolAction.InsertText(payload ?: "\n")
            38 -> SymbolAction.InsertText(payload ?: "\t")
            39 -> SymbolAction.EditorCommand("editor.unindent")
            in 40..51 -> SymbolAction.InsertText(payload ?: "<F${legacy.type - 39}>")
            55 -> SymbolAction.EditorCommand("history.undo")
            56 -> SymbolAction.EditorCommand("history.redo")
            else -> SymbolAction.Extension("legacy.action.${legacy.type}", payload)
        }
    }
}
