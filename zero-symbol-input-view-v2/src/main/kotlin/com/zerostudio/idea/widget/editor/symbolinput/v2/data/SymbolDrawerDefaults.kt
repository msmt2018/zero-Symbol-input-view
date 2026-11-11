package com.zerostudio.idea.widget.editor.symbolinput.v2.data

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.CursorTarget
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup

/**
 * Full first-run symbol set, ported from the V1 fallback configuration.
 *
 * Keep the old short/long action semantics here: arrow long-press moves to the
 * corresponding line/document boundary, paired delimiters retain their paired
 * long-press insertion, and the manager entry remains a dedicated V2 action.
 */
object SymbolDrawerDefaults {
    fun createGroups(): List<SymbolGroup> = listOf(
        SymbolGroup("commonlyUsed", "常用", listOf(
            entry(1, "←", SymbolAction.MoveCursor(CursorTarget.LEFT),
                SymbolAction.MoveCursor(CursorTarget.LINE_START)),
            entry(2, "↑", SymbolAction.MoveCursor(CursorTarget.UP),
                SymbolAction.MoveCursor(CursorTarget.DOCUMENT_START)),
            entry(3, "→", SymbolAction.MoveCursor(CursorTarget.RIGHT),
                SymbolAction.MoveCursor(CursorTarget.LINE_END)),
            entry(4, "\"", SymbolAction.InsertText("\"")),
            entry(5, "'", SymbolAction.InsertText("'")),
            entry(6, ".", SymbolAction.InsertText(".")),
            entry(7, ",", SymbolAction.InsertText(",")),
            entry(8, "/", SymbolAction.InsertText("/")),
            entry(9, "//", SymbolAction.InsertText("//")),
            entry(10, ":", SymbolAction.InsertText(":")),
            entry(11, ";", SymbolAction.InsertText(";")),
            entry(12, "↓", SymbolAction.MoveCursor(CursorTarget.DOWN),
                SymbolAction.MoveCursor(CursorTarget.DOCUMENT_END)),
            entry(13, "#", SymbolAction.InsertText("#")),
            entry(14, "+", SymbolAction.InsertText("+")),
            entry(15, "-", SymbolAction.InsertText("-")),
            entry(16, "*", SymbolAction.InsertText("*")),
            entry(17, "=", SymbolAction.InsertText("=")),
            entry(18, "|", SymbolAction.InsertText("|")),
            entry(19, "~", SymbolAction.InsertText("~")),
            entry(20, "(", SymbolAction.InsertText("(")),
            entry(21, ")", SymbolAction.InsertText(")")),
            entry(22, "(\"", SymbolAction.InsertText("(\"")),
            entry(23, "\")", SymbolAction.InsertText("\")")),
            entry(24, "{", SymbolAction.InsertText("{")),
            entry(25, "}", SymbolAction.InsertText("}")),
            entry(26, "()", SymbolAction.InsertText("()"), SymbolAction.InsertText("()")),
            entry(27, "[]", SymbolAction.InsertText("[]"), SymbolAction.InsertText("[]")),
            entry(28, "{}", SymbolAction.InsertText("{}"), SymbolAction.InsertText("{}")),
            entry(29, "<", SymbolAction.InsertText("<")),
            entry(30, ">", SymbolAction.InsertText(">")),
            entry(31, "\\", SymbolAction.InsertText("\\")),
            entry(32, "$", SymbolAction.InsertText("\$\$")),
            entry(33, "&", SymbolAction.InsertText("&")),
            entry(34, "/*", SymbolAction.InsertText("/**")),
            entry(35, "*/", SymbolAction.InsertText("*/")),
            entry(36, "\n", SymbolAction.InsertText("\n")),
            entry(37, "\t", SymbolAction.InsertText("\t")),
            entry(38, "\\", SymbolAction.InsertText("\\")),
            entry(39, "settings", SymbolAction.OpenManager)
        ))
    )

    private fun entry(
        id: Long,
        label: String,
        shortAction: SymbolAction,
        longAction: SymbolAction? = null
    ) = SymbolEntry(
        id = id,
        label = label,
        shortAction = shortAction,
        longAction = longAction,
        legacyShortActionId = legacyId(shortAction),
        legacyShortText = (shortAction as? SymbolAction.InsertText)?.template,
        legacyLongActionId = longAction?.let(::legacyId),
        legacyLongText = (longAction as? SymbolAction.InsertText)?.template
    )

    private fun legacyId(action: SymbolAction): Int = when (action) {
        is SymbolAction.InsertText -> 0
        is SymbolAction.MoveCursor -> when (action.target) {
            CursorTarget.LEFT -> 18
            CursorTarget.RIGHT -> 19
            CursorTarget.UP -> 20
            CursorTarget.DOWN -> 21
            CursorTarget.LINE_START -> 16
            CursorTarget.LINE_END -> 17
            CursorTarget.DOCUMENT_START -> 23
            CursorTarget.DOCUMENT_END -> 24
        }
        SymbolAction.OpenManager -> 22
        else -> error("Default symbol action is not part of the V1 action catalog")
    }
}
