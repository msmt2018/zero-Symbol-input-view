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

private const val GENERAL_TAB_ID = "general"
