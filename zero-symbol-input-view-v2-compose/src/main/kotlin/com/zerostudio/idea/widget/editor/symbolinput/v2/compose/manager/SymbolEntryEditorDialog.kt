/*
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.compose.manager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zerostudio.idea.widget.editor.symbolinput.v2.R
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.*

private enum class ActionKind {
    INSERT_TEXT, REPLACE_SELECTION, SURROUND_SELECTION, EDITOR_COMMAND,
    EXTENSION, MOVE_CURSOR, DELETE, TRANSFORM_SELECTION, CLIPBOARD, OPEN_MANAGER
}
private val actionKinds = ActionKind.values()

@Composable
private fun ActionKind.localizedName(): String = stringResource(when (this) {
    ActionKind.INSERT_TEXT -> R.string.compose_action_insert_text
    ActionKind.REPLACE_SELECTION -> R.string.compose_action_replace_selection
    ActionKind.SURROUND_SELECTION -> R.string.compose_action_surround_selection
    ActionKind.EDITOR_COMMAND -> R.string.compose_action_editor_command
    ActionKind.EXTENSION -> R.string.compose_action_extension
    ActionKind.MOVE_CURSOR -> R.string.compose_action_move_cursor
    ActionKind.DELETE -> R.string.compose_action_delete
    ActionKind.TRANSFORM_SELECTION -> R.string.compose_action_transform
    ActionKind.CLIPBOARD -> R.string.compose_action_clipboard
    ActionKind.OPEN_MANAGER -> R.string.compose_action_open_manager
})

private fun ActionKind.toAction(value: String): SymbolAction = when (this) {
    ActionKind.INSERT_TEXT -> SymbolAction.InsertText(value)
    ActionKind.REPLACE_SELECTION -> SymbolAction.ReplaceSelection(value)
    ActionKind.SURROUND_SELECTION -> {
        val split = value.indexOf('|')
        require(split >= 0) { "Surround action requires prefix|suffix" }
        SymbolAction.SurroundSelection(value.substring(0, split), value.substring(split + 1))
    }
    ActionKind.EDITOR_COMMAND -> SymbolAction.EditorCommand(value.substringBefore('|'), value.substringAfter('|', "").ifEmpty { null })
    ActionKind.EXTENSION -> SymbolAction.Extension(value.substringBefore('|'), value.substringAfter('|', "").ifEmpty { null })
    ActionKind.MOVE_CURSOR -> SymbolAction.MoveCursor(CursorTarget.valueOf(value))
    ActionKind.DELETE -> SymbolAction.Delete(DeleteKind.valueOf(value))
    ActionKind.TRANSFORM_SELECTION -> SymbolAction.TransformSelection(SelectionTransform.valueOf(value))
    ActionKind.CLIPBOARD -> SymbolAction.Clipboard(ClipboardOperation.valueOf(value))
    ActionKind.OPEN_MANAGER -> SymbolAction.OpenManager
}

private fun SymbolAction.toDraft(): Pair<ActionKind, String> = when (this) {
    is SymbolAction.InsertText -> ActionKind.INSERT_TEXT to template
    is SymbolAction.ReplaceSelection -> ActionKind.REPLACE_SELECTION to text
    is SymbolAction.SurroundSelection -> ActionKind.SURROUND_SELECTION to "$prefix|$suffix"
    is SymbolAction.EditorCommand -> ActionKind.EDITOR_COMMAND to (payload?.let { "$commandId|$it" } ?: commandId)
    is SymbolAction.Extension -> ActionKind.EXTENSION to (payload?.let { "$extensionId|$it" } ?: extensionId)
    is SymbolAction.MoveCursor -> ActionKind.MOVE_CURSOR to target.name
    is SymbolAction.Delete -> ActionKind.DELETE to kind.name
    is SymbolAction.TransformSelection -> ActionKind.TRANSFORM_SELECTION to transform.name
    is SymbolAction.Clipboard -> ActionKind.CLIPBOARD to operation.name
    SymbolAction.OpenManager -> ActionKind.OPEN_MANAGER to ""
    is SymbolAction.Composite -> ActionKind.INSERT_TEXT to ""
}

@Composable
internal fun SymbolEntryEditorDialog(
    initial: SymbolEntry?,
    onDismiss: () -> Unit,
    onSave: (String, String?, SymbolAction, SymbolAction?) -> Unit
) {
    val shortDraft = remember(initial) { initial?.shortAction?.toDraft() ?: (ActionKind.INSERT_TEXT to "") }
    val longDraft = remember(initial) { initial?.longAction?.toDraft() ?: (ActionKind.INSERT_TEXT to "") }
    var label by remember(initial) { mutableStateOf(initial?.label.orEmpty()) }
    var icon by remember(initial) { mutableStateOf(initial?.icon.orEmpty()) }
    var shortKind by remember(initial) { mutableStateOf(shortDraft.first) }
    var shortValue by remember(initial) { mutableStateOf(shortDraft.second) }
    var shortModified by remember(initial) { mutableStateOf(false) }
    var longEnabled by remember(initial) { mutableStateOf(initial?.longAction != null) }
    var longKind by remember(initial) { mutableStateOf(longDraft.first) }
    var longValue by remember(initial) { mutableStateOf(longDraft.second) }
    var longModified by remember(initial) { mutableStateOf(false) }
    var shortMenu by remember { mutableStateOf(false) }
    var longMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.compose_add_symbol_title else R.string.compose_edit_symbol_title)) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = label, onValueChange = { label = it },
                    modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.compose_symbol_label)) })
                OutlinedTextField(value = icon, onValueChange = { icon = it },
                    modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.compose_symbol_icon)) })
                Text(stringResource(R.string.compose_short_action), style = MaterialTheme.typography.titleSmall)
                OutlinedButton(onClick = { shortMenu = true }) { Text(shortKind.localizedName()) }
                DropdownMenu(expanded = shortMenu, onDismissRequest = { shortMenu = false }) {
                    actionKinds.forEach { kind ->
                        DropdownMenuItem(text = { Text(kind.localizedName()) }, onClick = {
                            shortKind = kind; shortValue = ""; shortModified = true; shortMenu = false
                        })
                    }
                }
                if (shortKind != ActionKind.OPEN_MANAGER) {
                    OutlinedTextField(value = shortValue, onValueChange = { shortValue = it; shortModified = true },
                        modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.compose_action_value)) },
                        supportingText = { Text(shortKind.hint()) })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = longEnabled, onCheckedChange = { longEnabled = it })
                    Text(stringResource(R.string.compose_enable_long_action))
                }
                if (longEnabled) {
                    Text(stringResource(R.string.compose_long_action), style = MaterialTheme.typography.titleSmall)
                    OutlinedButton(onClick = { longMenu = true }) { Text(longKind.localizedName()) }
                    DropdownMenu(expanded = longMenu, onDismissRequest = { longMenu = false }) {
                        actionKinds.forEach { kind ->
                            DropdownMenuItem(text = { Text(kind.localizedName()) }, onClick = {
                                longKind = kind; longValue = ""; longModified = true; longMenu = false
                            })
                        }
                    }
                    if (longKind != ActionKind.OPEN_MANAGER) {
                        OutlinedTextField(value = longValue, onValueChange = { longValue = it; longModified = true },
                            modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.compose_action_value)) },
                            supportingText = { Text(longKind.hint()) })
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = label.isNotBlank(), onClick = {
                runCatching {
                    val short = if (!shortModified && initial != null) initial.shortAction else shortKind.toAction(shortValue)
                    val long = if (!longEnabled) null
                        else if (!longModified && initial?.longAction != null) initial.longAction
                        else longKind.toAction(longValue)
                    onSave(label.trim(), icon.trim().ifEmpty { null }, short, long)
                }.onFailure { error = it.message ?: "Invalid action" }
            }) { Text(stringResource(R.string.compose_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.compose_cancel)) } }
    )
}

@Composable
private fun ActionKind.hint(): String = when (this) {
    ActionKind.SURROUND_SELECTION -> stringResource(R.string.compose_hint_surround)
    ActionKind.EDITOR_COMMAND -> stringResource(R.string.compose_hint_editor_command)
    ActionKind.EXTENSION -> stringResource(R.string.compose_hint_extension)
    ActionKind.MOVE_CURSOR -> CursorTarget.values().joinToString()
    ActionKind.DELETE -> DeleteKind.values().joinToString()
    ActionKind.TRANSFORM_SELECTION -> SelectionTransform.values().joinToString()
    ActionKind.CLIPBOARD -> ClipboardOperation.values().joinToString()
    else -> ""
}
