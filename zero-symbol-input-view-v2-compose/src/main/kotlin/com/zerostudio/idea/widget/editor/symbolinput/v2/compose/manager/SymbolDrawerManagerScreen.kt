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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
 * The screen owns only transient selection/expanded UI state. All mutations are
 * delegated as commands so a host can execute them through SymbolGroupManager
 * and its repository, keeping persistence and JSON compatibility out of UI.
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
    onImportJson: () -> Unit = {},
    onExportJson: () -> Unit = {}
) {
    var localSelectedGroupId by rememberSaveable { mutableStateOf(selectedGroupId ?: groups.firstOrNull()?.id) }
    val effectiveSelectedId = selectedGroupId ?: localSelectedGroupId
    val selectedGroup = remember(groups, effectiveSelectedId) {
        groups.firstOrNull { it.id == effectiveSelectedId }
    }
    LaunchedEffect(groups, selectedGroupId) {
        if (selectedGroupId == null && groups.none { it.id == localSelectedGroupId }) {
            localSelectedGroupId = groups.firstOrNull()?.id
        }
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
                        Text(
                            group.title,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
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
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.compose_symbols_in_group, selectedGroup.title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { onAddSymbol(selectedGroup.id) }) {
                        Text(stringResource(R.string.compose_add_symbol))
                    }
                }
            }
            items(selectedGroup.symbols, key = { "symbol:${selectedGroup.id}:${it.id}" }) { symbol ->
                val symbolIndex = selectedGroup.symbols.indexOfFirst { it.id == symbol.id }
                Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(symbol.label, style = MaterialTheme.typography.bodyLarge)
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
}
