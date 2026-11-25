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

/**
 * Android resource adapter and compatibility facade for V1 action metadata.
 *
 * Stable IDs and import rules live in [LegacySymbolActionDefinitions].
 * Localized labels stay in this adapter because they are resolved by Android.
 */
object LegacySymbolActionCatalog {
    val values: IntArray
        get() = LegacySymbolActionDefinitions.values

    val importableValues: Set<Int>
        get() = LegacySymbolActionDefinitions.importableValues

    val labels: List<String>
        get() = LegacySymbolActionDefinitions.labels

    fun hasTextPayload(actionId: Int): Boolean =
        LegacySymbolActionDefinitions.hasTextPayload(actionId)

    fun labels(context: Context): List<String> = listOf(
        context.getString(R.string.legacy_action_label_01),
        context.getString(R.string.legacy_action_label_02),
        context.getString(R.string.legacy_action_label_03),
        context.getString(R.string.legacy_action_label_04),
        context.getString(R.string.legacy_action_label_05),
        context.getString(R.string.legacy_action_label_06),
        context.getString(R.string.legacy_action_label_07),
        context.getString(R.string.legacy_action_label_08),
        context.getString(R.string.legacy_action_label_09),
        context.getString(R.string.legacy_action_label_10),
        context.getString(R.string.legacy_action_label_11),
        context.getString(R.string.legacy_action_label_12),
        context.getString(R.string.legacy_action_label_13),
        context.getString(R.string.legacy_action_label_14),
        context.getString(R.string.legacy_action_label_15),
        context.getString(R.string.legacy_action_label_16),
        context.getString(R.string.legacy_action_label_17),
        context.getString(R.string.legacy_action_label_18),
        context.getString(R.string.legacy_action_label_19),
        context.getString(R.string.legacy_action_label_20),
        context.getString(R.string.legacy_action_label_21),
        context.getString(R.string.legacy_action_label_22),
        context.getString(R.string.legacy_action_label_23),
        context.getString(R.string.legacy_action_label_24),
        context.getString(R.string.legacy_action_label_25),
        context.getString(R.string.legacy_action_label_26),
        context.getString(R.string.legacy_action_label_27),
        context.getString(R.string.legacy_action_label_28),
        context.getString(R.string.legacy_action_label_29),
        context.getString(R.string.legacy_action_label_30),
        context.getString(R.string.legacy_action_label_31),
        context.getString(R.string.legacy_action_label_32),
        context.getString(R.string.legacy_action_label_33)
    ).also { localized ->
        check(localized.size == values.size) {
            "Localized V1 action labels must stay aligned with the picker IDs."
        }
    }
}
