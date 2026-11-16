/*
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.compose.manager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
    EXTENSION, MOVE_CURSOR, DELETE, TRANSFORM_SELECTION, CLIPBOARD, OPEN_MANAGER, COMPOSITE
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
    ActionKind.COMPOSITE -> R.string.compose_action_composite
})

private fun SymbolAction.kind(): ActionKind = when (this) {
    is SymbolAction.InsertText -> ActionKind.INSERT_TEXT
    is SymbolAction.ReplaceSelection -> ActionKind.REPLACE_SELECTION
    is SymbolAction.SurroundSelection -> ActionKind.SURROUND_SELECTION
    is SymbolAction.EditorCommand -> ActionKind.EDITOR_COMMAND
    is SymbolAction.Extension -> ActionKind.EXTENSION
    is SymbolAction.MoveCursor -> ActionKind.MOVE_CURSOR
    is SymbolAction.Delete -> ActionKind.DELETE
    is SymbolAction.TransformSelection -> ActionKind.TRANSFORM_SELECTION
    is SymbolAction.Clipboard -> ActionKind.CLIPBOARD
    SymbolAction.OpenManager -> ActionKind.OPEN_MANAGER
    is SymbolAction.Composite -> ActionKind.COMPOSITE
}

private fun SymbolAction.valueText(): String = when (this) {
    is SymbolAction.InsertText -> template
    is SymbolAction.ReplaceSelection -> text
    is SymbolAction.SurroundSelection -> "$prefix|$suffix"
    is SymbolAction.EditorCommand -> payload?.let { "$commandId|$it" } ?: commandId
    is SymbolAction.Extension -> payload?.let { "$extensionId|$it" } ?: extensionId
    is SymbolAction.MoveCursor -> target.name
    is SymbolAction.Delete -> kind.name
    is SymbolAction.TransformSelection -> transform.name
    is SymbolAction.Clipboard -> operation.name
    SymbolAction.OpenManager, is SymbolAction.Composite -> ""
}

private fun ActionKind.create(value: String = defaultValue()): SymbolAction = when (this) {
    ActionKind.INSERT_TEXT -> SymbolAction.InsertText(value)
    ActionKind.REPLACE_SELECTION -> SymbolAction.ReplaceSelection(value)
    ActionKind.SURROUND_SELECTION -> {
        val split = value.indexOf('|')
        require(split >= 0) { "Surround action requires prefix|suffix" }
        SymbolAction.SurroundSelection(value.substring(0, split), value.substring(split + 1))
    }
    ActionKind.EDITOR_COMMAND -> SymbolAction.EditorCommand(
        value.substringBefore('|'), value.substringAfter('|', "").ifEmpty { null }
    )
    ActionKind.EXTENSION -> SymbolAction.Extension(
        value.substringBefore('|'), value.substringAfter('|", "").ifEmpty { null }
    )
    ActionKind.MOVE_CURSOR -> SymbolAction.MoveCursor(CursorTarget.valueOf(value))
    ActionKind.DELETE -> SymbolAction.Delete(DeleteKind.valueOf(value))
    ActionKind.TRANSFORM_SELECTION -> SymbolAction.TransformSelection(SelectionTransform.valueOf(value))
    ActionKind.CLIPBOARD -> SymbolAction.Clipboard(ClipboardOperation.valueOf(value))
    ActionKind.OPEN_MANAGER -> SymbolAction.OpenManager
    ActionKind.COMPOSITE -> SymbolAction.Composite(emptyList())
}

private fun ActionKind.defaultValue(): String = when (this) {
    ActionKind.SURROUND_SELECTION -> "|"
    ActionKind.EDITOR_COMMAND -> "command"
    ActionKind.EXTENSION -> "extension"
    ActionKind.MOVE_CURSOR -> CursorTarget.values().first().name
    ActionKind.DELETE -> DeleteKind.values().first().name
    ActionKind.TRANSFORM_SELECTION -> SelectionTransform.values().first().name
    ActionKind.CLIPBOARD -> ClipboardOperation.values().first().name
    else -> ""
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

/**
 * Recursive typed-action editor. Composite children remain SymbolAction instances,
 * so nesting/order and all action-specific payloads survive edits and JSON export.
 */
