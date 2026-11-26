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

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup

/**
 * Platform-neutral persistence contract consumed by the drawer and manager.
 * Implementations own storage and must return detached immutable snapshots.
 */
interface SymbolGroupRepository {
    fun loadGroups(): List<SymbolGroup>
    fun saveGroups(groups: List<SymbolGroup>)
    fun isLegacyMigrationComplete(): Boolean = loadGroups().isNotEmpty()
    fun markLegacyMigrationComplete() = Unit
}

/**
 * Optional live-change observation for repositories shared by a manager and a
 * visible drawer. Implementations notify after a successful durable write.
 *
 * The callback may run on any thread. Consumers that update thread-confined
 * state must marshal the notification to that state's owning thread.
 */
interface ObservableSymbolGroupRepository : SymbolGroupRepository {
    fun observeGroups(listener: () -> Unit): AutoCloseable
}
