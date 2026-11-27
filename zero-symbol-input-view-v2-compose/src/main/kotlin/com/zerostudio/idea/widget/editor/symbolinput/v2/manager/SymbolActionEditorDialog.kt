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
 * 1. GNU Affero General Public License v3.0 only (AGPL-3.0-only)
 * 2. GNU General Public License v3.0 only (GPL-3.0-only)
 * 3. Apache License 2.0 (Apache-2.0)
 *
 * The SPDX operator "AND" means that all listed license terms apply
 * cumulatively. See the repository LICENSE file for the full terms.
 *
 * Author: android_zero
 * GitHub: https://github.com/msmt2018
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.manager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zerostudio.idea.widget.editor.symbolinput.v2.compose.R
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.BuiltinSymbolActionCatalog
import com.zerostudio.idea.widget.editor.symbolinput.v2.manager.localizedBuiltinSymbolActionOptions
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.BuiltinSymbolActionOption
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.LegacySymbolActionAdapter
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.LegacySymbolActionDefinitions
import com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolDrawerActionExtension
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.LegacySymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry

private data class SymbolActionPickerOption(
    val key: String,
    val label: String,
    val legacyId: Int? = null,
    val builtin: BuiltinSymbolActionOption? = null,
    val extension: SymbolDrawerActionExtension? = null,
    val keepCurrent: Boolean = false,
    val none: Boolean = false
) {
    val hasPayload: Boolean
        get() = builtin?.hasPayload
            ?: extension?.payloadHint?.let { true }
            ?: (legacyId?.let(LegacySymbolActionDefinitions::hasTextPayload) == true)
}

/**
 * Compose-native replacement for the legacy Spinner/EditText action editor.
 *
 * The editor preserves unknown/current V2 actions, exposes the complete V1
 * compatibility palette, all built-in V2 commands and registered extensions,
 * and retains legacy numeric IDs and payloads for lossless persistence.
 */
