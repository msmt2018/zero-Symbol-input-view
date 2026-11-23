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

package com.zerostudio.idea.widget.editor.symbolinput.v2.data

import android.content.Context
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup

/**
 * Android persistence adapter for the platform-neutral group repository.
 * Keeps the versioned JSON schema and corruption recovery behavior unchanged.
 */
class SharedPreferencesSymbolGroupRepository(
    context: Context,
    preferencesName: String = "symbol_drawer_v2"
) : ObservableSymbolGroupRepository {
    private val preferences = context.applicationContext
        .getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    override fun observeGroups(listener: () -> Unit): AutoCloseable {
        val observer = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_GROUPS) runCatching(listener)
        }
        preferences.registerOnSharedPreferenceChangeListener(observer)
        return AutoCloseable {
            preferences.unregisterOnSharedPreferenceChangeListener(observer)
        }
    }

    override fun loadGroups(): List<SymbolGroup> {
        val json = preferences.getString(KEY_GROUPS, null) ?: return emptyList()
        return runCatching {
            val groups = SymbolGroupStorageCodec.decode(json)
            val canonical = SymbolGroupStorageCodec.encode(groups)
            if (canonical != json) {
                check(preferences.edit().putString(KEY_GROUPS, canonical).commit()) {
                    "Unable to canonicalize stored symbol groups"
                }
            }
            groups
        }.getOrElse { error ->
            if (!preferences.contains(KEY_CORRUPT_GROUPS_BACKUP)) {
                preferences.edit()
                    .putString(KEY_CORRUPT_GROUPS_BACKUP, json)
                    .putString(KEY_CORRUPT_GROUPS_REASON, error.message?.take(512) ?: "Invalid stored data")
                    .commit()
            }
            emptyList()
        }
    }

    override fun isLegacyMigrationComplete(): Boolean =
        preferences.getBoolean(KEY_LEGACY_MIGRATION_COMPLETE, false)

    override fun markLegacyMigrationComplete() {
        preferences.edit().putBoolean(KEY_LEGACY_MIGRATION_COMPLETE, true).apply()
    }

    override fun saveGroups(groups: List<SymbolGroup>) {
        val encoded = SymbolGroupStorageCodec.encode(groups)
        check(preferences.edit().putString(KEY_GROUPS, encoded).commit()) {
            "Unable to persist symbol groups"
        }
    }

    private companion object {
        const val KEY_GROUPS = "groups_json_v2"
        const val KEY_LEGACY_MIGRATION_COMPLETE = "legacy_migration_complete"
        const val KEY_CORRUPT_GROUPS_BACKUP = "groups_corrupt_backup_v2"
        const val KEY_CORRUPT_GROUPS_REASON = "groups_corrupt_reason_v2"
    }
}
