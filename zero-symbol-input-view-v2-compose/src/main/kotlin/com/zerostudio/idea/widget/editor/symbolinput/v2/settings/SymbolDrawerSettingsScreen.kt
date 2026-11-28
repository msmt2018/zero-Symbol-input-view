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

package com.zerostudio.idea.widget.editor.symbolinput.v2.settings

import android.net.Uri

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.zerostudio.idea.widget.editor.symbolinput.v2.compose.R
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupBundleCodec
import com.zerostudio.idea.widget.editor.symbolinput.v2.ui.LiquidScrollableTabRow
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupRepository

/**
 * Immutable settings editor rendered entirely with Compose. The host persists
 * snapshots; local state prevents rapid slider changes from using stale values.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymbolDrawerSettingsScreen(
    settings: SymbolDrawerSettings,
    onSettingsChange: (SymbolDrawerSettings) -> Unit,
    modifier: Modifier = Modifier,
    onReset: (() -> Unit)? = null,
    groupRepository: SymbolGroupRepository? = null,
    requestOpenDocument: (((Uri?) -> Unit) -> Unit)? = null,
    requestCreateDocument: ((String, (Uri?) -> Unit) -> Unit)? = null
) {
    // The component inherits the active host Material 3 theme directly.
    // Keep SYSTEM only as a backwards-compatible persisted value; theme mode,
    // Dynamic Color, and palette selection remain responsibilities of the host.
    var current by remember(settings) {
        mutableStateOf(settings.copy(themeMode = ThemeMode.SYSTEM))
    }

    fun update(transform: (SymbolDrawerSettings) -> SymbolDrawerSettings) {
        val updated = transform(current).copy(themeMode = ThemeMode.SYSTEM)
        current = updated
        // V2 settings are persisted and applied as soon as a control changes;
        // there is no separate Apply/Save phase.
        onSettingsChange(updated)
    }

    var selectedTab by remember { mutableStateOf(0) }
    var importDialog by remember { mutableStateOf(false) }
    var importGroups by remember { mutableStateOf(false) }
    var importJson by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var pendingFileImportGroups by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val documentPicker = rememberJsonDocumentPicker(
        contentResolver = context.contentResolver,
        requestOpenDocument = { onResult ->
            val request = requestOpenDocument
                ?: error("The host must provide an OpenDocument launcher")
            request(onResult)
        },
        requestCreateDocument = { fileName, onResult ->
            val request = requestCreateDocument
                ?: error("The host must provide a CreateDocument launcher")
            request(fileName, onResult)
        },
        onImported = { json ->
            runCatching {
                if (pendingFileImportGroups) {
                    SymbolGroupBundleCodec.decode(json)
                    importGroups = true
                    importJson = json
                    errorMessage = null
                    importDialog = true
                } else {
                    val decoded = SymbolDrawerSettingsCodec.decodeStrict(json)
                        .copy(themeMode = ThemeMode.SYSTEM)
                    current = decoded
                    onSettingsChange(decoded)
                    feedbackMessage = context.getString(R.string.compose_ui_settings_file_imported)
                }
            }.onFailure { errorMessage = it.message ?: context.getString(R.string.compose_ui_json_import_failed) }
        },
        onExported = { feedbackMessage = context.getString(R.string.compose_ui_settings_json_exported) },
        onError = { errorMessage = it.message ?: context.getString(R.string.compose_ui_json_operation_failed) }
    )
    val tabTitles = listOf(stringResource(R.string.compose_ui_settings_tab_appearance), stringResource(R.string.compose_ui_settings_tab_layout), stringResource(R.string.compose_ui_settings_tab_large_screen), stringResource(R.string.compose_ui_settings_tab_interaction), stringResource(R.string.compose_ui_settings_tab_performance), stringResource(R.string.compose_ui_settings_tab_data))
    // Each View V2 settings tab retains its own scroll position.
    val tabScrollStates = remember {
        List(tabTitles.size) { ScrollState(0) }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    stringResource(R.string.compose_settings_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    stringResource(R.string.compose_ui_settings_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        LiquidScrollableTabRow(
            titles = tabTitles,
            selectedIndex = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier.fillMaxWidth()
        )

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(tabScrollStates[selectedTab.coerceIn(tabTitles.indices)]),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                0 -> SettingsSection(stringResource(R.string.compose_settings_appearance)) {
                    EnumSetting(
                        stringResource(R.string.compose_settings_indicator),
                        current.indicatorStyle,
                        GroupIndicatorStyle.entries
                    ) { value -> update { it.copy(indicatorStyle = value) } }
                    SwitchSetting(
                        stringResource(R.string.compose_settings_group_titles),
                        current.showGroupTitles
                    ) { value -> update { it.copy(showGroupTitles = value) } }
                    SwitchSetting(
                        stringResource(R.string.compose_settings_show_labels),
                        current.showSymbolLabels
                    ) { value -> update { it.copy(showSymbolLabels = value) } }
                    SwitchSetting(
                        stringResource(R.string.compose_settings_full_labels),
                        current.showFullSymbolLabels
                    ) { value -> update { it.copy(showFullSymbolLabels = value) } }
                    Text(
                        stringResource(R.string.compose_ui_settings_full_labels_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IntSetting(
                        stringResource(R.string.compose_settings_label_size),
                        current.symbolLabelTextSizeSp,
                        8,
                        32
                    ) { value -> update { it.copy(symbolLabelTextSizeSp = value) } }
                    SwitchSetting(
                        stringResource(R.string.compose_settings_action_hints),
                        current.showActionHints
                    ) { value -> update { it.copy(showActionHints = value) } }
                }

                1 -> SettingsSection(stringResource(R.string.compose_settings_layout)) {
                    IntSetting(
                        stringResource(R.string.compose_settings_columns),
                        current.columns,
                        1,
                        30
                    ) { value -> update { it.copy(columns = value) } }
                    IntSetting(
                        stringResource(R.string.compose_settings_collapsed_rows),
                        current.collapsedRows,
                        1,
                        16
                    ) { value -> update { it.copy(collapsedRows = value) } }
                    IntSetting(
                        stringResource(R.string.compose_settings_cell_height),
                        current.cellHeightDp,
                        6,
                        128
                    ) { value -> update { it.copy(cellHeightDp = value) } }
                    IntSetting(
                        stringResource(R.string.compose_settings_horizontal_spacing),
                        current.cellHorizontalSpacingDp,
                        -24,
                        48
                    ) { value -> update { it.copy(cellHorizontalSpacingDp = value) } }
                    IntSetting(
                        stringResource(R.string.compose_settings_vertical_spacing),
                        current.cellVerticalSpacingDp,
                        -24,
                        48
                    ) { value -> update { it.copy(cellVerticalSpacingDp = value) } }
                    IntSetting(
                        stringResource(R.string.compose_settings_indicator_height),
                        current.indicatorHeightDp,
                        0,
                        96
                    ) { value -> update { it.copy(indicatorHeightDp = value) } }
                    IntSetting(
                        stringResource(R.string.compose_settings_max_height),
                        current.maxExpandedHeightDp,
                        80,
                        1800
                    ) { value -> update { it.copy(maxExpandedHeightDp = value) } }
                    IntSetting(
                        stringResource(R.string.compose_settings_touch_target),
                        current.minimumTouchTargetDp,
                        20,
                        112
                    ) { value -> update { it.copy(minimumTouchTargetDp = value) } }
                }

                2 -> SettingsSection(stringResource(R.string.compose_ui_settings_large_screen_title)) {
                    Text(
                        stringResource(R.string.compose_ui_settings_large_screen_hint),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                3 -> SettingsSection(stringResource(R.string.compose_settings_interaction)) {
                    IntSetting(
                        stringResource(R.string.compose_settings_long_press),
                        current.longPressTimeoutMs,
                        250,
                        1000
                    ) { value -> update { it.copy(longPressTimeoutMs = value) } }
                    IntSetting(
                        stringResource(R.string.compose_settings_fling),
                        current.flingVelocityDpPerSecond,
                        100,
                        6000
                    ) { value -> update { it.copy(flingVelocityDpPerSecond = value) } }
                    SwitchSetting(
                        stringResource(R.string.compose_settings_auto_collapse),
                        current.autoCollapseAfterAction
                    ) { value -> update { it.copy(autoCollapseAfterAction = value) } }
                    SwitchSetting(
                        stringResource(R.string.compose_settings_remember_expansion),
                        current.rememberExpandedState
                    ) { value -> update { it.copy(rememberExpandedState = value) } }
                    Text(
                        stringResource(R.string.compose_ui_settings_interaction_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IntSetting(
                        stringResource(R.string.compose_settings_animation),
                        current.animationDurationMs,
                        0,
                        1200
                    ) { value -> update { it.copy(animationDurationMs = value) } }
                    SwitchSetting(
                        stringResource(R.string.compose_settings_reduce_motion),
                        current.reduceMotion
                    ) { value -> update { it.copy(reduceMotion = value) } }
                    EnumSetting(
                        stringResource(R.string.compose_settings_haptics),
                        current.hapticMode,
                        HapticMode.entries
                    ) { value -> update { it.copy(hapticMode = value) } }
                }

                4 -> SettingsSection(stringResource(R.string.compose_settings_performance)) {
                    IntSetting(
                        stringResource(R.string.compose_settings_page_cache),
                        current.pageCacheLimit,
                        1,
                        9
                    ) { value -> update { it.copy(pageCacheLimit = value) } }
                    SwitchSetting(
                        stringResource(R.string.compose_settings_prefetch),
                        current.prefetchAdjacentGroups
                    ) { value -> update { it.copy(prefetchAdjacentGroups = value) } }
                    SwitchSetting(
                        stringResource(R.string.compose_settings_search_payload),
                        current.searchIncludesActionPayload
                    ) { value -> update { it.copy(searchIncludesActionPayload = value) } }
                }

                5 -> {
                    SettingsDataCard(
                        title = stringResource(R.string.compose_ui_settings_backup_title),
                        description = stringResource(R.string.compose_ui_settings_backup_description)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    runCatching { SymbolDrawerSettingsCodec.encode(current) }
                                        .onSuccess {
                                            clipboard.setText(AnnotatedString(it))
                                            feedbackMessage = context.getString(R.string.compose_ui_settings_json_copied)
                                        }
                                        .onFailure { errorMessage = it.message ?: context.getString(R.string.compose_ui_settings_export_json_failed) }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text(stringResource(R.string.compose_ui_settings_copy_json)) }
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        documentPicker.saveJson(
                                            "symbol-drawer-settings.json",
                                            SymbolDrawerSettingsCodec.encode(current)
                                        )
                                    }.onFailure { errorMessage = it.message ?: context.getString(R.string.compose_ui_settings_export_file_failed) }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text(stringResource(R.string.compose_ui_settings_export_file)) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    pendingFileImportGroups = false
                                    importGroups = false
                                    importJson = clipboard.getText()?.text.orEmpty()
                                    errorMessage = null
                                    importDialog = true
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text(stringResource(R.string.compose_ui_settings_paste_import)) }
                            OutlinedButton(
                                onClick = {
                                    pendingFileImportGroups = false
                                    documentPicker.openJson()
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text(stringResource(R.string.compose_ui_settings_import_file)) }
                        }
                    }

                    SettingsDataCard(
                        title = stringResource(R.string.compose_ui_groups_backup_title),
                        description = stringResource(R.string.compose_ui_groups_backup_description)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val repository = groupRepository
                                    if (repository == null) {
                                        errorMessage = context.getString(R.string.compose_ui_groups_repository_unavailable)
                                    } else {
                                        runCatching { SymbolGroupBundleCodec.encode(repository.loadGroups()) }
                                            .onSuccess {
                                                clipboard.setText(AnnotatedString(it))
                                                feedbackMessage = context.getString(R.string.compose_ui_groups_json_copied)
                                            }
                                            .onFailure { errorMessage = it.message ?: context.getString(R.string.compose_ui_groups_export_json_failed) }
                                    }
                                },
                                enabled = groupRepository != null,
                                modifier = Modifier.weight(1f)
                            ) { Text(stringResource(R.string.compose_ui_settings_copy_json)) }
                            OutlinedButton(
                                onClick = {
                                    val repository = groupRepository
                                    if (repository == null) {
                                        errorMessage = context.getString(R.string.compose_ui_groups_repository_unavailable)
                                    } else {
                                        runCatching {
                                            documentPicker.saveJson(
                                                "symbol-drawer-groups.json",
                                                SymbolGroupBundleCodec.encode(repository.loadGroups())
                                            )
                                        }.onFailure { errorMessage = it.message ?: context.getString(R.string.compose_ui_groups_export_file_failed) }
                                    }
                                },
                                enabled = groupRepository != null,
                                modifier = Modifier.weight(1f)
                            ) { Text(stringResource(R.string.compose_ui_settings_export_file)) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    importGroups = true
                                    importJson = clipboard.getText()?.text.orEmpty()
                                    errorMessage = null
                                    importDialog = true
                                },
                                enabled = groupRepository != null,
                                modifier = Modifier.weight(1f)
                            ) { Text(stringResource(R.string.compose_ui_settings_paste_import)) }
                            OutlinedButton(
                                onClick = {
                                    pendingFileImportGroups = true
                                    documentPicker.openJson()
                                },
                                enabled = groupRepository != null,
                                modifier = Modifier.weight(1f)
                            ) { Text(stringResource(R.string.compose_ui_settings_import_file)) }
                        }
                    }

                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(stringResource(R.string.compose_ui_security_title), style = MaterialTheme.typography.titleSmall)
                            Text(
                                stringResource(R.string.compose_ui_security_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val defaults = SymbolDrawerSettings()
                            current = defaults
                            onSettingsChange(defaults)
                            onReset?.invoke()
                            feedbackMessage = context.getString(R.string.compose_ui_settings_restored)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.compose_settings_reset)) }
                }
            }
        }
    }

    if (importDialog) {
        AlertDialog(
            onDismissRequest = { importDialog = false },
            title = { Text(if (importGroups) stringResource(R.string.compose_ui_import_groups_title) else stringResource(R.string.compose_ui_import_settings_title)) },
            text = {
                OutlinedTextField(
                    value = importJson,
                    onValueChange = { importJson = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 10
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    errorMessage = null
                    runCatching {
                        if (importGroups) {
                            val repository = requireNotNull(groupRepository)
                            val decoded = SymbolGroupBundleCodec.decode(importJson)
                            repository.saveGroups(decoded)
                        } else {
                            val decoded = SymbolDrawerSettingsCodec.decodeStrict(importJson)
                                .copy(themeMode = ThemeMode.SYSTEM)
                            current = decoded
                            onSettingsChange(decoded)
                        }
                    }.onFailure { errorMessage = it.message ?: context.getString(R.string.compose_ui_import_failed) }
                    if (errorMessage == null) {
                        feedbackMessage = if (importGroups) context.getString(R.string.compose_ui_groups_imported_success) else context.getString(R.string.compose_ui_settings_imported_success)
                        importDialog = false
                    }
                }) { Text(stringResource(R.string.compose_ui_import)) }
            },
            dismissButton = {
                TextButton(onClick = { importDialog = false }) { Text(stringResource(R.string.compose_ui_cancel)) }
            }
        )
    }

    feedbackMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { feedbackMessage = null },
            title = { Text(stringResource(R.string.compose_ui_operation_completed)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { feedbackMessage = null }) { Text(stringResource(R.string.compose_ui_ok)) }
            }
        )
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text(stringResource(R.string.compose_ui_operation_failed)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) { Text(stringResource(R.string.compose_ui_ok)) }
            }
        )
    }
}

@Composable
private fun SettingsDataCard(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content(this)
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            title,
            modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 2.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        content(this)
    }
}

@Composable
private fun SwitchSetting(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun IntSetting(label: String, value: Int, minimum: Int, maximum: Int, onValue: (Int) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.compose_ui_value_summary, label, value), style = MaterialTheme.typography.bodyMedium)
            }
            Slider(
                value = value.toFloat().coerceIn(minimum.toFloat(), maximum.toFloat()),
                onValueChange = { onValue(it.toInt().coerceIn(minimum, maximum)) },
                valueRange = minimum.toFloat()..maximum.toFloat(),
                steps = (maximum - minimum - 1).coerceAtLeast(0)
            )
        }
    }
}

@Composable
private fun <T : Enum<T>> EnumSetting(
    label: String,
    value: T,
    values: List<T>,
    onValue: (T) -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Box {
                OutlinedButton(onClick = { expanded = true }) {
                    Text(enumSettingLabel(context, value))
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    values.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(enumSettingLabel(context, option)) },
                            onClick = {
                                expanded = false
                                onValue(option)
                            }
                        )
                    }
                }
            }
        }
    }
}


private fun enumSettingLabel(context: Context, value: Enum<*>): String = when (value) {
    is ThemeMode -> context.getString(when (value) {
        ThemeMode.SYSTEM -> R.string.compose_ui_theme_system
        ThemeMode.LIGHT -> R.string.compose_ui_theme_light
        ThemeMode.DARK -> R.string.compose_ui_theme_dark
    })
    is GroupIndicatorStyle -> context.getString(when (value) {
        GroupIndicatorStyle.TEXT, GroupIndicatorStyle.TEXT_TABS -> R.string.group_indicator_style_text_tabs
        GroupIndicatorStyle.PILL -> R.string.group_indicator_style_pill_slider
        GroupIndicatorStyle.DOTS -> R.string.group_indicator_style_page_dots
        GroupIndicatorStyle.UNDERLINE -> R.string.group_indicator_style_bottom_underline
        GroupIndicatorStyle.GOOEY_METABALL -> R.string.group_indicator_style_liquid_metaball
        GroupIndicatorStyle.ELASTIC_WORM -> R.string.group_indicator_style_elastic_worm
        GroupIndicatorStyle.SPRING_BLOB -> R.string.group_indicator_style_spring_blob
        GroupIndicatorStyle.LIQUID_MERGE -> R.string.group_indicator_style_liquid_merge
        GroupIndicatorStyle.VERNIER_TICKS -> R.string.group_indicator_style_precision_vernier_ticks
        GroupIndicatorStyle.MECHANICAL_RULER -> R.string.group_indicator_style_mechanical_ruler
        GroupIndicatorStyle.WHEEL_ROLLER -> R.string.group_indicator_style_3d_roller
        GroupIndicatorStyle.CAMERA_DIAL -> R.string.group_indicator_style_camera_dial
        GroupIndicatorStyle.RADIAL_RING -> R.string.group_indicator_style_radial_progress
        GroupIndicatorStyle.RADAR_SWEEP -> R.string.symbol_manager_radar_sweep
        GroupIndicatorStyle.HUD_ARC -> R.string.group_indicator_style_hud_arc_gauge
        GroupIndicatorStyle.ORBIT_DOTS -> R.string.group_indicator_style_orbit_dots
        GroupIndicatorStyle.ELASTIC_WAVE -> R.string.group_indicator_style_elastic_wave
        GroupIndicatorStyle.SINE_WAVE -> R.string.group_indicator_style_sine_wave
        GroupIndicatorStyle.BOUNCE_TRACK -> R.string.group_indicator_style_bounce_track
        GroupIndicatorStyle.SPRING_SLIDER -> R.string.group_indicator_style_spring_slider
        GroupIndicatorStyle.MORPHING_BAR -> R.string.group_indicator_style_morphing_progress_bar
        GroupIndicatorStyle.SEGMENTED_TRACK -> R.string.group_indicator_style_segmented_track
        GroupIndicatorStyle.PROGRESS_LINE -> R.string.group_indicator_style_continuous_progress_line
        GroupIndicatorStyle.DOUBLE_TRACK -> R.string.group_indicator_style_double_track
        GroupIndicatorStyle.MINIMAL_DASHES -> R.string.group_indicator_style_minimal_ticks
        GroupIndicatorStyle.SQUARE_TICKS -> R.string.group_indicator_style_square_ticks
        GroupIndicatorStyle.DIAMOND_MARKERS -> R.string.group_indicator_style_diamond_markers
        GroupIndicatorStyle.TRIANGLE_POINTER -> R.string.group_indicator_style_triangle_pointer
        GroupIndicatorStyle.HEXAGON_NODES -> R.string.group_indicator_style_hexagon_nodes
        GroupIndicatorStyle.GLOWING_NODES -> R.string.group_indicator_style_glowing_nodes
        GroupIndicatorStyle.PULSE_DOTS -> R.string.group_indicator_style_pulse_dots
        GroupIndicatorStyle.STACKED_CARDS -> R.string.group_indicator_style_stacked_cards
        GroupIndicatorStyle.MINI_PREVIEW -> R.string.group_indicator_style_mini_page_preview
        GroupIndicatorStyle.THUMBNAIL_STRIP -> R.string.group_indicator_style_thumbnail_strip
        GroupIndicatorStyle.NUMBER_BADGES -> R.string.group_indicator_style_number_badges
        GroupIndicatorStyle.VERTICAL_RAIL -> R.string.group_indicator_style_vertical_rail
        GroupIndicatorStyle.ARC_NODES -> R.string.group_indicator_style_arc_nodes
        GroupIndicatorStyle.FLUID_TRAIL -> R.string.group_indicator_style_fluid_trail
        GroupIndicatorStyle.DIGITAL_COUNTER -> R.string.group_indicator_style_digital_counter
        GroupIndicatorStyle.STEPPER -> R.string.group_indicator_style_stepper
    })
    is HapticMode -> context.getString(when (value) {
        HapticMode.OFF -> R.string.compose_ui_haptic_off
        HapticMode.SELECTION -> R.string.compose_ui_haptic_selection
        HapticMode.ALL -> R.string.compose_ui_haptic_all
    })
    is LargeScreenLayoutMode -> context.getString(when (value) {
        LargeScreenLayoutMode.AUTO -> R.string.symbol_layout_adapt_to_large_screens
        LargeScreenLayoutMode.BOTTOM -> R.string.symbol_layout_dock_to_bottom
        LargeScreenLayoutMode.SIDE -> R.string.symbol_layout_dock_to_side
    })
    is SideDockEdge -> context.getString(when (value) {
        SideDockEdge.LEFT -> R.string.symbol_manager_left
        SideDockEdge.RIGHT -> R.string.symbol_manager_right
    })
    else -> value.name.replace('_', ' ')
}
