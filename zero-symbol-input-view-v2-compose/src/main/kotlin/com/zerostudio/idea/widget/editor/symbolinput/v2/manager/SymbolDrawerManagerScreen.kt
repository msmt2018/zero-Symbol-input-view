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

package com.zerostudio.idea.widget.editor.symbolinput.v2.manager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.zerostudio.idea.widget.editor.symbolinput.v2.R
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupBundleCodec
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupManager
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.ObservableSymbolGroupRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.manager.SymbolDrawerUsageState
import com.zerostudio.idea.widget.editor.symbolinput.v2.manager.SymbolManagerFilter
import com.zerostudio.idea.widget.editor.symbolinput.v2.manager.SymbolUsageRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolDrawerActionExtension
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import kotlinx.coroutines.launch

/**
 * Compose-native symbol data manager. Persistence and JSON codecs remain in
 * the domain/data layer; every screen interaction is an immutable command.
 */
@Composable
fun SymbolDrawerManagerScreen(
    repository: SymbolGroupRepository,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    availableActions: List<SymbolDrawerActionExtension> = emptyList(),
    usageRepository: SymbolUsageRepository? = null,
    searchIncludesActionPayload: Boolean = true
) {
    val manager = remember(repository) { SymbolGroupManager(repository) }
    val compositionScope = rememberCoroutineScope()
    var groups by remember(repository) { mutableStateOf(manager.groups) }
    var selectedGroupId by remember(repository) { mutableStateOf(groups.firstOrNull()?.id) }
    var groupDialog by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var editingGroupId by remember { mutableStateOf<String?>(null) }
    var symbolDialog by remember { mutableStateOf(false) }
    var editingSymbol by remember { mutableStateOf<SymbolEntry?>(null) }
    var importDialog by remember { mutableStateOf(false) }
    var importJson by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedSymbolIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val usageState = remember(usageRepository) { SymbolDrawerUsageState(usageRepository) }
    var usageSnapshot by remember(usageRepository) { mutableStateOf(usageState.snapshot()) }
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(SymbolManagerFilter.ALL) }
    var batchMode by remember { mutableStateOf(false) }
    var undoLabel by remember { mutableStateOf<String?>(null) }
    var undoAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var targetDialogOperation by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current

    fun refresh() {
        groups = manager.groups.toList()
        if (groups.none { it.id == selectedGroupId }) selectedGroupId = groups.firstOrNull()?.id
        val currentGroup = groups.firstOrNull { it.id == selectedGroupId }
        val validIds = groups.asSequence().flatMap { it.symbols.asSequence() }
            .mapTo(mutableSetOf()) { it.id }
        usageSnapshot = usageState.prune(validIds)
        val selectedGroupIds = currentGroup?.symbols?.mapTo(mutableSetOf()) { it.id }.orEmpty()
        selectedSymbolIds = selectedSymbolIds.intersect(selectedGroupIds)
    }
    fun runCommand(command: () -> Unit): Boolean {
        return runCatching(command)
            .onSuccess { refresh() }
            .onFailure { errorMessage = it.message ?: "Unknown error" }
            .isSuccess
    }

    // Keep an open manager screen coherent with writes performed by another
    // manager or host sharing this repository. The subscription belongs to
    // this Composition only; it does not require Activity/Fragment lifecycle APIs.
    DisposableEffect(repository, manager, compositionScope) {
        val observable = repository as? ObservableSymbolGroupRepository
        // The remembered scope uses the active Composition's UI dispatcher and
        // is cancelled automatically when this screen leaves Composition.
        // Repository callbacks may arrive on any thread; launch safely transfers
        // the reload and snapshot publication to the Compose-owned scope.
        val handle = observable?.observeGroups {
            compositionScope.launch {
                runCatching { manager.reload() }
                    .onSuccess { refresh() }
                    .onFailure { errorMessage = it.message ?: "Unable to reload symbol groups" }
            }
        }
        onDispose { handle?.close() }
    }

    val selectedGroup = groups.firstOrNull { it.id == selectedGroupId }
    val visibleSymbols = remember(selectedGroup, searchQuery, activeFilter, searchIncludesActionPayload, usageSnapshot) {
        selectedGroup?.let {
            usageState.filter(it.symbols, searchQuery, activeFilter, searchIncludesActionPayload)
        }.orEmpty()
    }
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.compose_manager_title), style = MaterialTheme.typography.headlineSmall)
            if (onClose != null) TextButton(onClick = onClose) { Text(stringResource(R.string.compose_manager_close)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                editingGroupId = null
                groupName = ""
                groupDialog = true
            }) { Text(stringResource(R.string.compose_manager_add_group)) }
            OutlinedButton(
                enabled = selectedGroup != null,
                onClick = { selectedGroup?.let { group -> runCommand { manager.duplicateGroup(group.id) } } }
            ) { Text(stringResource(R.string.symbol_drawer_071)) }
            OutlinedButton(onClick = {
                clipboard.setText(AnnotatedString(SymbolGroupBundleCodec.encode(groups)))
            }) { Text(stringResource(R.string.compose_manager_export)) }
            OutlinedButton(onClick = {
                importJson = clipboard.getText()?.text.orEmpty()
                importDialog = true
            }) { Text(stringResource(R.string.compose_manager_import)) }
        }
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.symbol_drawer_135)) }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { activeFilter = SymbolManagerFilter.ALL }) {
                Text(if (activeFilter == SymbolManagerFilter.ALL) "✓ All" else "All")
            }
            TextButton(onClick = { activeFilter = SymbolManagerFilter.FAVORITES }) {
                Text(if (activeFilter == SymbolManagerFilter.FAVORITES) "✓ Favorites" else "Favorites")
            }
            TextButton(onClick = { activeFilter = SymbolManagerFilter.RECENT }) {
                Text(if (activeFilter == SymbolManagerFilter.RECENT) "✓ Recent" else "Recent")
            }
            TextButton(onClick = {
                batchMode = !batchMode
                if (!batchMode) selectedSymbolIds = emptySet()
            }) { Text(if (batchMode) "Done" else "Select") }
        }
        HorizontalDivider()
        Text(stringResource(R.string.compose_manager_groups), style = MaterialTheme.typography.titleMedium)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(0.42f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(groups, key = { _, group -> group.id }) { index, group ->
                Card {
                    Row(
                        Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TextButton(onClick = {
                            selectedGroupId = group.id
                            selectedSymbolIds = emptySet()
                        }, modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (selectedGroupId == group.id) "● ${group.title}" else group.title,
                                color = if (selectedGroupId == group.id) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        TextButton(enabled = index > 0, onClick = {
                            runCommand { manager.moveGroup(index, index - 1) }
                        }) { Text("↑") }
                        TextButton(enabled = index < groups.lastIndex, onClick = {
                            runCommand { manager.moveGroup(index, index + 1) }
                        }) { Text("↓") }
                        TextButton(onClick = {
                            editingGroupId = group.id
                            groupName = group.title
                            groupDialog = true
                        }) { Text(stringResource(R.string.compose_manager_rename)) }
                        TextButton(onClick = { runCommand { manager.removeGroup(group.id) } }) {
                            Text(stringResource(R.string.compose_manager_delete))
                        }
                    }
                }
            }
        }
        HorizontalDivider()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(selectedGroup?.title ?: stringResource(R.string.compose_manager_no_group), style = MaterialTheme.typography.titleMedium)
            Button(enabled = selectedGroup != null, onClick = {
                editingSymbol = null
                symbolDialog = true
            }) { Text(stringResource(R.string.compose_manager_add_symbol)) }
        }
        if (selectedGroup == null) {
            Text(stringResource(R.string.compose_manager_create_group_hint))
        } else {
            if (selectedGroup.symbols.isNotEmpty() && batchMode) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        stringResource(R.string.symbol_drawer_102, selectedSymbolIds.size.toString()),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(
                        enabled = selectedSymbolIds.isNotEmpty(),
                        onClick = { targetDialogOperation = "copy" }
                    ) { Text(stringResource(R.string.symbol_drawer_070)) }
                    TextButton(
                        enabled = selectedSymbolIds.isNotEmpty() &&
                            groups.any { it.id != selectedGroup.id },
                        onClick = { targetDialogOperation = "move" }
                    ) { Text(stringResource(R.string.symbol_drawer_183)) }
                    TextButton(
                        enabled = selectedSymbolIds.isNotEmpty(),
                        onClick = {
                            if (runCommand { manager.removeSymbols(selectedGroup.id, selectedSymbolIds) }) {
                                selectedSymbolIds = emptySet()
                            }
                        }
                    ) { Text(stringResource(R.string.compose_manager_delete_selected)) }
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(0.58f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(visibleSymbols, key = { _, symbol -> symbol.id }) { index, symbol ->
                    Card {
                        Row(
                            Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (batchMode) {
                                Checkbox(
                                    checked = symbol.id in selectedSymbolIds,
                                    onCheckedChange = { checked ->
                                        selectedSymbolIds = if (checked) selectedSymbolIds + symbol.id
                                            else selectedSymbolIds - symbol.id
                                    }
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(symbol.label, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = symbol.shortAction.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(enabled = selectedGroup.symbols.indexOfFirst { it.id == symbol.id } > 0, onClick = {
                                val currentIndex = selectedGroup.symbols.indexOfFirst { it.id == symbol.id }
                                if (currentIndex > 0) runCommand { manager.moveSymbol(selectedGroup.id, currentIndex, currentIndex - 1) }
                            }) { Text("↑") }
                            TextButton(onClick = {
                                usageSnapshot = usageState.toggleFavorite(symbol.id)
                                undoAction = null
                                undoLabel = null
                            }) { Text(if (symbol.id in usageSnapshot.favorites) "★" else "☆") }
                            TextButton(enabled = index < visibleSymbols.lastIndex, onClick = {
                                val currentIndex = selectedGroup.symbols.indexOfFirst { it.id == symbol.id }
                                if (currentIndex >= 0 && currentIndex < selectedGroup.symbols.lastIndex) {
                                    runCommand { manager.moveSymbol(selectedGroup.id, currentIndex, currentIndex + 1) }
                                }
                            }) { Text("↓") }
                            TextButton(onClick = {
                                usageSnapshot = usageState.recordRecentlyUsed(symbol.id)
                                editingSymbol = symbol
                                symbolDialog = true
                            }) { Text(stringResource(R.string.compose_manager_edit)) }
                            TextButton(onClick = {
                                runCommand { manager.removeSymbol(selectedGroup.id, symbol.id) }
                            }) { Text(stringResource(R.string.compose_manager_delete)) }
                        }
                    }
                }
            }
        }
    }

    if (undoAction != null && undoLabel != null) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(undoLabel.orEmpty(), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = {
                runCatching { undoAction?.invoke() }
                    .onSuccess { refresh() }
                    .onFailure { errorMessage = it.message ?: "Unable to undo operation" }
                undoAction = null
                undoLabel = null
            }) { Text(stringResource(R.string.symbol_drawer_101)) }
        }
    }

    if (targetDialogOperation != null && selectedGroup != null) {
        AlertDialog(
            onDismissRequest = { targetDialogOperation = null },
            title = {
                Text(if (targetDialogOperation == "copy") stringResource(R.string.symbol_drawer_070) else stringResource(R.string.symbol_drawer_183))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    groups.filter { it.id != selectedGroup.id }.forEach { target ->
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val operation = targetDialogOperation
                                val completed = runCommand {
                                    if (operation == "copy") {
                                        manager.copySymbols(
                                            selectedGroup.id,
                                            selectedSymbolIds,
                                            target.id
                                        )
                                    } else {
                                        manager.moveSymbolsToGroup(
                                            selectedGroup.id,
                                            selectedSymbolIds,
                                            target.id
                                        )
                                    }
                                }
                                if (completed) {
                                    selectedSymbolIds = emptySet()
                                    targetDialogOperation = null
                                }
                            }
                        ) {
                            Text(target.title, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { targetDialogOperation = null }) {
                    Text(stringResource(R.string.compose_manager_cancel))
                }
            }
        )
    }

    if (groupDialog) {
        AlertDialog(
            onDismissRequest = { groupDialog = false },
            title = {
                Text(
                    if (editingGroupId == null) stringResource(R.string.compose_manager_add_group)
                    else stringResource(R.string.compose_manager_rename_group)
                )
            },
            text = {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.compose_manager_group_name)) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = groupName.trim()
                    if (name.isNotEmpty()) {
                        runCommand {
                            val currentId = editingGroupId
                            if (currentId == null) {
                                selectedGroupId = manager.addGroup(name).id
                            } else manager.renameGroup(currentId, name)
                        }
                        groupDialog = false
                    }
                }) { Text(stringResource(R.string.compose_manager_save)) }
            },
            dismissButton = {
                TextButton(onClick = { groupDialog = false }) { Text(stringResource(R.string.compose_manager_cancel)) }
            }
        )
    }

    if (symbolDialog && selectedGroup != null) {
        val targetGroupId = selectedGroup.id
        SymbolActionEditorDialog(
            existing = editingSymbol,
            extensions = availableActions,
            onDismiss = { symbolDialog = false },
            onSave = { symbol ->
                runCommand {
                    if (editingSymbol == null) {
                        manager.addSymbol(targetGroupId, symbol)
                    } else {
                        manager.updateSymbol(targetGroupId, symbol)
                    }
                }
                symbolDialog = false
            }
        )
    }

    if (importDialog) {
        AlertDialog(
            onDismissRequest = { importDialog = false },
            title = { Text(stringResource(R.string.compose_manager_import_title)) },
            text = {
                OutlinedTextField(
                    value = importJson,
                    onValueChange = { importJson = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    maxLines = 12,
                    label = { Text(stringResource(R.string.compose_manager_json)) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    runCommand { manager.replaceAll(SymbolGroupBundleCodec.decode(importJson)) }
                    importDialog = false
                }) { Text(stringResource(R.string.compose_manager_import)) }
            },
            dismissButton = {
                TextButton(onClick = { importDialog = false }) { Text(stringResource(R.string.compose_manager_cancel)) }
            }
        )
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text(stringResource(R.string.compose_manager_error)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) { Text(stringResource(R.string.compose_manager_ok)) }
            }
        )
    }
}
