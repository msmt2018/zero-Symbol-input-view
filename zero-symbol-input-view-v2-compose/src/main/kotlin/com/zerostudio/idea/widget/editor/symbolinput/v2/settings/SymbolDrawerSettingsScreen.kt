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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.zerostudio.idea.widget.editor.symbolinput.v2.compose.R
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupBundleCodec
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupRepository

/**
 * Immutable settings editor rendered entirely with Compose. The host persists
 * snapshots; local state prevents rapid slider changes from using stale values.
 */
@Composable
fun SymbolDrawerSettingsScreen(
    settings: SymbolDrawerSettings,
    onSettingsChange: (SymbolDrawerSettings) -> Unit,
    modifier: Modifier = Modifier,
    onReset: (() -> Unit)? = null,
    groupRepository: SymbolGroupRepository? = null
) {
    var current by remember(settings) { mutableStateOf(settings) }
    fun update(transform: (SymbolDrawerSettings) -> SymbolDrawerSettings) {
        current = transform(current)
        onSettingsChange(current)
    }

    var selectedTab by remember { mutableStateOf(0) }
    var importDialog by remember { mutableStateOf(false) }
    var importGroups by remember { mutableStateOf(false) }
    var importJson by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var pendingExportJson by remember { mutableStateOf<String?>(null) }
    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                val json = context.contentResolver.openInputStream(uri)?.use {
                    JsonDocumentIO.readUtf8Json(it)
                } ?: error("无法读取所选 JSON 文件")
                if (importGroups) {
                    val repository = requireNotNull(groupRepository)
                    repository.saveGroups(SymbolGroupBundleCodec.decode(json))
                } else {
                    val decoded = SymbolDrawerSettingsCodec.decodeStrict(json)
                    current = decoded
                    onSettingsChange(decoded)
                }
            }.onFailure { errorMessage = it.message ?: "导入失败" }
        }
    }
    val createDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val json = pendingExportJson
        if (uri != null && json != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    JsonDocumentIO.writeUtf8Json(it, json)
                } ?: error("无法写入所选文件")
            }.onFailure { errorMessage = it.message ?: "导出失败" }
        }
        pendingExportJson = null
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.compose_settings_title), style = MaterialTheme.typography.headlineSmall)
        TabRow(selectedTabIndex = selectedTab) {
            listOf("外观", "布局", "平板/大屏", "交互", "性能/搜索", "数据管理").forEachIndexed { index, label ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(label, maxLines = 1) })
            }
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedTab) {
                0 -> SettingsSection(stringResource(R.string.compose_settings_appearance)) {
                    EnumSetting(stringResource(R.string.compose_settings_theme), current.themeMode, ThemeMode.entries) { value ->
                        update { it.copy(themeMode = value) }
                    }
                    EnumSetting(stringResource(R.string.compose_settings_indicator), current.indicatorStyle, GroupIndicatorStyle.entries) { value ->
                        update { it.copy(indicatorStyle = value) }
                    }
                    IntSetting(stringResource(R.string.compose_settings_indicator_height), current.indicatorHeightDp, 0, 96) { value ->
                        update { it.copy(indicatorHeightDp = value) }
                    }
                    IntSetting(stringResource(R.string.compose_settings_label_size), current.symbolLabelTextSizeSp, 8, 32) { value ->
                        update { it.copy(symbolLabelTextSizeSp = value) }
                    }
                    SwitchSetting(stringResource(R.string.compose_settings_show_labels), current.showSymbolLabels) { value ->
                        update { it.copy(showSymbolLabels = value) }
                    }
                    SwitchSetting(stringResource(R.string.compose_settings_full_labels), current.showFullSymbolLabels) { value ->
                        update { it.copy(showFullSymbolLabels = value) }
                    }
                    SwitchSetting(stringResource(R.string.compose_settings_group_titles), current.showGroupTitles) { value ->
                        update { it.copy(showGroupTitles = value) }
                    }
                    SwitchSetting(stringResource(R.string.compose_settings_action_hints), current.showActionHints) { value ->
                        update { it.copy(showActionHints = value) }
                    }
                }
                1 -> SettingsSection(stringResource(R.string.compose_settings_layout)) {
                    IntSetting(stringResource(R.string.compose_settings_columns), current.columns, 1, 30) { value -> update { it.copy(columns = value) } }
                    IntSetting(stringResource(R.string.compose_settings_collapsed_rows), current.collapsedRows, 1, 16) { value -> update { it.copy(collapsedRows = value) } }
                    IntSetting(stringResource(R.string.compose_settings_cell_height), current.cellHeightDp, 6, 128) { value -> update { it.copy(cellHeightDp = value) } }
                    IntSetting(stringResource(R.string.compose_settings_horizontal_spacing), current.cellHorizontalSpacingDp, -24, 48) { value -> update { it.copy(cellHorizontalSpacingDp = value) } }
                    IntSetting(stringResource(R.string.compose_settings_vertical_spacing), current.cellVerticalSpacingDp, -24, 48) { value -> update { it.copy(cellVerticalSpacingDp = value) } }
                    IntSetting(stringResource(R.string.compose_settings_max_height), current.maxExpandedHeightDp, 80, 1800) { value -> update { it.copy(maxExpandedHeightDp = value) } }
                    IntSetting(stringResource(R.string.compose_settings_touch_target), current.minimumTouchTargetDp, 20, 112) { value -> update { it.copy(minimumTouchTargetDp = value) } }
                }
                2 -> SettingsSection("平板与大屏布局") {
                    EnumSetting(stringResource(R.string.compose_settings_large_screen), current.largeScreenLayoutMode, LargeScreenLayoutMode.entries) { value ->
                        update { it.copy(largeScreenLayoutMode = value) }
                    }
                    EnumSetting(stringResource(R.string.compose_settings_side_edge), current.sideDockEdge, SideDockEdge.entries) { value ->
                        update { it.copy(sideDockEdge = value) }
                    }
                }
                3 -> SettingsSection(stringResource(R.string.compose_settings_interaction)) {
                    IntSetting(stringResource(R.string.compose_settings_animation), current.animationDurationMs, 0, 1200) { value -> update { it.copy(animationDurationMs = value) } }
                    IntSetting(stringResource(R.string.compose_settings_fling), current.flingVelocityDpPerSecond, 100, 6000) { value -> update { it.copy(flingVelocityDpPerSecond = value) } }
                    IntSetting(stringResource(R.string.compose_settings_long_press), current.longPressTimeoutMs, 250, 1000) { value -> update { it.copy(longPressTimeoutMs = value) } }
                    EnumSetting(stringResource(R.string.compose_settings_haptics), current.hapticMode, HapticMode.entries) { value -> update { it.copy(hapticMode = value) } }
                    SwitchSetting(stringResource(R.string.compose_settings_reduce_motion), current.reduceMotion) { value -> update { it.copy(reduceMotion = value) } }
                    SwitchSetting(stringResource(R.string.compose_settings_auto_collapse), current.autoCollapseAfterAction) { value -> update { it.copy(autoCollapseAfterAction = value) } }
                    SwitchSetting(stringResource(R.string.compose_settings_remember_expansion), current.rememberExpandedState) { value -> update { it.copy(rememberExpandedState = value) } }
                }
                4 -> SettingsSection(stringResource(R.string.compose_settings_performance)) {
                    IntSetting(stringResource(R.string.compose_settings_page_cache), current.pageCacheLimit, 1, 9) { value -> update { it.copy(pageCacheLimit = value) } }
                    SwitchSetting(stringResource(R.string.compose_settings_prefetch), current.prefetchAdjacentGroups) { value -> update { it.copy(prefetchAdjacentGroups = value) } }
                    SwitchSetting(stringResource(R.string.compose_settings_search_payload), current.searchIncludesActionPayload) { value -> update { it.copy(searchIncludesActionPayload = value) } }
                }
                5 -> SettingsSection("数据备份与恢复") {
                    Text("设置与符号分组使用 JSON 格式备份；复制后可保存为文件，导入时会先完整校验数据。", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = {
                        clipboard.setText(AnnotatedString(SymbolDrawerSettingsCodec.encode(current)))
                    }, modifier = Modifier.fillMaxWidth()) { Text("复制设置 JSON") }
                    OutlinedButton(onClick = {
                        pendingExportJson = SymbolDrawerSettingsCodec.encode(current)
                        createDocument.launch("symbol-drawer-settings.json")
                    }, modifier = Modifier.fillMaxWidth()) { Text("导出设置文件") }
                    OutlinedButton(onClick = {
                        importGroups = false
                        openDocument.launch(arrayOf("application/json", "text/*"))
                    }, modifier = Modifier.fillMaxWidth()) { Text("导入设置文件") }
                    Button(
                        onClick = {
                            val repository = groupRepository
                            if (repository == null) errorMessage = "符号分组仓库不可用"
                            else runCatching { SymbolGroupBundleCodec.encode(repository.loadGroups()) }
                                .onSuccess { clipboard.setText(AnnotatedString(it)) }
                                .onFailure { errorMessage = it.message ?: "导出符号分组失败" }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("复制符号分组 JSON") }
                    OutlinedButton(
                        onClick = {
                            val repository = groupRepository
                            if (repository == null) errorMessage = "符号分组仓库不可用"
                            else runCatching { SymbolGroupBundleCodec.encode(repository.loadGroups()) }
                                .onSuccess { json ->
                                    pendingExportJson = json
                                    createDocument.launch("symbol-drawer-groups.json")
                                }
                                .onFailure { errorMessage = it.message ?: "导出符号分组失败" }
                        },
                        enabled = groupRepository != null,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("导出符号分组文件") }
                    OutlinedButton(
                        onClick = {
                            importGroups = true
                            openDocument.launch(arrayOf("application/json", "text/*"))
                        },
                        enabled = groupRepository != null,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("导入符号分组文件") }
                    OutlinedButton(onClick = {
                        importGroups = false
                        importJson = clipboard.getText()?.text.orEmpty()
                        importDialog = true
                    }, modifier = Modifier.fillMaxWidth()) { Text("从剪贴板导入设置 JSON") }
                    OutlinedButton(
                        onClick = {
                            importGroups = true
                            importJson = clipboard.getText()?.text.orEmpty()
                            importDialog = true
                        },
                        enabled = groupRepository != null,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("从剪贴板导入符号分组 JSON") }
                    if (onReset != null) {
                        OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.compose_settings_reset))
                        }
                    }
                }
            }
        }
        if (selectedTab != 5) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (onReset != null) {
                    OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.compose_settings_reset))
                    }
                }
                Button(onClick = { onSettingsChange(current) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.compose_settings_apply))
                }
            }
        }
    }
    if (importDialog) {
        AlertDialog(
            onDismissRequest = { importDialog = false },
            title = { Text(if (importGroups) "导入符号分组" else "导入设置") },
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
                    runCatching {
                        if (importGroups) {
                            val repository = requireNotNull(groupRepository)
                            val decoded = SymbolGroupBundleCodec.decode(importJson)
                            repository.saveGroups(decoded)
                        } else {
                            val decoded = SymbolDrawerSettingsCodec.decodeStrict(importJson)
                            current = decoded
                            onSettingsChange(decoded)
                        }
                    }.onFailure { errorMessage = it.message ?: "导入失败" }
                    if (errorMessage == null) importDialog = false
                }) { Text("导入") }
            },
            dismissButton = {
                TextButton(onClick = { importDialog = false }) { Text("取消") }
            }
        )
    }
    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("操作失败") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { errorMessage = null }) { Text("确定") } }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()
            content(this)
        }
    }
}

@Composable
private fun SwitchSetting(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun IntSetting(label: String, value: Int, minimum: Int, maximum: Int, onValue: (Int) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(value.toString(), style = MaterialTheme.typography.labelLarge)
        }
        Slider(
            value = value.toFloat().coerceIn(minimum.toFloat(), maximum.toFloat()),
            onValueChange = { onValue(it.toInt().coerceIn(minimum, maximum)) },
            valueRange = minimum.toFloat()..maximum.toFloat(),
            steps = (maximum - minimum - 1).coerceAtLeast(0)
        )
    }
}

@Composable
private fun <T : Enum<T>> EnumSetting(
    label: String,
    value: T,
    values: List<T>,
    onValue: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = { expanded = true }) {
            Text(value.name.replace('_', ' '))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            values.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name.replace('_', ' ')) },
                    onClick = {
                        expanded = false
                        onValue(option)
                    }
                )
            }
        }
    }
}
