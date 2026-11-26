/*
 * This file is part of zero-Symbol-input-view-V2.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.action

/**
 * Historical JVM result type retained for binary compatibility.
 * New integrations should use drawer.action.SymbolTextMacroExpansion.
 */
data class SymbolTextMacroExpansion(
    val text: String,
    val insertionOffset: Int,
    val selectionEndOffset: Int
)

/** Historical JVM range type retained for binary compatibility. */
data class SymbolTextRange(val start: Int, val end: Int) {
    init { require(start >= 0 && end >= start) }
}

/** Historical JVM insertion type retained for binary compatibility. */
data class SymbolTextInsertion(
    val line: Int,
    val column: Int,
    val text: String
)

/**
 * Binary-compatible forwarding facade. The implementation and canonical
 * result types now live in the editor-neutral drawer.action package.
 */
object SymbolTextEditing {
    @JvmStatic
    @JvmOverloads
    fun expandMacro(
        template: String,
        selectedText: String,
        currentLineText: String = "",
        currentWordText: String = "",
        lineSeparator: String = "\n",
        currentIndent: String = currentLineText.takeWhile { it == ' ' || it == '\t' }
    ): SymbolTextMacroExpansion {
        val result = com.zerostudio.idea.widget.drawer.action.SymbolTextEditing.expandMacro(
            template = template,
            selectedText = selectedText,
            currentLineText = currentLineText,
            currentWordText = currentWordText,
            lineSeparator = lineSeparator,
            currentIndent = currentIndent
        )
        return SymbolTextMacroExpansion(
            result.text,
            result.insertionOffset,
            result.selectionEndOffset
        )
    }

    @JvmStatic
    fun codePointDeletionRange(
        text: CharSequence,
        cursorOffset: Int,
        backwards: Boolean
    ): SymbolTextRange? =
        com.zerostudio.idea.widget.drawer.action.SymbolTextEditing
            .codePointDeletionRange(text, cursorOffset, backwards)
            ?.let { SymbolTextRange(it.start, it.end) }

    @JvmStatic
    fun duplicateLineInsertion(
        line: Int,
        lineCount: Int,
        lineLength: Int,
        lineContent: String,
        lineSeparator: String = "\n"
    ): SymbolTextInsertion {
        val result = com.zerostudio.idea.widget.drawer.action.SymbolTextEditing
            .duplicateLineInsertion(line, lineCount, lineLength, lineContent, lineSeparator)
        return SymbolTextInsertion(result.line, result.column, result.text)
    }
}
