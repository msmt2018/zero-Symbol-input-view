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
import androidx.compose.material3.Tab
import androidx.compose.material3.ScrollableTabRow
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.zerostudio.idea.widget.editor.symbolinput.v2.compose.R
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupBundleCodec
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
    // View V2 always follows the host system theme. Keep the same persisted
    // value instead of exposing independent light/dark controls in Compose.
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
                    feedbackMessage = "设置文件导入成功"
                }
            }.onFailure { errorMessage = it.message ?: "JSON 文件导入失败" }
        },
        onExported = { feedbackMessage = "JSON 文件导出成功" },
        onError = { errorMessage = it.message ?: "JSON 文件操作失败" }
    )
    val tabTitles = listOf("外观", "布局", "平板/大屏", "交互", "性能/搜索", "数据管理")
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
                    "配置符号抽屉外观、布局、交互和数据备份。设置变更会立即应用。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        ScrollableTabRow(
            selectedTabIndex = selectedTab.coerceIn(tabTitles.indices),
            edgePadding = 0.dp
        ) {
            tabTitles.forEachIndexed { index, label ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(label, maxLines = 1) }
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(tabScrollStates[selectedTab.coerceIn(tabTitles.indices)]),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                0 -> SettingsSection(stringResource(R.string.compose_settings_appearance)) {
                    EnumSetting(
                        stringResource(R.string.symbol_drawer_027),
                        current.themeMode,
                        listOf(ThemeMode.SYSTEM)
                    ) { value -> update { it.copy(themeMode = value) } }
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
                        "完整标签模式会保留符号的全部文本，并根据单元格宽度自动缩小字号。",
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

                2 -> SettingsSection("平板与大屏") {
                    Text(
                        "大屏布局由窗口尺寸类别、可用空间和宿主布局约束自动决定。符号控件在手机上保持底部停靠；在宽窗口中由自适应布局策略选择合适的内容区域。此页面与 View V2 一样仅提供布局说明，不增加独立的停靠边缘配置。",
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
                        "以下选项控制抽屉展开/关闭动画与触觉反馈。",
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
                        title = "设置备份与恢复",
                        description = "设置数据独立备份。支持复制 JSON、导出文件、粘贴 JSON 导入和选择文件导入。"
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    runCatching { SymbolDrawerSettingsCodec.encode(current) }
                                        .onSuccess {
                                            clipboard.setText(AnnotatedString(it))
                                            feedbackMessage = "设置 JSON 已复制"
                                        }
                                        .onFailure { errorMessage = it.message ?: "导出设置 JSON 失败" }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("复制 JSON") }
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        documentPicker.saveJson(
                                            "symbol-drawer-settings.json",
                                            SymbolDrawerSettingsCodec.encode(current)
                                        )
                                    }.onFailure { errorMessage = it.message ?: "导出设置文件失败" }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("导出文件") }
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
                            ) { Text("粘贴 JSON 导入") }
                            OutlinedButton(
                                onClick = {
                                    pendingFileImportGroups = false
                                    documentPicker.openJson()
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("从文件导入") }
                        }
                    }

                    SettingsDataCard(
                        title = "符号分组备份与恢复",
                        description = "符号分组数据与控件设置分开存储。文件导入会先校验 JSON，再由你确认替换当前分组。"
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val repository = groupRepository
                                    if (repository == null) {
                                        errorMessage = "符号分组仓库不可用"
                                    } else {
                                        runCatching { SymbolGroupBundleCodec.encode(repository.loadGroups()) }
                                            .onSuccess {
                                                clipboard.setText(AnnotatedString(it))
                                                feedbackMessage = "符号分组 JSON 已复制"
                                            }
                                            .onFailure { errorMessage = it.message ?: "导出符号分组 JSON 失败" }
                                    }
                                },
                                enabled = groupRepository != null,
                                modifier = Modifier.weight(1f)
                            ) { Text("复制 JSON") }
                            OutlinedButton(
                                onClick = {
                                    val repository = groupRepository
                                    if (repository == null) {
                                        errorMessage = "符号分组仓库不可用"
                                    } else {
                                        runCatching {
                                            documentPicker.saveJson(
                                                "symbol-drawer-groups.json",
                                                SymbolGroupBundleCodec.encode(repository.loadGroups())
                                            )
                                        }.onFailure { errorMessage = it.message ?: "导出符号分组文件失败" }
                                    }
                                },
                                enabled = groupRepository != null,
                                modifier = Modifier.weight(1f)
                            ) { Text("导出文件") }
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
                            ) { Text("粘贴 JSON 导入") }
                            OutlinedButton(
                                onClick = {
                                    pendingFileImportGroups = true
                                    documentPicker.openJson()
                                },
                                enabled = groupRepository != null,
                                modifier = Modifier.weight(1f)
                            ) { Text("从文件导入") }
                        }
                    }

                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("数据安全提示", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "导入操作会替换对应的设置或分组数据。请在导入前保留备份，并确认 JSON 来源可信。分组文件在覆盖前需要确认。",
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
                            feedbackMessage = "已恢复默认设置"
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
                    }.onFailure { errorMessage = it.message ?: "导入失败" }
                    if (errorMessage == null) {
                        feedbackMessage = if (importGroups) "符号分组导入成功" else "设置导入成功"
                        importDialog = false
                    }
                }) { Text("导入") }
            },
            dismissButton = {
                TextButton(onClick = { importDialog = false }) { Text("取消") }
            }
        )
    }

    feedbackMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { feedbackMessage = null },
            title = { Text("操作完成") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { feedbackMessage = null }) { Text("确定") }
            }
        )
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("操作失败") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) { Text("确定") }
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
                Text("$label：$value", style = MaterialTheme.typography.bodyMedium)
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
    var expanded by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Box {
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
    }
}
