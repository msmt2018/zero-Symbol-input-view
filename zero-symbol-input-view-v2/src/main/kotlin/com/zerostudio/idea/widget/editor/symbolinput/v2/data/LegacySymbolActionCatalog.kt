/*
 * This file is part of zero-Symbol-input-view-V2.
 *
 * Copyright (C) 2026 android_zero
 *
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 *
 * License Information:
 *
 * This file is distributed under the following licenses, which apply
 * cumulatively to the same work:
 *
 * 1. GNU Affero General Public License v3.0 only (AGPL-3.0-only):
 *    Grants the rights to use, study, modify, and redistribute this work.
 *    When a modified version is used to provide network services to users,
 *    the corresponding source code must be made available under the
 *    applicable AGPL terms.
 *
 * 2. GNU General Public License v3.0 only (GPL-3.0-only):
 *    Grants the rights to use, study, modify, and redistribute this work.
 *    Distribution of covered binaries requires compliance with the source
 *    code, license notice, attribution, and other applicable GPL terms.
 *
 * 3. Apache License 2.0 (Apache-2.0):
 *    Grants copyright and applicable patent rights to use, modify, and
 *    distribute this work, subject to its license conditions, including
 *    preservation of notices, inclusion of the license, and marking changes.
 *
 * The SPDX operator "AND" means that all listed license terms apply
 * cumulatively. It does not mean that recipients may choose only one
 * of the listed licenses.
 *
 * You must comply with the applicable terms of all three licenses.
 * This notice is a summary and does not replace the full license texts.
 *
 * Full license texts:
 * - GNU AGPL v3.0: https://www.gnu.org/licenses/agpl-3.0.html
 * - GNU GPL v3.0:  https://www.gnu.org/licenses/gpl-3.0.html
 * - Apache 2.0:    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Author: android_zero
 * GitHub: https://github.com/msmt2018
 * WeChat: android_zero
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.data

import android.content.Context
import com.zerostudio.idea.widget.editor.symbolinput.v2.R

/** Action IDs and ordering copied from V1 res/values/arrays.xml. */
object LegacySymbolActionCatalog {
    val values = intArrayOf(
        0, 3, 4, 7, 8, 9, 11, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 32, 33, 34, 35, 36, 37, 38, 39, 52, 53, 54, 55, 56
    )
    // Legacy F1-F12 IDs remain decodable for existing data, but are not offered
    // as new choices: V1 only inserted literal "<F1>" text rather than dispatching
    // a function key. Historical clipboard IDs 1/2 are also import-only.
    val importableValues: Set<Int> = values.toSet() + setOf(1, 2) + (40..51)

    /** V1 text payloads are used by insertion and replace-current-line actions. */
    fun hasTextPayload(actionId: Int): Boolean = actionId == 0 || actionId == 36

    val labels = listOf(
        "Insert text",
        "Delete line",
        "Clear line",
        "Convert to uppercase",
        "Convert to lowercase",
        "Indent (Tab)",
        "Toggle comment",
        "Go to line start",
        "Go to line end",
        "Move cursor left",
        "Move cursor right",
        "Move cursor up",
        "Move cursor down",
        "Open symbol toolbar manager",
        "Go to document start",
        "Go to document end",
        "Select all",
        "Copy",
        "Cut",
        "Paste",
        "Format code",
        "Copy line",
        "Cut line",
        "Duplicate line",
        "Replace line",
        "Enter key",
        "Tab key",
        "Shift + Tab",
        "Copy",
        "Cut",
        "Paste",
        "Undo",
        "Redo"
    )

    fun labels(context: Context): List<String> = listOf(
        context.getString(R.string.legacy_action_insert_text),
        context.getString(R.string.legacy_action_delete_line),
        context.getString(R.string.legacy_action_clear_line),
        context.getString(R.string.legacy_action_convert_to_uppercase),
        context.getString(R.string.legacy_action_convert_to_lowercase),
        context.getString(R.string.legacy_action_indent_tab),
        context.getString(R.string.legacy_action_toggle_comment),
        context.getString(R.string.legacy_action_go_to_line_start),
        context.getString(R.string.legacy_action_go_to_line_end),
        context.getString(R.string.legacy_action_move_cursor_left),
        context.getString(R.string.legacy_action_move_cursor_right),
        context.getString(R.string.legacy_action_move_cursor_up),
        context.getString(R.string.legacy_action_move_cursor_down),
        context.getString(R.string.legacy_action_open_symbol_toolbar_manager),
        context.getString(R.string.legacy_action_go_to_document_start),
        context.getString(R.string.legacy_action_go_to_document_end),
        context.getString(R.string.legacy_action_select_all),
        context.getString(R.string.legacy_action_copy),
        context.getString(R.string.legacy_action_cut),
        context.getString(R.string.legacy_action_paste),
        context.getString(R.string.legacy_action_format_code),
        context.getString(R.string.legacy_action_copy_line),
        context.getString(R.string.legacy_action_cut_line),
        context.getString(R.string.legacy_action_duplicate_line),
        context.getString(R.string.legacy_action_replace_line),
        context.getString(R.string.legacy_action_enter_key),
        context.getString(R.string.legacy_action_tab_key),
        context.getString(R.string.legacy_action_shift_tab),
        context.getString(R.string.legacy_action_clipboard_copy),
        context.getString(R.string.legacy_action_clipboard_cut),
        context.getString(R.string.legacy_action_clipboard_paste),
        context.getString(R.string.legacy_action_undo),
        context.getString(R.string.legacy_action_redo)
    )
    init { check(values.size == labels.size) }
}
