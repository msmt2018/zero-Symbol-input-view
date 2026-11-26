/*
 * This file is part of zero-Symbol-input-view-V2.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.drawer.action

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
    /** Reverses Unicode code points without reversing surrogate code units. */
    private fun reverseCodePoints(value: String): String {
        val result = StringBuilder(value.length)
        var offset = value.length
        while (offset > 0) {
            val codePoint = Character.codePointBefore(value, offset)
            result.appendCodePoint(codePoint)
            offset -= Character.charCount(codePoint)
        }
        return result.toString()
    }

    /**
     * Expands $S/$E/$T/$$ while recording marker offsets against the actual
     * expanded output, including text substituted for $T.
     */
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
        val output = StringBuilder()
        var insertionOffset: Int? = null
        var selectionEndOffset: Int? = null
        var index = 0
        while (index < template.length) {
            val longToken = when {
                template.startsWith("\$TRIM", index) -> "\$TRIM"
                template.startsWith("\$CAP", index) -> "\$CAP"
                template.startsWith("\$LOW", index) -> "\$LOW"
                template.startsWith("\$UP", index) -> "\$UP"
                template.startsWith("\$REV", index) -> "\$REV"
                template.startsWith("\$TAB", index) -> "\$TAB"
                else -> null
            }
            if (longToken != null) {
                val transformed = when (longToken) {
                    "\$TRIM" -> selectedText.trim()
                    "\$CAP" -> selectedText.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    "\$LOW" -> selectedText.lowercase()
                    "\$UP" -> selectedText.uppercase()
                    "\$REV" -> reverseCodePoints(selectedText)
                    else -> "\t"
                }
                output.append(transformed)
                index += longToken.length
                continue
            }
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
                'C' -> {
                    insertionOffset = output.length
                    index += 2
                }
                'T' -> {
                    output.append(selectedText)
                    index += 2
                }
                'U' -> {
                    output.append(selectedText.uppercase())
                    index += 2
                }
                'V' -> {
                    output.append(selectedText.lowercase())
                    index += 2
                }
                'R' -> {
                    output.append(reverseCodePoints(selectedText))
                    index += 2
                }
                'N' -> {
                    output.append(lineSeparator)
                    index += 2
                }
                'L' -> {
                    output.append(currentLineText)
                    index += 2
                }
                'W' -> {
                    output.append(currentWordText)
                    index += 2
                }
                'I' -> {
                    output.append(currentIndent)
                    index += 2
                }
                else -> {
                    output.append('$')
                    index++
                }
            }
        }
        val startOffset = insertionOffset ?: output.length
        val endOffset = selectionEndOffset ?: startOffset
        return SymbolTextMacroExpansion(output.toString(), startOffset, endOffset)
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
        // A caret reported between a high/low surrogate is normalized to the
        // leading boundary before applying directional deletion semantics.
        val safeCursor = if (
            cursorOffset > 0 && cursorOffset < text.length &&
            Character.isHighSurrogate(text[cursorOffset - 1]) &&
            Character.isLowSurrogate(text[cursorOffset])
        ) cursorOffset - 1 else cursorOffset
        if (backwards) {
            if (safeCursor == 0) return null
            var start = safeCursor - 1
            if (Character.isLowSurrogate(text[start]) &&
                start > 0 && Character.isHighSurrogate(text[start - 1])
            ) {
                start--
            }
            return SymbolTextRange(start, safeCursor)
        }
        if (safeCursor == text.length) return null
        var end = safeCursor + 1
        if (Character.isHighSurrogate(text[safeCursor]) &&
            end < text.length && Character.isLowSurrogate(text[end])
        ) {
            end++
        }
        return SymbolTextRange(safeCursor, end)
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
        lineContent: String,
        lineSeparator: String = "\n"
    ): SymbolTextInsertion {
        require(lineCount > 0 && line in 0 until lineCount)
        require(lineLength >= 0)
        return if (line + 1 < lineCount) {
            SymbolTextInsertion(line + 1, 0, "$lineContent$lineSeparator")
        } else {
            SymbolTextInsertion(line, lineLength, "$lineSeparator$lineContent")
        }
    }
}
