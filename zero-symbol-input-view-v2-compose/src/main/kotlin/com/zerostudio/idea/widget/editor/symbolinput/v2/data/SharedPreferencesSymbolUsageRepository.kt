/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 * Author: android_zero · https://github.com/msmt2018
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.data

import android.content.Context
import com.zerostudio.idea.widget.editor.symbolinput.v2.manager.SymbolUsageRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.manager.SymbolUsageSnapshot

/**
 * SharedPreferences adapter for manager favorites and recent symbol history.
 * Uses the same preference file and keys as the legacy manager for continuity.
 */
class SharedPreferencesSymbolUsageRepository(context: Context) : SymbolUsageRepository {
    private val preferences = context.applicationContext.getSharedPreferences(
        "zero_symbol_manager_usage_v2",
        Context.MODE_PRIVATE
    )

    override fun load(): SymbolUsageSnapshot {
        val favorites = preferences.getStringSet(KEY_FAVORITES, emptySet())
            .orEmpty()
            .mapNotNull(String::toLongOrNull)
            .filterTo(linkedSetOf()) { it != 0L }
        val recent = preferences.getString(KEY_RECENT, "")
            .orEmpty()
            .split(',')
            .mapNotNull(String::toLongOrNull)
            .filter { it != 0L }
            .distinct()
            .take(RECENT_LIMIT)
        return SymbolUsageSnapshot(favorites, recent)
    }

    override fun save(snapshot: SymbolUsageSnapshot) {
        preferences.edit()
            .putStringSet(KEY_FAVORITES, snapshot.favorites.map(Long::toString).toSet())
            .putString(KEY_RECENT, snapshot.recent.joinToString(","))
            .apply()
    }

    private companion object {
        const val KEY_FAVORITES = "favorites"
        const val KEY_RECENT = "recent"
        const val RECENT_LIMIT = 30
    }
}
