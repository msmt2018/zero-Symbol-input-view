/*
 * This file is part of zero-Symbol-input-view-V2.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.drawer.action

/**
 * Editor-neutral text document contract.
 *
 * The drawer/action engine never receives an editor, view, controller, syntax
 * tree, or editor-core object. A host supplies this small adapter for the
 * currently focused document. The adapter may be backed by any editor, a
 * plain text field, an in-memory buffer, or a remote document implementation.
 *
 * All offsets are UTF-16 offsets (the indexing convention used by JVM String).
 * [lineEndOffset] excludes the line separator. Implementations must keep the
 * document and selection state coherent when replace() succeeds.
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

    /** Replace UTF-16 range [start, end) and return false if rejected. */
    fun replace(start: Int, end: Int, replacement: String): Boolean

    /** Set a selection using UTF-16 offsets; equal offsets place the caret. */
    fun setSelection(start: Int, end: Int)

    /**
     * Returns a selection range whose boundaries never split a UTF-16 surrogate pair.
     * A collapsed caret inside a pair snaps to its leading boundary; a non-empty
     * selection expands to include every code point touched by either endpoint.
     */
    fun codePointSafeSelectionRange(): TextDocumentRange {
        val rawStart = minOf(selectionStart, selectionEnd).coerceIn(0, textLength)
        val rawEnd = maxOf(selectionStart, selectionEnd).coerceIn(rawStart, textLength)
        if (rawStart == rawEnd) {
            val caret = if (rawStart > 0 && rawStart < textLength &&
                Character.isHighSurrogate(charAt(rawStart - 1)) &&
                Character.isLowSurrogate(charAt(rawStart))
            ) rawStart - 1 else rawStart
            return TextDocumentRange(caret, caret)
        }
        val start = if (rawStart > 0 && rawStart < textLength &&
            Character.isHighSurrogate(charAt(rawStart - 1)) &&
            Character.isLowSurrogate(charAt(rawStart))
        ) rawStart - 1 else rawStart
        val end = if (rawEnd > 0 && rawEnd < textLength &&
            Character.isHighSurrogate(charAt(rawEnd - 1)) &&
            Character.isLowSurrogate(charAt(rawEnd))
        ) rawEnd + 1 else rawEnd
        return TextDocumentRange(start, end)
    }

    /**
     * True only when [endEditTransaction] with commit=false atomically restores
     * both document text and selection to the state at [beginEditTransaction].
     * Hosts that do not provide that guarantee must leave this false; the
     * default edit journal will then restore the changed ranges itself.
     */
    val supportsAtomicEditTransactions: Boolean get() = false

    /** Optional host transaction hooks. */
    fun beginEditTransaction() = Unit
    fun endEditTransaction(commit: Boolean) = Unit

    fun selectedText(): String {
        val range = codePointSafeSelectionRange()
        return textBetween(range.start, range.end)
    }

    fun positionForOffset(offset: Int): TextDocumentPosition {
        val safeOffset = offset.coerceIn(0, textLength)
        if (lineCount <= 0) return TextDocumentPosition(0, safeOffset)
        val line = lineForOffset(safeOffset).coerceIn(0, lineCount - 1)
        return TextDocumentPosition(line, (safeOffset - lineStartOffset(line)).coerceAtLeast(0))
    }

    fun offsetForPosition(position: TextDocumentPosition): Int {
        if (lineCount <= 0) return 0
        val line = position.line.coerceIn(0, lineCount - 1)
        val start = lineStartOffset(line).coerceIn(0, textLength)
        val end = lineEndOffset(line).coerceIn(start, textLength)
        return (start + position.column.coerceAtLeast(0)).coerceAtMost(end)
    }

    fun setCursorPosition(position: TextDocumentPosition) {
        val offset = offsetForPosition(position)
        setSelection(offset, offset)
    }

    /**
     * Applies non-overlapping edits from right to left so earlier offsets remain
     * stable. Successfully applied edits are journaled and reversed if a later
     * replacement is rejected or throws. The journal stores only replaced
     * ranges, not a duplicate of the complete document, so memory use scales
     * with the edited content rather than the document size.
     *
     * Hosts with native edit transactions may set [supportsAtomicEditTransactions]
     * to true only when endEditTransaction(false) restores both text and selection.
     * Otherwise the default rollback restores changed ranges through [replace];
     * if a host rejects rollback, this method throws because the document may
     * be left partially modified.
     */
    fun applyEdits(edits: List<TextDocumentEdit>): Boolean {
        if (!isEditable) return false
        if (edits.isEmpty()) return true
        val ordered = edits.sortedByDescending { it.start }
        var previousStart = textLength + 1
        var previousWasInsertion = false
        for (edit in ordered) {
            val sameOffsetInsertionsConflict =
                edit.start == previousStart && (edit.start == edit.end || previousWasInsertion)
            if (edit.start < 0 || edit.end < edit.start || edit.end > textLength ||
                edit.end > previousStart || sameOffsetInsertionsConflict
            ) return false
            previousStart = edit.start
            previousWasInsertion = edit.start == edit.end
        }

        data class AppliedEdit(val edit: TextDocumentEdit, val originalText: String)
        val applied = ArrayList<AppliedEdit>(ordered.size)
        val originalSelectionStart = selectionStart.coerceIn(0, textLength)
        val originalSelectionEnd = selectionEnd.coerceIn(0, textLength)
        beginEditTransaction()
        var transactionFinalized = false

        fun rollback(cause: Throwable? = null): Throwable? {
            if (supportsAtomicEditTransactions) return cause
            var failure = cause
            // Edits were applied from right to left. Undo them from left to right
            // so restoring an earlier range cannot invalidate a later offset.
            for (entry in applied.asReversed()) {
                val edit = entry.edit
                val changedEnd = edit.start + edit.replacement.length
                try {
                    if (!replace(edit.start, changedEnd, entry.originalText)) {
                        throw IllegalStateException(
                            "Host rejected rollback for edit at ${edit.start}"
                        )
                    }
                } catch (rollbackError: Throwable) {
                    if (failure == null) failure = rollbackError
                    else failure.addSuppressed(rollbackError)
                }
            }
            try {
                setSelection(originalSelectionStart, originalSelectionEnd)
            } catch (selectionError: Throwable) {
                if (failure == null) failure = selectionError
                else failure.addSuppressed(selectionError)
            }
            return failure
        }

        try {
            for (edit in ordered) {
                val originalText = textBetween(edit.start, edit.end)
                if (!replace(edit.start, edit.end, edit.replacement)) {
                    val rollbackFailure = rollback()
                    transactionFinalized = true
                    try {
                        endEditTransaction(false)
                    } catch (transactionError: Throwable) {
                        if (rollbackFailure == null) throw transactionError
                        rollbackFailure.addSuppressed(transactionError)
                    }
                    if (rollbackFailure != null) {
                        throw IllegalStateException(
                            "Edit batch was rejected and document rollback was incomplete",
                            rollbackFailure
                        )
                    }
                    return false
                }
                applied += AppliedEdit(edit, originalText)
            }
            endEditTransaction(true)
            transactionFinalized = true
            return true
        } catch (error: Throwable) {
            if (!transactionFinalized) {
                val rollbackFailure = rollback(error)
                try {
                    endEditTransaction(false)
                } catch (transactionError: Throwable) {
                    rollbackFailure?.addSuppressed(transactionError)
                        ?: error.addSuppressed(transactionError)
                }
                transactionFinalized = true
            }
            throw error
        }
    }
}

data class TextDocumentRange(val start: Int, val end: Int) {
    init { require(start >= 0 && end >= start) }
}

data class TextDocumentPosition(val line: Int, val column: Int) {
    init { require(line >= 0 && column >= 0) }
}

/** UTF-16 edit range; end is exclusive. */
data class TextDocumentEdit(val start: Int, val end: Int, val replacement: String) {
    init { require(start >= 0 && end >= start) }
}