@Composable
private fun SymbolActionEditor(
    action: SymbolAction,
    depth: Int = 0,
    onChange: (SymbolAction) -> Unit,
    onRemove: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null
) {
    var menuExpanded by remember(action) { mutableStateOf(false) }
    val kind = action.kind()
    Card(Modifier.fillMaxWidth().padding(start = (depth * 12).dp)) {
        Column(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { menuExpanded = true }) {
                    Text(kind.localizedName())
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    actionKinds.forEach { candidate ->
                        DropdownMenuItem(
                            text = { Text(candidate.localizedName()) },
                            onClick = {
                                onChange(candidate.create())
                                menuExpanded = false
                            }
                        )
                    }
                }
                if (onMoveUp != null) TextButton(onClick = onMoveUp) {
                    Text(stringResource(R.string.compose_action_move_up))
                }
                if (onMoveDown != null) TextButton(onClick = onMoveDown) {
                    Text(stringResource(R.string.compose_action_move_down))
                }
                if (onRemove != null) TextButton(onClick = onRemove) {
                    Text(stringResource(R.string.compose_action_remove))
                }
            }

            if (action !is SymbolAction.Composite && action != SymbolAction.OpenManager) {
                OutlinedTextField(
                    value = action.valueText(),
                    onValueChange = { value -> runCatching { kind.create(value) }.onSuccess(onChange) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.compose_action_value)) },
                    supportingText = { Text(kind.hint()) },
                    singleLine = false
                )
            }

            if (action is SymbolAction.Composite) {
                Text(
                    stringResource(R.string.compose_composite_children, action.actions.size),
                    style = MaterialTheme.typography.labelLarge
                )
                action.actions.forEachIndexed { index, child ->
                    key(index) {
                        SymbolActionEditor(
                            action = child,
                            depth = depth + 1,
                            onChange = { updated ->
                                onChange(action.copy(actions = action.actions.toMutableList().also {
                                    it[index] = updated
                                }))
                            },
                            onRemove = {
                                onChange(action.copy(actions = action.actions.toMutableList().also {
                                    it.removeAt(index)
                                }))
                            },
                            onMoveUp = if (index > 0) ({
                                onChange(action.copy(actions = action.actions.toMutableList().also {
                                    val item = it.removeAt(index); it.add(index - 1, item)
                                }))
                            }) else null,
                            onMoveDown = if (index < action.actions.lastIndex) ({
                                onChange(action.copy(actions = action.actions.toMutableList().also {
                                    val item = it.removeAt(index); it.add(index + 1, item)
                                }))
                            }) else null
                        )
                    }
                }
                var addMenu by remember(action) { mutableStateOf(false) }
                OutlinedButton(onClick = { addMenu = true }) {
                    Text(stringResource(R.string.compose_composite_add_action))
                }
                DropdownMenu(expanded = addMenu, onDismissRequest = { addMenu = false }) {
                    actionKinds.forEach { candidate ->
                        DropdownMenuItem(
                            text = { Text(candidate.localizedName()) },
                            onClick = {
                                onChange(action.copy(actions = action.actions + candidate.create()))
                                addMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SymbolEntryEditorDialog(
    initial: SymbolEntry?,
    onDismiss: () -> Unit,
    onSave: (String, String?, SymbolAction, SymbolAction?) -> Unit
) {
    var label by remember(initial) { mutableStateOf(initial?.label.orEmpty()) }
    var icon by remember(initial) { mutableStateOf(initial?.icon.orEmpty()) }
    var shortAction by remember(initial) {
        mutableStateOf(initial?.shortAction ?: SymbolAction.InsertText(""))
    }
    var longAction by remember(initial) { mutableStateOf(initial?.longAction) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (initial == null) R.string.compose_add_symbol_title
                else R.string.compose_edit_symbol_title))
        },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = label, onValueChange = { label = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.compose_symbol_label)) }
                )
                OutlinedTextField(
                    value = icon, onValueChange = { icon = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.compose_symbol_icon)) }
                )
                Text(stringResource(R.string.compose_short_action), style = MaterialTheme.typography.titleSmall)
                SymbolActionEditor(action = shortAction, onChange = { shortAction = it })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = longAction != null, onCheckedChange = {
                        longAction = if (it) longAction ?: SymbolAction.InsertText("") else null
                    })
                    Text(stringResource(R.string.compose_enable_long_action))
                }
                longAction?.let { current ->
                    Text(stringResource(R.string.compose_long_action), style = MaterialTheme.typography.titleSmall)
                    SymbolActionEditor(action = current, onChange = { longAction = it })
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = label.isNotBlank(), onClick = {
                runCatching {
                    onSave(label.trim(), icon.trim().ifEmpty { null }, shortAction, longAction)
                }.onFailure { error = it.message ?: "Invalid action" }
            }) { Text(stringResource(R.string.compose_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.compose_cancel)) }
        }
    )
}
