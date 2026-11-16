/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 * Author: android_zero · https://github.com/msmt2018
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.compose.manager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.zerostudio.idea.widget.editor.symbolinput.v2.R
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup

/** Compose-native manager surface; mutations are explicit immutable snapshots. */
@Composable
fun SymbolDrawerManagerScreen(
    groups: List<SymbolGroup>,
    onSelectGroup: (String) -> Unit,
    onAddGroup: () -> Unit,
    onRenameGroup: (SymbolGroup) -> Unit,
    onDeleteGroup: (SymbolGroup) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.compose_group_manager_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = onAddGroup) { Text(stringResource(R.string.compose_add_group)) }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(groups, key = { it.id }) { group ->
                Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(group.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            TextButton(onClick = { onSelectGroup(group.id) }) { Text(stringResource(R.string.compose_select)) }
                            TextButton(onClick = { onRenameGroup(group) }) { Text(stringResource(R.string.compose_rename)) }
                            TextButton(onClick = { onDeleteGroup(group) }) { Text(stringResource(R.string.compose_delete)) }
                        }
                        Text(stringResource(R.string.compose_symbol_count, group.symbols.size), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
