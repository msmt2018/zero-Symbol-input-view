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

import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicBoolean

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
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup

/**
 * Compose-native symbol data manager. Persistence and JSON codecs remain in
 * the domain/data layer; every screen interaction is an immutable command.
 */
@Composable
fun SymbolDrawerManagerScreen(
    repository: SymbolGroupRepository,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
) {
    val manager = remember(repository) { SymbolGroupManager(repository) }
    val mainThreadHandler = remember { Handler(Looper.getMainLooper()) }
    var groups by remember(repository) { mutableStateOf(manager.groups) }
    var selectedGroupId by remember(repository) { mutableStateOf(groups.firstOrNull()?.id) }
    var groupDialog by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var editingGroupId by remember { mutableStateOf<String?>(null) }
    var symbolDialog by remember { mutableStateOf(false) }
    var editingSymbol by remember { mutableStateOf<SymbolEntry?>(null) }
    var symbolLabel by remember { mutableStateOf("") }
    var symbolActionText by remember { mutableStateOf("") }
    var importDialog by remember { mutableStateOf(false) }
    var importJson by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedSymbolIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var targetDialogOperation by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current

    fun refresh() {
        groups = manager.groups.toList()
        if (groups.none { it.id == selectedGroupId }) selectedGroupId = groups.firstOrNull()?.id
        val currentGroup = groups.firstOrNull { it.id == selectedGroupId }
        val validIds = currentGroup?.symbols?.mapTo(mutableSetOf()) { it.id }.orEmpty()
        selectedSymbolIds = selectedSymbolIds.intersect(validIds)
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
    DisposableEffect(repository, manager, mainThreadHandler) {
        val observable = repository as? ObservableSymbolGroupRepository
        val active = AtomicBoolean(true)
        val handle = observable?.observeGroups {
            // Repository implementations may notify from arbitrary threads.
            // Compose state and this screen's manager are confined to the main thread.
            if (active.get()) {
                mainThreadHandler.post {
                    if (active.get()) {
                        runCatching { manager.reload() }
                            .onSuccess { refresh() }
                            .onFailure { errorMessage = it.message ?: "Unable to reload symbol groups" }
                    }
                }
            }
        }
        onDispose {
            active.set(false)
            handle?.close()
        }
    }

    val selectedGroup = groups.firstOrNull { it.id == selectedGroupId }
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
                symbolLabel = ""
                symbolActionText = ""
                symbolDialog = true
            }) { Text(stringResource(R.string.compose_manager_add_symbol)) }
        }
        if (selectedGroup == null) {
            Text(stringResource(R.string.compose_manager_create_group_hint))
        } else {
            if (selectedGroup.symbols.isNotEmpty()) {
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
                itemsIndexed(selectedGroup.symbols, key = { _, symbol -> symbol.id }) { index, symbol ->
                    Card {
                        Row(
                            Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Checkbox(
                                checked = symbol.id in selectedSymbolIds,
                                onCheckedChange = { checked ->
                                    selectedSymbolIds = if (checked) selectedSymbolIds + symbol.id
                                        else selectedSymbolIds - symbol.id
                                }
                            )
                            Column(Modifier.weight(1f)) {
                                Text(symbol.label, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = symbol.shortAction.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(enabled = index > 0, onClick = {
                                runCommand { manager.moveSymbol(selectedGroup.id, index, index - 1) }
                            }) { Text("↑") }
                            TextButton(enabled = index < selectedGroup.symbols.lastIndex, onClick = {
                                runCommand { manager.moveSymbol(selectedGroup.id, index, index + 1) }
                            }) { Text("↓") }
                            TextButton(onClick = {
                                editingSymbol = symbol
                                symbolLabel = symbol.label
                                symbolActionText = (symbol.shortAction as? SymbolAction.InsertText)?.template.orEmpty()
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
        AlertDialog(
            onDismissRequest = { symbolDialog = false },
            title = {
                Text(
                    if (editingSymbol == null) stringResource(R.string.compose_manager_add_symbol)
                    else stringResource(R.string.compose_manager_edit_symbol)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = symbolLabel,
                        onValueChange = { symbolLabel = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.compose_manager_symbol_label)) }
                    )
                    OutlinedTextField(
                        value = symbolActionText,
                        onValueChange = { symbolActionText = it },
                        label = { Text(stringResource(R.string.compose_manager_insert_text)) }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val label = symbolLabel.trim()
                    if (label.isNotEmpty()) {
                        runCommand {
                            val old = editingSymbol
                            if (old == null) {
                                manager.addSymbol(
                                    selectedGroup.id,
                                    SymbolEntry(0L, label, SymbolAction.InsertText(symbolActionText))
                                )
                            } else {
                                manager.updateSymbol(
                                    selectedGroup.id,
                                    old.copy(label = label, shortAction = SymbolAction.InsertText(symbolActionText))
                                )
                            }
                        }
                        symbolDialog = false
                    }
                }) { Text(stringResource(R.string.compose_manager_save)) }
            },
            dismissButton = {
                TextButton(onClick = { symbolDialog = false }) { Text(stringResource(R.string.compose_manager_cancel)) }
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
