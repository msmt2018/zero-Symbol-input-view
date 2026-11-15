package com.zerostudio.idea.widget.editor.symbolinput.v2.action

/**
 * Minimal editor-neutral text document contract used by built-in editing actions.
 * An editor integration implements this adapter once; action algorithms do not
 * depend on Sora, Android Views, or any particular editor engine.
 */
interface SymbolTextDocument {
    val isEditable: Boolean
    val textLength: Int
    val lineCount: Int
    val cursorOffset: Int
    val selectionStart: Int
    val selectionEnd: Int

    fun lineForOffset(offset: Int): Int
    fun lineStartOffset(line: Int): Int
    fun lineEndOffset(line: Int): Int
    fun lineText(line: Int): String
    fun textBetween(start: Int, end: Int): String
    fun charAt(offset: Int): Char

    /** Replace UTF-16 range [start, end) and return false if the host rejects the edit. */
    fun replace(start: Int, end: Int, replacement: String): Boolean

    /** Set a selection using UTF-16 offsets; equal offsets place the caret. */
    fun setSelection(start: Int, end: Int)
}
