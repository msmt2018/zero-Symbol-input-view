/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 * Author: android_zero · https://github.com/msmt2018
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.compose.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.ThemeMode
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.GroupIndicatorStyle
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.HapticMode
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.LargeScreenLayoutMode
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SideDockEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.zerostudio.idea.widget.editor.symbolinput.v2.R
import com.zerostudio.idea.widget.editor.symbolinput.v2.compose.ComposeSymbolDrawerExtensions
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings

/**
 * Material 3 settings host. Built-in preferences remain available by default;
 * extension tabs and items are composed in deterministic priority/id order.
 */
@Composable
fun SymbolDrawerSettingsScreen(
    settings: SymbolDrawerSettings,
    onSettingsChange: (SymbolDrawerSettings) -> Unit,
    modifier: Modifier = Modifier,
    extensions: ComposeSymbolDrawerExtensions = ComposeSymbolDrawerExtensions()
) {
    val extensionTabs = remember(extensions.settingsTabs) {
        extensions.settingsTabs.sortedWith(
            compareByDescending<com.zerostudio.idea.widget.editor.symbolinput.v2.compose.ComposeSymbolDrawerSettingsTabExtension> { it.priority }
                .thenBy { it.id }
        )
    }
    val tabIds = remember(extensionTabs) { listOf(GENERAL_TAB_ID) + extensionTabs.map { it.id } }
    var selectedTabId by rememberSaveable { mutableStateOf(GENERAL_TAB_ID) }
    LaunchedEffect(tabIds) {
        if (selectedTabId !in tabIds) selectedTabId = GENERAL_TAB_ID
    }
    val selectedTabIndex = tabIds.indexOf(selectedTabId).coerceAtLeast(0)
    val selectedExtensions = remember(extensions.settingsItems, selectedTabId) {
        extensions.settingsItems
            .filter { it.tabId == selectedTabId }
            .sortedWith(
                compareByDescending<com.zerostudio.idea.widget.editor.symbolinput.v2.compose.ComposeSymbolDrawerSettingExtension> { it.priority }
                    .thenBy { it.id }
            )
    }

    Column(modifier) {
        if (extensionTabs.isNotEmpty()) {
            ScrollableTabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabId == GENERAL_TAB_ID,
                    onClick = { selectedTabId = GENERAL_TAB_ID },
                    text = { Text(stringResource(R.string.compose_settings_title)) }
                )
                extensionTabs.forEach { tab ->
                    Tab(
                        selected = selectedTabId == tab.id,
                        onClick = { selectedTabId = tab.id },
                        text = { Text(tab.title) }
                    )
                }
            }
        }

        if (selectedTabId == GENERAL_TAB_ID) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text(
                        stringResource(R.string.compose_settings_title),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                item {
                    SettingChoice(
                        title = stringResource(R.string.compose_setting_theme),
                        selected = settings.themeMode.name,
                        options = ThemeMode.values().map { it to it.name },
                        onSelected = { onSettingsChange(settings.copy(themeMode = it)) }
                    )
                }
                item {
                    SettingChoice(
                        title = stringResource(R.string.compose_setting_indicator_style),
                        selected = settings.indicatorStyle.name.replace('_', ' '),
                        options = GroupIndicatorStyle.values().map { it to it.name.replace('_', ' ') },
                        onSelected = { onSettingsChange(settings.copy(indicatorStyle = it)) }
                    )
                }
                item {
                    SettingChoice(
                        title = stringResource(R.string.compose_setting_haptics),
                        selected = settings.hapticMode.name,
                        options = HapticMode.values().map { it to it.name },
                        onSelected = { onSettingsChange(settings.copy(hapticMode = it)) }
                    )
                }
                item {
                    SettingChoice(
                        title = stringResource(R.string.compose_setting_large_screen_layout),
                        selected = settings.largeScreenLayoutMode.name,
                        options = LargeScreenLayoutMode.values().map { it to it.name },
                        onSelected = { onSettingsChange(settings.copy(largeScreenLayoutMode = it)) }
                    )
                }
                item {
                    SettingChoice(
                        title = stringResource(R.string.compose_setting_side_dock_edge),
                        selected = settings.sideDockEdge.name,
                        options = SideDockEdge.values().map { it to it.name },
                        onSelected = { onSettingsChange(settings.copy(sideDockEdge = it)) }
                    )
                }
                item {
                    SettingSlider(
                        title = stringResource(R.string.compose_setting_columns),
                        value = settings.columns.toFloat(),
                        range = 1f..30f,
                        valueLabel = settings.columns.toString(),
                        onValueChange = { onSettingsChange(settings.copy(columns = it.toInt().coerceIn(1, 30))) }
                    )
                }
                item {
                    SettingSlider(
                        title = stringResource(R.string.compose_setting_collapsed_rows),
                        value = settings.collapsedRows.toFloat(),
                        range = 1f..16f,
                        valueLabel = settings.collapsedRows.toString(),
                        onValueChange = { onSettingsChange(settings.copy(collapsedRows = it.toInt().coerceIn(1, 16))) }
                    )
                }
                item {
                    SettingSlider(
                        title = stringResource(R.string.compose_setting_cell_height),
                        value = settings.cellHeightDp.toFloat(),
                        range = 6f..128f,
                        valueLabel = "${settings.cellHeightDp} dp",
                        onValueChange = { onSettingsChange(settings.copy(cellHeightDp = it.toInt().coerceIn(6, 128))) }
                    )
                }
                item {
                    SettingSlider(
                        title = stringResource(R.string.compose_setting_max_height),
                        value = settings.maxExpandedHeightDp.toFloat(),
                        range = 80f..1800f,
                        valueLabel = "${settings.maxExpandedHeightDp} dp",
                        onValueChange = { onSettingsChange(settings.copy(maxExpandedHeightDp = it.toInt().coerceIn(80, 1800))) }
                    )
                }
                item {
                    SettingSlider(
                        title = stringResource(R.string.compose_setting_horizontal_spacing),
                        value = settings.cellHorizontalSpacingDp.toFloat(),
                        range = -24f..48f,
                        valueLabel = "${settings.cellHorizontalSpacingDp} dp",
                        onValueChange = { onSettingsChange(settings.copy(cellHorizontalSpacingDp = it.toInt().coerceIn(-24, 48))) }
                    )
                }
                item {
                    SettingSlider(
                        title = stringResource(R.string.compose_setting_vertical_spacing),
                        value = settings.cellVerticalSpacingDp.toFloat(),
                        range = -24f..48f,
                        valueLabel = "${settings.cellVerticalSpacingDp} dp",
                        onValueChange = { onSettingsChange(settings.copy(cellVerticalSpacingDp = it.toInt().coerceIn(-24, 48))) }
                    )
                }
                item {
                    SettingSlider(
                        title = stringResource(R.string.compose_setting_indicator_height),
                        value = settings.indicatorHeightDp.toFloat(),
                        range = 0f..96f,
                        valueLabel = "${settings.indicatorHeightDp} dp",
                        onValueChange = { onSettingsChange(settings.copy(indicatorHeightDp = it.toInt().coerceIn(0, 96))) }
                    )
                }
                item {
                    SettingSlider(
                        title = stringResource(R.string.compose_setting_label_size),
                        value = settings.symbolLabelTextSizeSp.toFloat(),
                        range = 8f..32f,
                        valueLabel = "${settings.symbolLabelTextSizeSp} sp",
                        onValueChange = { onSettingsChange(settings.copy(symbolLabelTextSizeSp = it.toInt().coerceIn(8, 32))) }
                    )
                }
                item {
                    SettingSlider(
                        title = stringResource(R.string.compose_setting_animation_duration),
                        value = settings.animationDurationMs.toFloat(),
                        range = 0f..1200f,
                        valueLabel = "${settings.animationDurationMs} ms",
                        onValueChange = { onSettingsChange(settings.copy(animationDurationMs = it.toInt().coerceIn(0, 1200))) }
                    )
                }
                item {
                    SettingSlider(
                        title = stringResource(R.string.compose_setting_fling_velocity),
                        value = settings.flingVelocityDpPerSecond.toFloat(),
                        range = 100f..6000f,
                        valueLabel = "${settings.flingVelocityDpPerSecond}",
                        onValueChange = { onSettingsChange(settings.copy(flingVelocityDpPerSecond = it.toInt().coerceIn(100, 6000))) }
                    )
                }
                item {
                    SettingSlider(
                        title = stringResource(R.string.compose_setting_long_press_timeout),
                        value = settings.longPressTimeoutMs.toFloat(),
                        range = 250f..1000f,
                        valueLabel = "${settings.longPressTimeoutMs} ms",
                        onValueChange = { onSettingsChange(settings.copy(longPressTimeoutMs = it.toInt().coerceIn(250, 1000))) }
                    )
                }
                item {
                    SettingSlider(
                        title = stringResource(R.string.compose_setting_page_cache),
                        value = settings.pageCacheLimit.toFloat(),
                        range = 1f..9f,
                        valueLabel = settings.pageCacheLimit.toString(),
                        onValueChange = { onSettingsChange(settings.copy(pageCacheLimit = it.toInt().coerceIn(1, 9))) }
                    )
                }
                item {
                    SettingSlider(
                        title = stringResource(R.string.compose_setting_touch_target),
                        value = settings.minimumTouchTargetDp.toFloat(),
                        range = 20f..112f,
                        valueLabel = "${settings.minimumTouchTargetDp} dp",
                        onValueChange = { onSettingsChange(settings.copy(minimumTouchTargetDp = it.toInt().coerceIn(20, 112))) }
                    )
                }
                item {
                    SettingSwitch(stringResource(R.string.compose_setting_group_titles), settings.showGroupTitles) {
                        onSettingsChange(settings.copy(showGroupTitles = it))
                    }
                }
                item {
                    SettingSwitch(stringResource(R.string.compose_setting_action_hints), settings.showActionHints) {
                        onSettingsChange(settings.copy(showActionHints = it))
                    }
                }
                item {
                    SettingSwitch(stringResource(R.string.compose_setting_auto_collapse), settings.autoCollapseAfterAction) {
                        onSettingsChange(settings.copy(autoCollapseAfterAction = it))
                    }
                }
                item {
                    SettingSwitch(stringResource(R.string.compose_setting_prefetch_groups), settings.prefetchAdjacentGroups) {
                        onSettingsChange(settings.copy(prefetchAdjacentGroups = it))
                    }
                }
                item {
                    SettingSwitch(stringResource(R.string.compose_setting_search_payload), settings.searchIncludesActionPayload) {
                        onSettingsChange(settings.copy(searchIncludesActionPayload = it))
                    }
                }
                item {
                    SettingSwitch(stringResource(R.string.compose_setting_remember_expansion), settings.rememberExpandedState) {
                        onSettingsChange(settings.copy(rememberExpandedState = it))
                    }
                }
                item {
                    SettingSwitch(stringResource(R.string.compose_setting_full_labels), settings.showFullSymbolLabels) {
                        onSettingsChange(settings.copy(showFullSymbolLabels = it))
                    }
                }
                item {
                    SettingSwitch(stringResource(R.string.compose_setting_show_labels), settings.showSymbolLabels) {
                        onSettingsChange(settings.copy(showSymbolLabels = it))
                    }
                }
                item {
                    SettingSwitch(stringResource(R.string.compose_setting_reduce_motion), settings.reduceMotion) {
                        onSettingsChange(settings.copy(reduceMotion = it))
                    }
                }
                items(selectedExtensions, key = { it.id }) { extension ->
                    extension.Content(settings, onSettingsChange, Modifier.fillMaxWidth())
                }
            }
        } else {
            val selectedTab = extensionTabs.firstOrNull { it.id == selectedTabId }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (selectedTab != null) {
                    item(key = "tab:${selectedTab.id}") {
                        selectedTab.Content(settings, onSettingsChange, Modifier.fillMaxWidth())
                    }
                }
                items(selectedExtensions, key = { it.id }) { extension ->
                    extension.Content(settings, onSettingsChange, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun SettingSlider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(valueLabel, style = MaterialTheme.typography.labelLarge)
            }
            Slider(value = value.coerceIn(range), onValueChange = onValueChange, valueRange = range)
        }
    }
}

@Composable
private fun SettingSwitch(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun <T> SettingChoice(
    title: String,
    selected: String,
    options: List<Pair<T, String>>,
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Column(horizontalAlignment = Alignment.End) {
                OutlinedButton(onClick = { expanded = true }) { Text(selected) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = { onSelected(value); expanded = false }
                        )
                    }
                }
            }
        }
    }
}

private const val GENERAL_TAB_ID = "general"
