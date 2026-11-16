/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 * Author: android_zero · https://github.com/msmt2018
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.compose.manager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zerostudio.idea.widget.editor.symbolinput.v2.R
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup

/**
 * Compose-native symbol manager.
 *
 * Selection is scoped to the active group and keyed by stable symbol IDs.
 * Bulk commands are delegated as single manager transactions by the route.
 */
@Composable
fun SymbolDrawerManagerScreen(
    groups: List<SymbolGroup>,
    onSelectGroup: (String) -> Unit,
    onAddGroup: () -> Unit,
    onRenameGroup: (SymbolGroup) -> Unit,
    onDeleteGroup: (SymbolGroup) -> Unit,
    modifier: Modifier = Modifier,
    selectedGroupId: String? = null,
    onMoveGroup: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onAddSymbol: (groupId: String) -> Unit = {},
    onEditSymbol: (groupId: String, symbol: SymbolEntry) -> Unit = { _, _ -> },
    onDeleteSymbol: (groupId: String, symbol: SymbolEntry) -> Unit = { _, _ -> },
    onMoveSymbol: (groupId: String, fromIndex: Int, toIndex: Int) -> Unit = { _, _, _ -> },
    onDeleteSymbols: (groupId: String, symbolIds: Set<Long>) -> Unit = { _, _ -> },
    onCopySymbols: (sourceGroupId: String, symbolIds: Set<Long>, targetGroupId: String) -> Unit = { _, _, _ -> },
    onMoveSymbols: (sourceGroupId: String, symbolIds: Set<Long>, targetGroupId: String) -> Unit = { _, _, _ -> },
    onImportJson: () -> Unit = {},
    onExportJson: () -> Unit = {}
) {
    var localSelectedGroupId by rememberSaveable {
        mutableStateOf(selectedGroupId ?: groups.firstOrNull()?.id)
    }
    val effectiveSelectedId = selectedGroupId ?: localSelectedGroupId
    val selectedGroup = remember(groups, effectiveSelectedId) {
        groups.firstOrNull { it.id == effectiveSelectedId }
    }
    val selectedSymbolIds = remember(effectiveSelectedId) { mutableStateListOf<Long>() }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var targetAction by remember { mutableStateOf<BulkTargetAction?>(null) }

    LaunchedEffect(groups, selectedGroupId) {
        if (selectedGroupId == null && groups.none { it.id == localSelectedGroupId }) {
            localSelectedGroupId = groups.firstOrNull()?.id
        }
    }
    LaunchedEffect(selectedGroup?.id, selectedGroup?.symbols?.map { it.id }) {
        val validIds = selectedGroup?.symbols?.mapTo(mutableSetOf()) { it.id }.orEmpty()
        selectedSymbolIds.retainAll(validIds)
    }

    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "manager-header") {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.compose_group_manager_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onImportJson) { Text(stringResource(R.string.compose_import_json)) }
                TextButton(onClick = onExportJson) { Text(stringResource(R.string.compose_export_json)) }
                TextButton(onClick = onAddGroup) { Text(stringResource(R.string.compose_add_group)) }
            }
        }

        items(groups, key = { "group:${it.id}" }) { group ->
            val index = groups.indexOfFirst { it.id == group.id }
            val selected = group.id == effectiveSelectedId
            Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(group.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            if (effectiveSelectedId != group.id) selectedSymbolIds.clear()
                            localSelectedGroupId = group.id
                            onSelectGroup(group.id)
                        }) {
                            Text(if (selected) stringResource(R.string.compose_selected) else stringResource(R.string.compose_select))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.compose_symbol_count, group.symbols.size),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(enabled = index > 0, onClick = { onMoveGroup(index, index - 1) }) {
                            Text(stringResource(R.string.compose_move_up))
                        }
                        TextButton(enabled = index < groups.lastIndex, onClick = { onMoveGroup(index, index + 1) }) {
                            Text(stringResource(R.string.compose_move_down))
                        }
                        TextButton(onClick = { onRenameGroup(group) }) { Text(stringResource(R.string.compose_rename)) }
                        TextButton(onClick = { onDeleteGroup(group) }) { Text(stringResource(R.string.compose_delete)) }
                    }
                }
            }
        }

        if (selectedGroup != null) {
            item(key = "symbol-header:${selectedGroup.id}") {
                Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.compose_symbols_in_group, selectedGroup.title),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { onAddSymbol(selectedGroup.id) }) {
                            Text(stringResource(R.string.compose_add_symbol))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.compose_bulk_selected, selectedSymbolIds.size),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            enabled = selectedGroup.symbols.isNotEmpty(),
                            onClick = {
                                if (selectedSymbolIds.size == selectedGroup.symbols.size) selectedSymbolIds.clear()
                                else {
                                    selectedSymbolIds.clear()
                                    selectedSymbolIds.addAll(selectedGroup.symbols.map { it.id })
                                }
                            }
                        ) {
                            Text(stringResource(R.string.compose_bulk_select_all))
                        }
                        TextButton(enabled = selectedSymbolIds.isNotEmpty(), onClick = { selectedSymbolIds.clear() }) {
                            Text(stringResource(R.string.compose_bulk_clear))
                        }
                    }
                    if (selectedSymbolIds.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { showDeleteConfirmation = true }) {
                                Text(stringResource(R.string.compose_bulk_delete))
                            }
                            TextButton(
                                enabled = groups.any { it.id != selectedGroup.id },
                                onClick = { targetAction = BulkTargetAction.COPY }
                            ) { Text(stringResource(R.string.compose_bulk_copy)) }
                            TextButton(
                                enabled = groups.any { it.id != selectedGroup.id },
                                onClick = { targetAction = BulkTargetAction.MOVE }
                            ) { Text(stringResource(R.string.compose_bulk_move)) }
                        }
                    }
                }
            }
            items(selectedGroup.symbols, key = { "symbol:${selectedGroup.id}:${it.id}" }) { symbol ->
                val symbolIndex = selectedGroup.symbols.indexOfFirst { it.id == symbol.id }
                val checked = symbol.id in selectedSymbolIds
                Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { value ->
                                    if (value) {
                                        if (symbol.id !in selectedSymbolIds) selectedSymbolIds.add(symbol.id)
                                    } else selectedSymbolIds.remove(symbol.id)
                                }
                            )
                            Text(symbol.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(R.string.compose_symbol_id, symbol.id),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                enabled = symbolIndex > 0,
                                onClick = { onMoveSymbol(selectedGroup.id, symbolIndex, symbolIndex - 1) }
                            ) { Text(stringResource(R.string.compose_move_up)) }
                            TextButton(
                                enabled = symbolIndex < selectedGroup.symbols.lastIndex,
                                onClick = { onMoveSymbol(selectedGroup.id, symbolIndex, symbolIndex + 1) }
                            ) { Text(stringResource(R.string.compose_move_down)) }
                            TextButton(onClick = { onEditSymbol(selectedGroup.id, symbol) }) {
                                Text(stringResource(R.string.compose_edit))
                            }
                            TextButton(onClick = { onDeleteSymbol(selectedGroup.id, symbol) }) {
                                Text(stringResource(R.string.compose_delete))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation && selectedGroup != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.compose_bulk_delete_title)) },
            text = { Text(stringResource(R.string.compose_bulk_delete_message, selectedSymbolIds.size)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteSymbols(selectedGroup.id, selectedSymbolIds.toSet())
                    selectedSymbolIds.clear()
                    showDeleteConfirmation = false
                }) { Text(stringResource(R.string.compose_bulk_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.compose_cancel))
                }
            }
        )
    }

    val action = targetAction
    if (action != null && selectedGroup != null) {
        val targets = groups.filter { it.id != selectedGroup.id }
        AlertDialog(
            onDismissRequest = { targetAction = null },
            title = {
                Text(stringResource(
                    if (action == BulkTargetAction.COPY) R.string.compose_bulk_copy_target
                    else R.string.compose_bulk_move_target
                ))
            },
            text = {
                if (targets.isEmpty()) {
                    Text(stringResource(R.string.compose_bulk_no_target))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        targets.forEach { target ->
                            TextButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    val ids = selectedSymbolIds.toSet()
                                    if (action == BulkTargetAction.COPY) onCopySymbols(selectedGroup.id, ids, target.id)
                                    else onMoveSymbols(selectedGroup.id, ids, target.id)
                                    selectedSymbolIds.clear()
                                    targetAction = null
                                }
                            ) { Text(target.title) }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { targetAction = null }) {
                    Text(stringResource(R.string.compose_cancel))
                }
            }
        )
    }
}

private enum class BulkTargetAction { COPY, MOVE }
