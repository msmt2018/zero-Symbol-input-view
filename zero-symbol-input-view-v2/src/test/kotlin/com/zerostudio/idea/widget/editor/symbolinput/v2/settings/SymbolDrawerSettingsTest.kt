/*
 * This file is part of ZeroStudio.
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

package com.zerostudio.idea.widget.editor.symbolinput.v2.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolDrawerSettingsTest {
    @Test fun settingsCodecRoundTripsAllValues() {
        val value = SymbolDrawerSettings(
            columns = 6,
            themeMode = ThemeMode.DARK,
            showActionHints = true,
            reduceMotion = true,
            pageCacheLimit = 5
        )
        assertEquals(value, SymbolDrawerSettingsCodec.decode(SymbolDrawerSettingsCodec.encode(value)))
    }

    @Test fun missingFieldsUseCurrentDefaultsForStoredSettingsMigration() {
        val value = SymbolDrawerSettingsCodec.decode("""{"schemaVersion":1,"columns":7}""")
        assertEquals(7, value.columns)
        assertEquals(SymbolDrawerSettings().cellHeightDp, value.cellHeightDp)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMissingFieldsInExternalSettingsBackup() {
        SymbolDrawerSettingsCodec.decodeStrict("""{"schemaVersion":1,"columns":7}""")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnknownSettingsFields() {
        val json = SymbolDrawerSettingsCodec.encode(SymbolDrawerSettings()).dropLast(1) + ""","unknown":true}"""
        SymbolDrawerSettingsCodec.decodeStrict(json)
    }

    @Test fun negativeCellSpacingIsSupportedAndPersisted() {
        val value = SymbolDrawerSettings(cellHorizontalSpacingDp = -10, cellVerticalSpacingDp = -6)
        val restored = SymbolDrawerSettingsCodec.decode(SymbolDrawerSettingsCodec.encode(value))
        assertEquals(-10, restored.cellHorizontalSpacingDp)
        assertEquals(-6, restored.cellVerticalSpacingDp)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsCellSpacingBelowFineAdjustmentLimit() {
        SymbolDrawerSettings(cellHorizontalSpacingDp = -11)
    }

    @Test fun drawerConfigHonorsReducedMotion() {
        val config = SymbolDrawerSettings(animationDurationMs = 350, reduceMotion = true).toDrawerConfig()
        assertEquals(0L, config.animationDurationMs)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOutOfRangeColumns() {
        SymbolDrawerSettings(columns = 0)
    }
}
