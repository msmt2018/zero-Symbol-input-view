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

/**
 * Platform-neutral source of truth for the V1 action identifier contract.
 *
 * Picker ordering is distinct from the importable set: function-key IDs and
 * historical clipboard IDs remain readable without being offered as choices.
 */
object LegacySymbolActionDefinitions {
    private val orderedActionIds = intArrayOf(
        0, 3, 4, 7, 8, 9, 11, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 32, 33, 34, 35, 36, 37, 38, 39, 52, 53, 54, 55, 56
    )

    /** Defensive copy; external callers cannot mutate the stable V1 ID order. */
    val values: IntArray
        get() = orderedActionIds.copyOf()

    /** Includes picker IDs and import-only historical IDs. */
    val importableValues: Set<Int> =
        orderedActionIds.toSet() + setOf(1, 2) + (40..51)

    /** V1 text payloads are used by insertion and replace-current-line actions. */
    fun hasTextPayload(actionId: Int): Boolean = actionId == 0 || actionId == 36

    /** Stable English fallback labels in the same order as [values]. */
    val labels: List<String> = listOf(
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

    init {
        check(orderedActionIds.size == labels.size)
        check(orderedActionIds.distinct().size == orderedActionIds.size)
        check(importableValues.containsAll(orderedActionIds.toList()))
    }
}
