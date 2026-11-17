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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zerostudio.idea.widget.editor.symbolinput.v2.R

/**
 * Immutable settings editor rendered entirely with Compose. The host persists
 * snapshots; local state prevents rapid slider changes from using stale values.
 */
@Composable
fun SymbolDrawerSettingsScreen(
    settings: SymbolDrawerSettings,
    onSettingsChange: (SymbolDrawerSettings) -> Unit,
    modifier: Modifier = Modifier,
    onReset: (() -> Unit)? = null
) {
    var current by remember(settings) { mutableStateOf(settings) }
    fun update(transform: (SymbolDrawerSettings) -> SymbolDrawerSettings) {
        current = transform(current)
        onSettingsChange(current)
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.compose_settings_title), style = MaterialTheme.typography.headlineSmall)

        SettingsSection(stringResource(R.string.compose_settings_appearance)) {
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

        SettingsSection(stringResource(R.string.compose_settings_layout)) {
            IntSetting(stringResource(R.string.compose_settings_columns), current.columns, 1, 30) { value ->
                update { it.copy(columns = value) }
            }
            IntSetting(stringResource(R.string.compose_settings_collapsed_rows), current.collapsedRows, 1, 16) { value ->
                update { it.copy(collapsedRows = value) }
            }
            IntSetting(stringResource(R.string.compose_settings_cell_height), current.cellHeightDp, 6, 128) { value ->
                update { it.copy(cellHeightDp = value) }
            }
            IntSetting(stringResource(R.string.compose_settings_horizontal_spacing), current.cellHorizontalSpacingDp, -24, 48) { value ->
                update { it.copy(cellHorizontalSpacingDp = value) }
            }
            IntSetting(stringResource(R.string.compose_settings_vertical_spacing), current.cellVerticalSpacingDp, -24, 48) { value ->
                update { it.copy(cellVerticalSpacingDp = value) }
            }
            IntSetting(stringResource(R.string.compose_settings_max_height), current.maxExpandedHeightDp, 80, 1800) { value ->
                update { it.copy(maxExpandedHeightDp = value) }
            }
            EnumSetting(stringResource(R.string.compose_settings_large_screen), current.largeScreenLayoutMode, LargeScreenLayoutMode.entries) { value ->
                update { it.copy(largeScreenLayoutMode = value) }
            }
            EnumSetting(stringResource(R.string.compose_settings_side_edge), current.sideDockEdge, SideDockEdge.entries) { value ->
                update { it.copy(sideDockEdge = value) }
            }
        }

        SettingsSection(stringResource(R.string.compose_settings_interaction)) {
            IntSetting(stringResource(R.string.compose_settings_animation), current.animationDurationMs, 0, 1200) { value ->
                update { it.copy(animationDurationMs = value) }
            }
            IntSetting(stringResource(R.string.compose_settings_fling), current.flingVelocityDpPerSecond, 100, 6000) { value ->
                update { it.copy(flingVelocityDpPerSecond = value) }
            }
            IntSetting(stringResource(R.string.compose_settings_long_press), current.longPressTimeoutMs, 250, 1000) { value ->
                update { it.copy(longPressTimeoutMs = value) }
            }
            EnumSetting(stringResource(R.string.compose_settings_haptics), current.hapticMode, HapticMode.entries) { value ->
                update { it.copy(hapticMode = value) }
            }
            SwitchSetting(stringResource(R.string.compose_settings_reduce_motion), current.reduceMotion) { value ->
                update { it.copy(reduceMotion = value) }
            }
            SwitchSetting(stringResource(R.string.compose_settings_auto_collapse), current.autoCollapseAfterAction) { value ->
                update { it.copy(autoCollapseAfterAction = value) }
            }
            SwitchSetting(stringResource(R.string.compose_settings_remember_expansion), current.rememberExpandedState) { value ->
                update { it.copy(rememberExpandedState = value) }
            }
        }

        SettingsSection(stringResource(R.string.compose_settings_performance)) {
            IntSetting(stringResource(R.string.compose_settings_page_cache), current.pageCacheLimit, 1, 9) { value ->
                update { it.copy(pageCacheLimit = value) }
            }
            IntSetting(stringResource(R.string.compose_settings_touch_target), current.minimumTouchTargetDp, 20, 112) { value ->
                update { it.copy(minimumTouchTargetDp = value) }
            }
            SwitchSetting(stringResource(R.string.compose_settings_prefetch), current.prefetchAdjacentGroups) { value ->
                update { it.copy(prefetchAdjacentGroups = value) }
            }
            SwitchSetting(stringResource(R.string.compose_settings_search_payload), current.searchIncludesActionPayload) { value ->
                update { it.copy(searchIncludesActionPayload = value) }
            }
        }

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
