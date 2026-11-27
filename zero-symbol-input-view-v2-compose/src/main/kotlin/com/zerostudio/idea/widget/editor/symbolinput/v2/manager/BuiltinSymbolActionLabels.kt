/*
 * This file is part of zero-Symbol-input-view-V2.
 *
 * Copyright (C) 2026 android_zero
 *
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.manager

import android.content.Context
import com.zerostudio.idea.widget.editor.symbolinput.v2.compose.R
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.BuiltinSymbolActionOption

/** Resolves built-in action presentation labels at the Compose UI boundary. */
fun localizedBuiltinSymbolActionOptions(context: Context): List<BuiltinSymbolActionOption> = listOf(
        BuiltinSymbolActionOption("line.duplicate", context.getString(R.string.builtin_action_line_duplicate_label)),
        BuiltinSymbolActionOption("line.delete", context.getString(R.string.builtin_action_line_delete_label)),
        BuiltinSymbolActionOption("line.clear", context.getString(R.string.builtin_action_line_clear_label)),
        BuiltinSymbolActionOption("line.replace", context.getString(R.string.builtin_action_line_replace_label), context.getString(R.string.builtin_action_line_replace_hint)),
        BuiltinSymbolActionOption("line.insertAbove", context.getString(R.string.builtin_action_line_insert_above_label)),
        BuiltinSymbolActionOption("line.insertBelow", context.getString(R.string.builtin_action_line_insert_below_label)),
        BuiltinSymbolActionOption("line.joinNext", context.getString(R.string.builtin_action_line_join_next_label)),
        BuiltinSymbolActionOption("line.moveUp", context.getString(R.string.builtin_action_line_move_up_label)),
        BuiltinSymbolActionOption("line.moveDown", context.getString(R.string.builtin_action_line_move_down_label)),
        BuiltinSymbolActionOption("line.select", context.getString(R.string.builtin_action_line_select_label)),
        BuiltinSymbolActionOption("line.uppercase", context.getString(R.string.builtin_action_line_uppercase_label)),
        BuiltinSymbolActionOption("line.lowercase", context.getString(R.string.builtin_action_line_lowercase_label)),
        BuiltinSymbolActionOption("line.toggleCase", context.getString(R.string.builtin_action_line_toggle_case_label)),
        BuiltinSymbolActionOption("line.trimTrailingWhitespace", context.getString(R.string.builtin_action_line_trim_trailing_whitespace_label)),
        BuiltinSymbolActionOption("line.trimLeadingWhitespace", context.getString(R.string.builtin_action_line_trim_leading_whitespace_label)),
        BuiltinSymbolActionOption("line.duplicateAbove", context.getString(R.string.builtin_action_line_duplicate_above_label)),
        BuiltinSymbolActionOption("line.toggleComment", context.getString(R.string.builtin_action_line_toggle_comment_label), context.getString(R.string.builtin_action_line_toggle_comment_hint)),
        BuiltinSymbolActionOption("editor.toggleLineComment", context.getString(R.string.builtin_action_editor_toggle_line_comment_label), context.getString(R.string.builtin_action_editor_toggle_line_comment_hint)),
        BuiltinSymbolActionOption("line.indent", context.getString(R.string.builtin_action_line_indent_label), context.getString(R.string.builtin_action_line_indent_hint)),
        BuiltinSymbolActionOption("line.unindent", context.getString(R.string.builtin_action_line_unindent_label)),
        BuiltinSymbolActionOption("selection.delete", context.getString(R.string.builtin_action_selection_delete_label)),
        BuiltinSymbolActionOption("selection.duplicate", context.getString(R.string.builtin_action_selection_duplicate_label)),
        BuiltinSymbolActionOption("selection.uppercase", context.getString(R.string.builtin_action_selection_uppercase_label)),
        BuiltinSymbolActionOption("selection.lowercase", context.getString(R.string.builtin_action_selection_lowercase_label)),
        BuiltinSymbolActionOption("selection.toggleCase", context.getString(R.string.builtin_action_selection_toggle_case_label)),
        BuiltinSymbolActionOption("selection.wrapParentheses", context.getString(R.string.builtin_action_selection_wrap_parentheses_label)),
        BuiltinSymbolActionOption("selection.wrapBrackets", context.getString(R.string.builtin_action_selection_wrap_brackets_label)),
        BuiltinSymbolActionOption("selection.wrapBraces", context.getString(R.string.builtin_action_selection_wrap_braces_label)),
        BuiltinSymbolActionOption("selection.wrapQuotes", context.getString(R.string.builtin_action_selection_wrap_quotes_label)),
        BuiltinSymbolActionOption("selection.wrapSingleQuotes", context.getString(R.string.builtin_action_selection_wrap_single_quotes_label)),
        BuiltinSymbolActionOption("selection.wrapBackticks", context.getString(R.string.builtin_action_selection_wrap_backticks_label)),
        BuiltinSymbolActionOption("selection.wrapAngleBrackets", context.getString(R.string.builtin_action_selection_wrap_angle_brackets_label)),
        BuiltinSymbolActionOption("selection.wrapAsterisks", context.getString(R.string.builtin_action_selection_wrap_asterisks_label)),
        BuiltinSymbolActionOption("selection.wrapUnderscores", context.getString(R.string.builtin_action_selection_wrap_underscores_label)),
        BuiltinSymbolActionOption("selection.wrapHtmlTag", context.getString(R.string.builtin_action_selection_wrap_html_tag_label), context.getString(R.string.builtin_action_selection_wrap_html_tag_hint)),
        BuiltinSymbolActionOption("selection.indentLines", context.getString(R.string.builtin_action_selection_indent_lines_label), context.getString(R.string.builtin_action_selection_indent_lines_hint)),
        BuiltinSymbolActionOption("selection.unindentLines", context.getString(R.string.builtin_action_selection_unindent_lines_label)),
        BuiltinSymbolActionOption("selection.reverseLines", context.getString(R.string.builtin_action_selection_reverse_lines_label)),
        BuiltinSymbolActionOption("selection.sortLines", context.getString(R.string.builtin_action_selection_sort_lines_label)),
        BuiltinSymbolActionOption("selection.comment", context.getString(R.string.builtin_action_selection_comment_label), context.getString(R.string.builtin_action_selection_comment_hint)),
        BuiltinSymbolActionOption("cursor.wordLeft", context.getString(R.string.builtin_action_cursor_word_left_label)),
        BuiltinSymbolActionOption("cursor.wordRight", context.getString(R.string.builtin_action_cursor_word_right_label)),
        BuiltinSymbolActionOption("cursor.deleteWordBackward", context.getString(R.string.builtin_action_cursor_delete_word_backward_label)),
        BuiltinSymbolActionOption("cursor.deleteWordForward", context.getString(R.string.builtin_action_cursor_delete_word_forward_label)),
        BuiltinSymbolActionOption("cursor.selectWord", context.getString(R.string.builtin_action_cursor_select_word_label)),
        BuiltinSymbolActionOption("cursor.selectLine", context.getString(R.string.builtin_action_cursor_select_line_label)),
        BuiltinSymbolActionOption("cursor.lineStart", context.getString(R.string.builtin_action_cursor_line_start_label)),
        BuiltinSymbolActionOption("cursor.lineEnd", context.getString(R.string.builtin_action_cursor_line_end_label)),
        BuiltinSymbolActionOption("cursor.selectToLineStart", context.getString(R.string.builtin_action_cursor_select_to_line_start_label)),
        BuiltinSymbolActionOption("cursor.selectToLineEnd", context.getString(R.string.builtin_action_cursor_select_to_line_end_label)),
        BuiltinSymbolActionOption("cursor.selectToDocumentStart", context.getString(R.string.builtin_action_cursor_select_to_document_start_label)),
        BuiltinSymbolActionOption("cursor.selectToDocumentEnd", context.getString(R.string.builtin_action_cursor_select_to_document_end_label)),
        BuiltinSymbolActionOption("cursor.documentStart", context.getString(R.string.builtin_action_cursor_document_start_label)),
        BuiltinSymbolActionOption("cursor.documentEnd", context.getString(R.string.builtin_action_cursor_document_end_label)),
        BuiltinSymbolActionOption("document.selectAll", context.getString(R.string.builtin_action_document_select_all_label)),
        BuiltinSymbolActionOption("document.trimTrailingWhitespace", context.getString(R.string.builtin_action_document_trim_trailing_whitespace_label)),
        BuiltinSymbolActionOption("document.trimLeadingWhitespace", context.getString(R.string.builtin_action_document_trim_leading_whitespace_label)),
        BuiltinSymbolActionOption("document.ensureFinalNewline", context.getString(R.string.builtin_action_document_ensure_final_newline_label), context.getString(R.string.builtin_action_document_ensure_final_newline_hint)),
        BuiltinSymbolActionOption("document.removeBlankLines", context.getString(R.string.builtin_action_document_remove_blank_lines_label)),
        BuiltinSymbolActionOption("document.toggleComment", context.getString(R.string.builtin_action_document_toggle_comment_label), context.getString(R.string.builtin_action_document_toggle_comment_hint)),
        BuiltinSymbolActionOption("undo", context.getString(R.string.builtin_action_undo_label)),
        BuiltinSymbolActionOption("redo", context.getString(R.string.builtin_action_redo_label)),
        BuiltinSymbolActionOption("indent", context.getString(R.string.builtin_action_indent_label)),
        BuiltinSymbolActionOption("unindent", context.getString(R.string.builtin_action_unindent_label)),
        BuiltinSymbolActionOption("format", context.getString(R.string.builtin_action_format_label)),
        BuiltinSymbolActionOption("insert_payload", context.getString(R.string.builtin_action_insert_payload_label), context.getString(R.string.builtin_action_insert_payload_hint))
    )
