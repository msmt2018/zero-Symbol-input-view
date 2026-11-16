/*
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.compose.manager

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.zerostudio.idea.widget.editor.symbolinput.v2.R
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupBundleCodec
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

/** Repository-backed manager with SAF backup and a built-in typed symbol editor. */
@Composable
fun SymbolDrawerManagerRoute(
    repository: SymbolGroupRepository,
    modifier: Modifier = Modifier,
    onAddSymbol: ((String) -> Unit)? = null,
    onEditSymbol: ((String, SymbolEntry) -> Unit)? = null
) {
    val state = rememberSymbolDrawerManagerState(repository)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedGroupId by rememberSaveable { mutableStateOf(state.groups.firstOrNull()?.id) }
    var groupTitle by rememberSaveable { mutableStateOf("") }
    var renameId by rememberSaveable { mutableStateOf<String?>(null) }
    var addingGroup by rememberSaveable { mutableStateOf(false) }
    var editingGroupId by remember { mutableStateOf<String?>(null) }
    var editingSymbol by remember { mutableStateOf<SymbolEntry?>(null) }
    var showSymbolEditor by remember { mutableStateOf(false) }
    var operationError by remember { mutableStateOf<String?>(null) }
    var exportContent by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        InputStreamReader(input, StandardCharsets.UTF_8).use { reader ->
                            val text = StringBuilder()
                            val buffer = CharArray(8192)
                            while (true) {
                                val count = reader.read(buffer)
                                if (count < 0) break
                                require(text.length + count <= SymbolGroupBundleCodec.MAX_BUNDLE_CHARS) { "Backup is too large" }
                                text.append(buffer, 0, count)
                            }
                            SymbolGroupBundleCodec.decode(text.toString())
                        }
                    } ?: error(context.getString(R.string.compose_read_backup_failed))
                }
            }.onSuccess { groups ->
                if (state.importGroups(groups)) selectedGroupId = state.groups.firstOrNull()?.id
                else operationError = state.lastError
            }.onFailure { operationError = it.message ?: context.getString(R.string.compose_import_failed) }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val content = exportContent
        exportContent = null
        if (uri != null && content != null) scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        OutputStreamWriter(output, StandardCharsets.UTF_8).use { it.write(content) }
                    } ?: error(context.getString(R.string.compose_create_backup_failed))
                }
            }.onFailure { operationError = it.message ?: context.getString(R.string.compose_write_backup_failed) }
        }
    }

    SymbolDrawerManagerScreen(
        groups = state.groups, selectedGroupId = selectedGroupId, modifier = modifier,
        onSelectGroup = { selectedGroupId = it },
        onAddGroup = { groupTitle = ""; addingGroup = true },
        onRenameGroup = { group -> renameId = group.id; groupTitle = group.title },
        onDeleteGroup = { group ->
            if (state.removeGroup(group.id) && selectedGroupId == group.id) selectedGroupId = state.groups.firstOrNull()?.id
            else if (state.lastError != null) operationError = state.lastError
        },
        onMoveGroup = { from, to -> if (!state.moveGroup(from, to)) operationError = state.lastError },
        onAddSymbol = { id ->
            if (onAddSymbol != null) onAddSymbol(id)
            else { editingGroupId = id; editingSymbol = null; showSymbolEditor = true }
        },
        onEditSymbol = { id, symbol ->
            if (onEditSymbol != null) onEditSymbol(id, symbol)
            else { editingGroupId = id; editingSymbol = symbol; showSymbolEditor = true }
        },
        onDeleteSymbol = { id, symbol -> if (!state.removeSymbol(id, symbol.id)) operationError = state.lastError },
        onMoveSymbol = { id, from, to -> if (!state.moveSymbol(id, from, to)) operationError = state.lastError },
        onImportJson = { importLauncher.launch(arrayOf("application/json", "text/json", "text/plain")) },
        onExportJson = {
            state.exportJson()?.let { exportContent = it; exportLauncher.launch(context.getString(R.string.compose_backup_file_name)) }
                ?: run { operationError = state.lastError }
        }
    )

    if (showSymbolEditor) {
        SymbolEntryEditorDialog(
            initial = editingSymbol,
            onDismiss = { showSymbolEditor = false },
            onSave = { label, icon, short, long ->
                val groupId = editingGroupId
                if (groupId == null) operationError = context.getString(R.string.compose_group_not_found)
                else {
                    val old = editingSymbol
                    val entry = SymbolEntry(
                        id = old?.id ?: 0L, label = label, shortAction = short, longAction = long,
                        icon = icon,
                        legacyShortActionId = old?.legacyShortActionId,
                        legacyShortText = old?.legacyShortText,
                        legacyLongActionId = old?.legacyLongActionId,
                        legacyLongText = old?.legacyLongText
                    )
                    val saved = if (old == null) state.addSymbol(groupId, entry) else state.updateSymbol(groupId, entry)
                    if (saved) showSymbolEditor = false else operationError = state.lastError
                }
            }
        )
    }

    if (addingGroup || renameId != null) {
        val renaming = renameId != null
        AlertDialog(
            onDismissRequest = { addingGroup = false; renameId = null },
            title = { Text(stringResource(if (renaming) R.string.compose_rename_group_title else R.string.compose_new_group_title)) },
            text = {
                OutlinedTextField(value = groupTitle, onValueChange = { groupTitle = it },
                    singleLine = true, label = { Text(stringResource(R.string.compose_group_name)) })
            },
            confirmButton = {
                TextButton(enabled = groupTitle.isNotBlank(), onClick = {
                    val id = renameId
                    val saved = if (id != null) state.renameGroup(id, groupTitle) else state.addGroup(groupTitle) != null
                    if (saved) {
                        if (id == null) selectedGroupId = state.groups.lastOrNull()?.id
                        addingGroup = false; renameId = null
                    } else operationError = state.lastError
                }) { Text(stringResource(if (renaming) R.string.compose_save else R.string.compose_create)) }
            },
            dismissButton = { TextButton(onClick = { addingGroup = false; renameId = null }) { Text(stringResource(R.string.compose_cancel)) } }
        )
    }
    val visibleError = operationError ?: state.lastError
    if (visibleError != null) {
        AlertDialog(
            onDismissRequest = { operationError = null },
            title = { Text(stringResource(R.string.compose_operation_failed)) },
            text = { Text(visibleError, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = { TextButton(onClick = { operationError = null }) { Text(stringResource(R.string.compose_ok)) } }
        )
    }
}
