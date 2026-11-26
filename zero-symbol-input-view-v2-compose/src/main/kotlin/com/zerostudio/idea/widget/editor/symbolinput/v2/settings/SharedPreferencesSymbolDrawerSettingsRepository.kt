/*
 * This file is part of zero-Symbol-input-view-V2.
 *
 * Copyright (C) 2026 android_zero
 *
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 *
 * License Information:
 *
 * This file is distributed under the AGPL-3.0-only, GPL-3.0-only and
 * Apache-2.0 licenses cumulatively. See the repository license files for
 * the complete terms.
 *
 * Author: android_zero
 * GitHub: https://github.com/msmt2018
 * WeChat: android_zero
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.settings

import android.content.Context
import android.content.SharedPreferences

/** Android SharedPreferences adapter for the platform-neutral settings contract. */
class SharedPreferencesSymbolDrawerSettingsRepository(
    context: Context,
    preferencesName: String = "symbol_drawer_v2_settings",
    private val preferences: SharedPreferences = context.applicationContext
        .getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
) : SymbolDrawerSettingsRepository {
    override fun load(): SettingsLoadResult {
        val raw = preferences.getString(KEY_SETTINGS, null)
            ?: return SettingsLoadResult(SymbolDrawerSettings())
        val decoded = runCatching { SymbolDrawerSettingsCodec.decode(raw) }
        return decoded.fold(
            onSuccess = { SettingsLoadResult(it) },
            onFailure = { SettingsLoadResult(SymbolDrawerSettings(), recoveredFromInvalidData = true) }
        )
    }

    override fun save(settings: SymbolDrawerSettings) {
        val json = SymbolDrawerSettingsCodec.encode(settings)
        check(preferences.edit().putString(KEY_SETTINGS, json).commit()) {
            "Unable to persist symbol drawer settings"
        }
    }

    override fun reset() {
        check(preferences.edit().remove(KEY_SETTINGS).commit()) {
            "Unable to reset symbol drawer settings"
        }
    }

    override fun hasStoredSettings(): Boolean = preferences.contains(KEY_SETTINGS)

    private companion object {
        const val KEY_SETTINGS = "settings_json_v1"
    }
}
