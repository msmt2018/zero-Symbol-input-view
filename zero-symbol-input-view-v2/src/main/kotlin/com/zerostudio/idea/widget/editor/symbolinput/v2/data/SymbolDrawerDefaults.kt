package com.zerostudio.idea.widget.editor.symbolinput.v2.data

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.CursorTarget
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup

/** Built-in first-run symbols used when neither V2 data nor legacy data exists. */
object SymbolDrawerDefaults {
    fun createGroups(): List<SymbolGroup> = listOf(
        SymbolGroup("common", "常用", listOf(
            entry(1, "Tab", SymbolAction.InsertText("\t")),
            entry(2, "{}", SymbolAction.InsertText("{}")),
            entry(3, "()", SymbolAction.InsertText("()")),
            entry(4, "[]", SymbolAction.InsertText("[]")),
            entry(5, "=>", SymbolAction.InsertText(" => ")),
            entry(6, "->", SymbolAction.InsertText(" -> ")),
            entry(7, ";", SymbolAction.InsertText(";")),
            entry(8, ":", SymbolAction.InsertText(":")),
            entry(9, "\"", SymbolAction.InsertText("\"")),
            entry(10, "'", SymbolAction.InsertText("'")),
            entry(11, "←", SymbolAction.MoveCursor(CursorTarget.LEFT)),
            entry(12, "→", SymbolAction.MoveCursor(CursorTarget.RIGHT)),
            entry(13, "↑", SymbolAction.MoveCursor(CursorTarget.UP)),
            entry(14, "↓", SymbolAction.MoveCursor(CursorTarget.DOWN)),
            entry(15, "Home", SymbolAction.MoveCursor(CursorTarget.LINE_START)),
            entry(16, "End", SymbolAction.MoveCursor(CursorTarget.LINE_END)),
            entry(17, "()", SymbolAction.SurroundSelection("(", ")")),
            entry(18, "[]", SymbolAction.SurroundSelection("[", "]")),
            entry(19, "{}", SymbolAction.SurroundSelection("{", "}")),
            entry(20, "...", SymbolAction.InsertText("...")),
            entry(21, "=", SymbolAction.InsertText(" = ")),
            entry(22, "==", SymbolAction.InsertText(" == ")),
            entry(23, "!=", SymbolAction.InsertText(" != ")),
            entry(24, "&&", SymbolAction.InsertText(" && ")),
            entry(25, "||", SymbolAction.InsertText(" || ")),
            entry(26, "//", SymbolAction.InsertText("// ")),
            entry(27, "/* */", SymbolAction.SurroundSelection("/* ", " */")),
            entry(28, "管理", SymbolAction.OpenManager)
        ))
    )

    private fun entry(id: Long, label: String, action: SymbolAction) =
        SymbolEntry(id = id, label = label, shortAction = action)
}
