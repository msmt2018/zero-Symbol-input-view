/*
 * This file is part of ZeroStudio.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.action

/** Expanded V1-compatible insertion template and offsets in the expanded output. */
data class SymbolTextMacroExpansion(
    val text: String,
    val insertionOffset: Int,
    val selectionEndOffset: Int
)

/** UTF-16 range; end is exclusive. */
data class SymbolTextRange(val start: Int, val end: Int) {
    init {
        require(start >= 0 && end >= start)
    }
}

/** Position and content for inserting a duplicate line into a UTF-16 text document. */
data class SymbolTextInsertion(
    val line: Int,
    val column: Int,
    val text: String
)

/**
 * Small, editor-neutral text operations shared by hosts. They intentionally
 * operate on CharSequence and offsets rather than Sora Content or CodeEditor.
 */
object SymbolTextEditing {
    /**
     * Expands $S/$E/$T/$$ while recording marker offsets against the actual
     * expanded output, including text substituted for $T.
     */
    @JvmStatic
    fun expandMacro(template: String, selectedText: String): SymbolTextMacroExpansion {
        val output = StringBuilder(template.length + selectedText.length)
        var insertionOffset: Int? = null
        var selectionEndOffset: Int? = null
        var index = 0
        while (index < template.length) {
            if (template[index] != '$' || index + 1 >= template.length) {
                output.append(template[index++])
                continue
            }
            when (template[index + 1]) {
                '$' -> {
                    output.append('$')
                    index += 2
                }
                'S' -> {
                    if (insertionOffset == null) insertionOffset = output.length
                    index += 2
                }
                'E' -> {
                    if (selectionEndOffset == null) selectionEndOffset = output.length
                    index += 2
                }
                'T' -> {
                    output.append(selectedText)
                    index += 2
                }
                else -> {
                    output.append('$')
                    index++
                }
            }
        }
        val start = insertionOffset ?: output.length
        val end = selectionEndOffset ?: start
        return SymbolTextMacroExpansion(output.toString(), start, end)
    }

    /**
     * Returns the UTF-16 range for deleting one adjacent Unicode code point.
     * A supplementary character is represented by a surrogate pair and must
     * be removed as a whole; ordinary BMP characters remain one code unit.
     */
    @JvmStatic
    fun codePointDeletionRange(
        text: CharSequence,
        cursorOffset: Int,
        backwards: Boolean
    ): SymbolTextRange? {
        require(cursorOffset in 0..text.length)
        if (backwards) {
            if (cursorOffset == 0) return null
            var start = cursorOffset - 1
            if (Character.isLowSurrogate(text[start]) &&
                start > 0 && Character.isHighSurrogate(text[start - 1])
            ) {
                start--
            }
            return SymbolTextRange(start, cursorOffset)
        }
        if (cursorOffset == text.length) return null
        var end = cursorOffset + 1
        if (Character.isHighSurrogate(text[cursorOffset]) &&
            end < text.length && Character.isLowSurrogate(text[end])
        ) {
            end++
        }
        return SymbolTextRange(cursorOffset, end)
    }

    /**
     * Inserts after the selected line. When the selected line is the final
     * unterminated line, append a line separator and the duplicate at its end
     * instead of addressing the invalid lineCount index.
     */
    @JvmStatic
    fun duplicateLineInsertion(
        line: Int,
        lineCount: Int,
        lineLength: Int,
        lineContent: String
    ): SymbolTextInsertion {
        require(lineCount > 0 && line in 0 until lineCount)
        require(lineLength >= 0)
        return if (line + 1 < lineCount) {
            SymbolTextInsertion(line + 1, 0, "$lineContent\n")
        } else {
            SymbolTextInsertion(line, lineLength, "\n$lineContent")
        }
    }
}
