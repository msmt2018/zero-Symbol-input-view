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
import androidx.compose.ui.unit.dp
import com.zerostudio.idea.widget.editor.symbolinput.v2.R
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.BuiltinSymbolActionCatalog
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

    val legacyOptions = remember {
        LegacySymbolActionDefinitions.values.mapIndexed { index, id ->
            SymbolActionPickerOption(
                key = "v1:$id",
                label = "[V1] " + LegacySymbolActionDefinitions.labels[index],
                legacyId = id
            )
        }
    }
    val builtinOptions = remember {
        BuiltinSymbolActionCatalog.options.map { option ->
            SymbolActionPickerOption(
                key = "v2:${option.commandId}",
                label = "[V2] " + option.label,
                builtin = option
            )
        }
    }
    val extensionOptions = remember(extensions) {
        extensions.map { extension ->
            SymbolActionPickerOption(
                key = "extension:${extension.id}",
                label = "[Extension] ${extension.title}",
                extension = extension
            )
        }
    }
    val commonOptions = legacyOptions + builtinOptions + extensionOptions
    val shortOptions = remember(commonOptions, preserveShort, existing) {
        buildList {
            if (preserveShort) {
                add(SymbolActionPickerOption("keep:short", "Keep current action", keepCurrent = true))
            }
            addAll(commonOptions)
        }
    }
    val longOptions = remember(commonOptions, preserveLong, existing) {
        buildList {
            if (preserveLong) {
                add(SymbolActionPickerOption("keep:long", "Keep current action", keepCurrent = true))
            }
            add(SymbolActionPickerOption("none", "None", none = true))
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

                Text(stringResource(R.string.symbol_drawer_177), style = MaterialTheme.typography.titleSmall)
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
                            ?: stringResource(R.string.symbol_drawer_050)) }
                    )
                }

                Text(stringResource(R.string.symbol_drawer_224), style = MaterialTheme.typography.titleSmall)
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
                            ?: stringResource(R.string.symbol_drawer_050)) }
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
                    validationError = stringResource(R.string.symbol_drawer_187)
                    return@TextButton
                }
                val shortOption = shortOptions.firstOrNull { it.key == shortKey }
                    ?: run {
                        validationError = stringResource(R.string.symbol_drawer_215)
                        return@TextButton
                    }
                val longOption = longOptions.firstOrNull { it.key == longKey }
                    ?: run {
                        validationError = stringResource(R.string.symbol_drawer_216)
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
