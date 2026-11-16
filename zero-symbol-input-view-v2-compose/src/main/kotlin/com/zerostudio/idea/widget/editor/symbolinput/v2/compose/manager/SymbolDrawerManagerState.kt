/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 * Author: android_zero · https://github.com/msmt2018
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.compose.manager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupBundleCodec
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupManager
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup

/**
 * Compose-observable command facade over the existing V2 repository/manager.
 *
 * The manager remains the single source of truth for validation and durable
 * writes. This facade only publishes immutable snapshots to Compose after a
 * command succeeds; it does not introduce another persistence format.
 */
class SymbolDrawerManagerState internal constructor(
    private val manager: SymbolGroupManager
) {
    var groups by mutableStateOf(manager.groups)
        private set

    var lastError by mutableStateOf<String?>(null)
        private set

    fun reload(): Boolean = command { manager.reload() }

    fun addGroup(title: String): SymbolGroup? =
        commandResult { manager.addGroup(title) }

    fun duplicateGroup(groupId: String): SymbolGroup? =
        commandResult { manager.duplicateGroup(groupId) }

    fun renameGroup(groupId: String, title: String): Boolean =
        command { manager.renameGroup(groupId, title) }

    fun removeGroup(groupId: String): Boolean =
        command { manager.removeGroup(groupId) }

    fun moveGroup(fromIndex: Int, toIndex: Int): Boolean =
        command { manager.moveGroup(fromIndex, toIndex) }

    fun addSymbol(groupId: String, symbol: SymbolEntry, index: Int? = null): Boolean =
        command { manager.addSymbol(groupId, symbol, index) }

    fun updateSymbol(groupId: String, symbol: SymbolEntry): Boolean =
        command { manager.updateSymbol(groupId, symbol) }

    fun removeSymbol(groupId: String, symbolId: Long): Boolean =
        command { manager.removeSymbol(groupId, symbolId) }

    fun moveSymbol(groupId: String, fromIndex: Int, toIndex: Int): Boolean =
        command { manager.moveSymbol(groupId, fromIndex, toIndex) }

    /** Atomically delete a selected set of symbols with one repository write. */
    fun removeSymbols(groupId: String, symbolIds: Set<Long>): Int? =
        commandResult { manager.removeSymbols(groupId, symbolIds) }

    /** Atomically copy selected symbols, assigning fresh stable IDs. */
    fun copySymbols(sourceGroupId: String, symbolIds: Set<Long>, targetGroupId: String): List<SymbolEntry>? =
        commandResult { manager.copySymbols(sourceGroupId, symbolIds, targetGroupId) }

    /** Atomically move selected symbols while preserving IDs when collision-free. */
    fun moveSymbolsToGroup(sourceGroupId: String, symbolIds: Set<Long>, targetGroupId: String): Boolean =
        command { manager.moveSymbolsToGroup(sourceGroupId, symbolIds, targetGroupId) }

    fun copySymbol(sourceGroupId: String, symbolId: Long, targetGroupId: String): SymbolEntry? =
        commandResult { manager.copySymbol(sourceGroupId, symbolId, targetGroupId) }

    fun moveSymbolToGroup(sourceGroupId: String, symbolId: Long, targetGroupId: String): Boolean =
        command { manager.moveSymbolToGroup(sourceGroupId, symbolId, targetGroupId) }

    /** Reorders a complete group by stable IDs in one validated transaction. */
    fun reorderSymbols(groupId: String, orderedIds: List<Long>): Boolean =
        command { manager.reorderSymbols(groupId, orderedIds) }

    /** Import is validated completely before the manager commits one snapshot. */
    fun importJson(json: String): Boolean =
        command { manager.replaceAll(SymbolGroupBundleCodec.decode(json)) }

    /** Commit an already-decoded and validated bundle, e.g. decoded off the UI thread. */
    fun importGroups(imported: List<SymbolGroup>): Boolean =
        command { manager.replaceAll(imported.toList()) }

    /** Exports the current committed snapshot using the canonical V2 bundle format. */
    fun exportJson(): String? {
        return runCatching { SymbolGroupBundleCodec.encode(groups) }
            .onSuccess { lastError = null }
            .onFailure { lastError = it.message ?: it::class.java.simpleName }
            .getOrNull()
    }

    private inline fun command(block: () -> Unit): Boolean {
        val result = runCatching(block)
        result.onSuccess {
            groups = manager.groups
            lastError = null
        }.onFailure {
            lastError = it.message ?: it::class.java.simpleName
        }
        return result.isSuccess
    }

    private inline fun <T> commandResult(block: () -> T): T? {
        var value: T? = null
        val success = command { value = block() }
        return value.takeIf { success }
    }
}

/** Creates one manager facade per repository identity and retains it across recompositions. */
@Composable
fun rememberSymbolDrawerManagerState(
    repository: SymbolGroupRepository
): SymbolDrawerManagerState = remember(repository) {
    SymbolDrawerManagerState(SymbolGroupManager(repository))
}