@Composable
internal fun SymbolActionEditorDialog(
    existing: SymbolEntry?,
    extensions: List<SymbolDrawerActionExtension>,
    onDismiss: () -> Unit,
    onSave: (SymbolEntry) -> Unit
) {
    val shortLegacyIndex = remember(existing) {
        LegacyActionPickerResolver.legacyActionIndex(
            existing?.shortAction,
            existing?.legacyShortActionId,
            existing?.legacyShortText
        )
    }
    val longLegacyIndex = remember(existing) {
        existing?.longAction?.let {
            LegacyActionPickerResolver.legacyActionIndex(
                it,
                existing.legacyLongActionId,
                existing.legacyLongText
            )
        }
    }
    val preserveShort = existing?.shortAction != null && shortLegacyIndex == null
    val preserveLong = existing?.longAction != null && longLegacyIndex == null

    val legacyLabels = listOf(
        stringResource(R.string.legacy_action_insert_text),
        stringResource(R.string.legacy_action_delete_line),
        stringResource(R.string.legacy_action_clear_line),
        stringResource(R.string.legacy_action_convert_to_uppercase),
        stringResource(R.string.legacy_action_convert_to_lowercase),
        stringResource(R.string.legacy_action_indent_tab),
        stringResource(R.string.legacy_action_toggle_comment),
        stringResource(R.string.legacy_action_go_to_line_start),
        stringResource(R.string.legacy_action_go_to_line_end),
        stringResource(R.string.legacy_action_move_cursor_left),
        stringResource(R.string.legacy_action_move_cursor_right),
        stringResource(R.string.legacy_action_move_cursor_up),
        stringResource(R.string.legacy_action_move_cursor_down),
        stringResource(R.string.legacy_action_open_symbol_toolbar_manager),
        stringResource(R.string.legacy_action_go_to_document_start),
        stringResource(R.string.legacy_action_go_to_document_end),
        stringResource(R.string.legacy_action_select_all),
        stringResource(R.string.legacy_action_copy),
        stringResource(R.string.legacy_action_cut),
        stringResource(R.string.legacy_action_paste),
        stringResource(R.string.legacy_action_format_code),
        stringResource(R.string.legacy_action_copy_line),
        stringResource(R.string.legacy_action_cut_line),
        stringResource(R.string.legacy_action_duplicate_line),
        stringResource(R.string.legacy_action_replace_line),
        stringResource(R.string.legacy_action_enter_key),
        stringResource(R.string.legacy_action_tab_key),
        stringResource(R.string.legacy_action_shift_tab),
        stringResource(R.string.legacy_action_clipboard_copy),
        stringResource(R.string.legacy_action_clipboard_cut),
        stringResource(R.string.legacy_action_clipboard_paste),
        stringResource(R.string.legacy_action_undo),
        stringResource(R.string.legacy_action_redo)
    )
    val context = LocalContext.current
    val legacySourcePrefix = stringResource(R.string.compose_ui_action_source_v1)
    val builtinSourcePrefix = stringResource(R.string.compose_ui_action_source_v2)
    val extensionSourcePrefix = stringResource(R.string.compose_ui_action_source_extension)
    val keepCurrentActionLabel = stringResource(R.string.compose_ui_keep_current_action)
    val noActionLabel = stringResource(R.string.compose_ui_no_action)

    val legacyOptions = remember(legacyLabels, legacySourcePrefix) {
        LegacySymbolActionDefinitions.values.mapIndexed { index, id ->
            SymbolActionPickerOption(
                key = "v1:$id",
                label = legacySourcePrefix + legacyLabels[index],
                legacyId = id
            )
        }
    }
    val builtinOptions = remember(context, builtinSourcePrefix) {
        localizedBuiltinSymbolActionOptions(context).map { option ->
            SymbolActionPickerOption(
                key = "v2:${option.commandId}",
                label = builtinSourcePrefix + option.label,
                builtin = option
            )
        }
    }
    val extensionOptions = remember(extensions, extensionSourcePrefix) {
        extensions.map { extension ->
            SymbolActionPickerOption(
                key = "extension:${extension.id}",
                label = extensionSourcePrefix + extension.title,
                extension = extension
            )
        }
    }
    val commonOptions = legacyOptions + builtinOptions + extensionOptions
    val shortOptions = remember(commonOptions, preserveShort, existing, keepCurrentActionLabel) {
        buildList {
            if (preserveShort) {
                add(SymbolActionPickerOption("keep:short", keepCurrentActionLabel, keepCurrent = true))
            }
            addAll(commonOptions)
        }
    }
    val longOptions = remember(commonOptions, preserveLong, existing, keepCurrentActionLabel, noActionLabel) {
        buildList {
            if (preserveLong) {
                add(SymbolActionPickerOption("keep:long", keepCurrentActionLabel, keepCurrent = true))
            }
            add(SymbolActionPickerOption("none", noActionLabel, none = true))
            addAll(commonOptions)
        }
    }

    val shortInitialKey = when {
        preserveShort -> "keep:short"
        else -> shortLegacyIndex?.let { legacyOptions.getOrNull(it)?.key } ?: "v1:0"
    }
    val longInitialKey = when {
        preserveLong -> "keep:long"
        existing?.longAction == null -> "none"
        else -> longLegacyIndex?.let { legacyOptions.getOrNull(it)?.key } ?: "none"
    }

    var label by remember(existing) { mutableStateOf(existing?.label.orEmpty()) }
    var shortKey by remember(existing, shortInitialKey) { mutableStateOf(shortInitialKey) }
    var longKey by remember(existing, longInitialKey) { mutableStateOf(longInitialKey) }
    var shortPayload by remember(existing, shortLegacyIndex) {
        mutableStateOf(
            existing?.legacyShortText?.takeIf { shortLegacyIndex != null }
                ?: (existing?.shortAction as? SymbolAction.InsertText)?.template
                ?: (existing?.shortAction as? SymbolAction.EditorCommand)?.payload
                ?: (existing?.shortAction as? SymbolAction.Extension)?.payload
                ?: ""
        )
    }
    var longPayload by remember(existing, longLegacyIndex) {
        mutableStateOf(
            existing?.legacyLongText?.takeIf { longLegacyIndex != null }
                ?: (existing?.longAction as? SymbolAction.InsertText)?.template
                ?: (existing?.longAction as? SymbolAction.EditorCommand)?.payload
                ?: (existing?.longAction as? SymbolAction.Extension)?.payload
                ?: ""
        )
    }
    var shortMenuExpanded by remember { mutableStateOf(false) }
    var longMenuExpanded by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    val emptyLabelMessage = stringResource(R.string.symbol_settings_symbol_display_text_cannot_be_empty)
    val shortSelectionError = stringResource(R.string.symbol_interaction_choose_a_valid_tap_action)
    val longSelectionError = stringResource(R.string.symbol_interaction_choose_a_valid_long_press_action)

    val selectedShort = shortOptions.firstOrNull { it.key == shortKey } ?: shortOptions.first()
    val selectedLong = longOptions.firstOrNull { it.key == longKey } ?: longOptions.first()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existing == null) stringResource(R.string.compose_manager_add_symbol)
                else stringResource(R.string.compose_manager_edit_symbol)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.compose_manager_symbol_label)) }
                )

                Text(stringResource(R.string.symbol_interaction_tap_action), style = MaterialTheme.typography.titleSmall)
                Box {
                    OutlinedButton(
                        onClick = { shortMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(selectedShort.label) }
                    DropdownMenu(
                        expanded = shortMenuExpanded,
                        onDismissRequest = { shortMenuExpanded = false },
                        modifier = Modifier.heightIn(max = 360.dp)
                    ) {
                        shortOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    shortKey = option.key
                                    shortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                if (selectedShort.hasPayload) {
                    OutlinedTextField(
                        value = shortPayload,
                        onValueChange = { shortPayload = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        label = { Text(selectedShort.builtin?.payloadHint
                            ?: selectedShort.extension?.payloadHint
                            ?: stringResource(R.string.symbol_interaction_action_payload_text_used_by_insert_text_replace_line_et)) }
                    )
                }

                Text(stringResource(R.string.symbol_interaction_long_press_action_optional), style = MaterialTheme.typography.titleSmall)
                Box {
                    OutlinedButton(
                        onClick = { longMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(selectedLong.label) }
                    DropdownMenu(
                        expanded = longMenuExpanded,
                        onDismissRequest = { longMenuExpanded = false },
                        modifier = Modifier.heightIn(max = 360.dp)
                    ) {
                        longOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    longKey = option.key
                                    longMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                if (selectedLong.hasPayload) {
                    OutlinedTextField(
                        value = longPayload,
                        onValueChange = { longPayload = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        label = { Text(selectedLong.builtin?.payloadHint
                            ?: selectedLong.extension?.payloadHint
                            ?: stringResource(R.string.symbol_interaction_action_payload_text_used_by_insert_text_replace_line_et)) }
                    )
                }
                validationError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val display = label.trim()
                if (display.isEmpty()) {
                    validationError = emptyLabelMessage
                    return@TextButton
                }
                val shortOption = shortOptions.firstOrNull { it.key == shortKey }
                    ?: run {
                        validationError = shortSelectionError
                        return@TextButton
                    }
                val longOption = longOptions.firstOrNull { it.key == longKey }
                    ?: run {
                        validationError = longSelectionError
                        return@TextButton
                    }

                val shortId = if (shortOption.keepCurrent) existing?.legacyShortActionId else shortOption.legacyId
                val shortText = if (shortOption.hasPayload) shortPayload.takeIf { it.isNotEmpty() } else null
                val shortAction = when {
                    shortOption.keepCurrent -> existing!!.shortAction
                    shortOption.builtin != null -> SymbolAction.EditorCommand(shortOption.builtin.commandId, shortText)
                    shortOption.extension != null -> SymbolAction.Extension(shortOption.extension.id, shortText)
                    shortOption.legacyId != null -> LegacySymbolActionAdapter.convert(
                        LegacySymbolAction(shortOption.legacyId, shortText)
                    )
                    else -> SymbolAction.InsertText("")
                }
                val shortLegacyText = when {
                    shortOption.keepCurrent -> existing?.legacyShortText
                    shortId?.let(LegacySymbolActionDefinitions::hasTextPayload) == true -> shortText
                    else -> null
                }

                val longId = if (longOption.keepCurrent) existing?.legacyLongActionId else longOption.legacyId
                val longText = if (longOption.hasPayload) longPayload.takeIf { it.isNotEmpty() } else null
                val longAction = when {
                    longOption.keepCurrent -> existing?.longAction
                    longOption.none -> null
                    longOption.builtin != null -> SymbolAction.EditorCommand(longOption.builtin.commandId, longText)
                    longOption.extension != null -> SymbolAction.Extension(longOption.extension.id, longText)
                    longOption.legacyId != null -> LegacySymbolActionAdapter.convert(
                        LegacySymbolAction(longOption.legacyId, longText)
                    )
                    else -> null
                }
                val longLegacyText = when {
                    longOption.keepCurrent -> existing?.legacyLongText
                    longId?.let(LegacySymbolActionDefinitions::hasTextPayload) == true -> longText
                    else -> null
                }
                onSave(
                    SymbolEntry(
                        id = existing?.id ?: 0L,
                        label = display,
                        shortAction = shortAction,
                        longAction = longAction,
                        icon = existing?.icon,
                        legacyShortActionId = shortId,
                        legacyShortText = shortLegacyText,
                        legacyLongActionId = longId,
                        legacyLongText = longLegacyText
                    )
                )
            }) { Text(stringResource(R.string.compose_manager_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.compose_manager_cancel)) }
        }
    )
}
