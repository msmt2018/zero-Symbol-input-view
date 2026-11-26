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

package com.zerostudio.idea.widget.editor.symbolinput.v2.manager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Tab
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.zerostudio.idea.widget.editor.symbolinput.v2.compose.R
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupBundleCodec
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.LegacySymbolDataCodec
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupManager
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.ObservableSymbolGroupRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SharedPreferencesSymbolUsageRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.manager.SymbolManagerFilter
import com.zerostudio.idea.widget.editor.symbolinput.v2.manager.SymbolUsageRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolDrawerActionExtension
import com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolDrawerBuiltInFeatures
import com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolDrawerExtensionRegistry
import com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolDrawerExtensionSnapshot
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.SymbolDrawer
import com.zerostudio.idea.widget.editor.symbolinput.v2.rememberSymbolDrawerState
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Compose-native symbol data manager. Persistence and JSON codecs remain in
 * the domain/data layer; every screen interaction is an immutable command.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SymbolDrawerManagerScreen(
    repository: SymbolGroupRepository,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    availableActions: List<SymbolDrawerActionExtension> = emptyList(),
    usageRepository: SymbolUsageRepository? = null,
    searchIncludesActionPayload: Boolean = false,
    sharedUsageState: SymbolDrawerUsageState? = null,
    extensionRegistry: SymbolDrawerExtensionRegistry? = null
) {
    val manager = remember(repository) { SymbolGroupManager(repository) }
    val compositionScope = rememberCoroutineScope()
    var groups by remember(repository) { mutableStateOf(manager.groups) }
    var selectedGroupId by remember(repository) { mutableStateOf(groups.firstOrNull()?.id) }
    var groupDialog by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var editingGroupId by remember { mutableStateOf<String?>(null) }
    var symbolDialog by remember { mutableStateOf(false) }
    var editingSymbol by remember { mutableStateOf<SymbolEntry?>(null) }
    var importDialog by remember { mutableStateOf(false) }
    var importJson by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedSymbolIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var dragWorkingOrder by remember { mutableStateOf<List<SymbolEntry>?>(null) }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val effectiveUsageRepository = usageRepository ?: remember(context) {
        SharedPreferencesSymbolUsageRepository(context)
    }
    val ownedUsageState = remember(effectiveUsageRepository) {
        SymbolDrawerUsageState(effectiveUsageRepository)
    }
    val usageState = sharedUsageState ?: ownedUsageState
    var usageSnapshot by remember(usageState) { mutableStateOf(usageState.snapshot()) }
    var searchQuery by remember { mutableStateOf("") }
    var searchVisible by remember { mutableStateOf(false) }
    var extensionSnapshot by remember(extensionRegistry) {
        mutableStateOf(extensionRegistry?.snapshot() ?: SymbolDrawerExtensionSnapshot(0L, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyMap()))
    }
    val searchFeatureVisible = extensionSnapshot.builtInVisibility[SymbolDrawerBuiltInFeatures.SEARCH] ?: true
    val favoritesFeatureVisible = extensionSnapshot.builtInVisibility[SymbolDrawerBuiltInFeatures.FAVORITES] ?: true
    val recentFeatureVisible = extensionSnapshot.builtInVisibility[SymbolDrawerBuiltInFeatures.RECENT] ?: true
    val effectiveActions = availableActions.ifEmpty { extensionSnapshot.actions }
    var activeFilter by remember { mutableStateOf(SymbolManagerFilter.ALL) }
    var batchMode by remember { mutableStateOf(false) }
    var undoLabel by remember { mutableStateOf<String?>(null) }
    var undoAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var targetDialogOperation by remember { mutableStateOf<String?>(null) }
    var deleteGroupId by remember { mutableStateOf<String?>(null) }
    var groupMenuId by remember { mutableStateOf<String?>(null) }
    var symbolMenuId by remember { mutableStateOf<Long?>(null) }
    var copyGroupSourceId by remember { mutableStateOf<String?>(null) }
    var moreMenuExpanded by remember { mutableStateOf(false) }
    var importLegacyJson by remember { mutableStateOf(false) }
    var previewModeActive by remember { mutableStateOf(false) }
    var previewSnapshot by remember { mutableStateOf<List<SymbolGroup>?>(null) }
    var previewSelectedGroupId by remember { mutableStateOf<String?>(null) }
    val previewDrawerState = rememberSymbolDrawerState(initiallyExpanded = true)
    val undoMessage = stringResource(R.string.compose_manager_undo_message)

    fun refresh() {
        groups = manager.groups.toList()
        if (groups.none { it.id == selectedGroupId }) selectedGroupId = groups.firstOrNull()?.id
        val currentGroup = groups.firstOrNull { it.id == selectedGroupId }
        val validIds = groups.asSequence().flatMap { it.symbols.asSequence() }
            .mapTo(mutableSetOf()) { it.id }
        usageSnapshot = usageState.prune(validIds)
        val selectedGroupIds = currentGroup?.symbols?.mapTo(mutableSetOf()) { it.id }.orEmpty()
        selectedSymbolIds = selectedSymbolIds.intersect(selectedGroupIds)
    }
    fun runCommand(command: () -> Unit): Boolean {
        return runCatching(command)
            .onSuccess { refresh() }
            .onFailure { errorMessage = it.message ?: "Unknown error" }
            .isSuccess
    }
    fun runUndoable(command: () -> Unit): Boolean {
        val previousGroups = manager.groups.toList()
        val previousUsage = usageState.snapshot()
        val previousSelection = selectedGroupId
        val completed = runCommand(command)
        val usageRevisionAfterCommand = usageState.revision()
        if (completed) {
            undoLabel = undoMessage
            undoAction = {
                manager.replaceAll(previousGroups)
                usageSnapshot = if (usageState.revision() == usageRevisionAfterCommand) {
                    usageState.restore(previousUsage)
                } else {
                    usageState.snapshot()
                }
                selectedGroupId = previousSelection
            }
        }
        return completed
    }

    DisposableEffect(extensionRegistry) {
        val handle = extensionRegistry?.observe(emitCurrent = true) { snapshot ->
            compositionScope.launch {
                extensionSnapshot = snapshot
                val visibility = snapshot.builtInVisibility
                if (visibility[SymbolDrawerBuiltInFeatures.SEARCH] == false) {
                    searchVisible = false
                    searchQuery = ""
                }
                if (visibility[SymbolDrawerBuiltInFeatures.FAVORITES] == false &&
                    activeFilter == SymbolManagerFilter.FAVORITES
                ) activeFilter = SymbolManagerFilter.ALL
                if (visibility[SymbolDrawerBuiltInFeatures.RECENT] == false &&
                    activeFilter == SymbolManagerFilter.RECENT
                ) activeFilter = SymbolManagerFilter.ALL
            }
        }
        onDispose { handle?.close() }
    }

    DisposableEffect(usageState) {
        val handle = usageState.observe { snapshot ->
            compositionScope.launch { usageSnapshot = snapshot }
        }
        onDispose { handle.close() }
    }

    // Keep an open manager screen coherent with writes performed by another
    // manager or host sharing this repository. The subscription belongs to
    // this Composition only; it does not require Activity/Fragment lifecycle APIs.
    DisposableEffect(repository, manager, compositionScope) {
        val observable = repository as? ObservableSymbolGroupRepository
        // The remembered scope uses the active Composition's UI dispatcher and
        // is cancelled automatically when this screen leaves Composition.
        // Repository callbacks may arrive on any thread; launch safely transfers
        // the reload and snapshot publication to the Compose-owned scope.
        val handle = observable?.observeGroups {
            compositionScope.launch {
                runCatching { manager.reload() }
                    .onSuccess { refresh() }
                    .onFailure { errorMessage = it.message ?: "Unable to reload symbol groups" }
            }
        }
        onDispose { handle?.close() }
    }

    val selectedGroup = groups.firstOrNull { it.id == selectedGroupId }
    Column(
        modifier = modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // View V2's manager shell: search and overflow actions in one compact
        // header, followed by group tabs, search and the active symbol list.
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            if (searchFeatureVisible) {
                OutlinedButton(onClick = {
                    searchVisible = !searchVisible
                    if (!searchVisible) searchQuery = ""
                }) {
                    Text(stringResource(if (searchVisible) R.string.symbol_drawer_139 else R.string.symbol_drawer_132))
                }
            }
            Box {
                OutlinedButton(onClick = { moreMenuExpanded = true }) {
                    Text(stringResource(R.string.compose_manager_more))
                }
                DropdownMenu(expanded = moreMenuExpanded, onDismissRequest = { moreMenuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.compose_manager_add_group)) },
                        onClick = {
                            moreMenuExpanded = false
                            editingGroupId = null
                            groupName = ""
                            groupDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.compose_manager_add_symbol)) },
                        enabled = selectedGroup != null,
                        onClick = {
                            moreMenuExpanded = false
                            editingSymbol = null
                            symbolDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (batchMode) stringResource(R.string.compose_manager_batch_done)
                                else stringResource(R.string.compose_manager_batch_select)
                            )
                        },
                        onClick = {
                            moreMenuExpanded = false
                            if (batchMode) selectedSymbolIds = emptySet()
                            batchMode = !batchMode
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.compose_manager_filter_all)) },
                        onClick = {
                            moreMenuExpanded = false
                            activeFilter = SymbolManagerFilter.ALL
                        }
                    )
                    if (favoritesFeatureVisible) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.compose_manager_filter_favorites)) },
                            onClick = {
                                moreMenuExpanded = false
                                activeFilter = SymbolManagerFilter.FAVORITES
                            }
                        )
                    }
                    if (recentFeatureVisible) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.compose_manager_filter_recent)) },
                            onClick = {
                                moreMenuExpanded = false
                                activeFilter = SymbolManagerFilter.RECENT
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.symbol_drawer_071)) },
                        enabled = selectedGroup != null,
                        onClick = {
                            moreMenuExpanded = false
                            selectedGroup?.let { group ->
                                runCatching { manager.duplicateGroup(group.id) }
                                    .onSuccess { duplicate ->
                                        selectedGroupId = duplicate.id
                                        refresh()
                                    }
                                    .onFailure { errorMessage = it.message ?: "Unable to duplicate group" }
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.compose_manager_export)) },
                        onClick = {
                            moreMenuExpanded = false
                            runCatching { SymbolGroupBundleCodec.encode(groups) }
                                .onSuccess { clipboard.setText(AnnotatedString(it)) }
                                .onFailure { errorMessage = it.message ?: "Unable to export groups" }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.compose_manager_import)) },
                        onClick = {
                            moreMenuExpanded = false
                            importLegacyJson = false
                            importJson = clipboard.getText()?.text.orEmpty()
                            importDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.compose_manager_export_legacy)) },
                        onClick = {
                            moreMenuExpanded = false
                            runCatching { LegacySymbolDataCodec.encode(groups) }
                                .onSuccess { clipboard.setText(AnnotatedString(it)) }
                                .onFailure { errorMessage = it.message ?: "Legacy export is not supported for this data" }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.compose_manager_import_legacy)) },
                        onClick = {
                            moreMenuExpanded = false
                            importLegacyJson = true
                            importJson = clipboard.getText()?.text.orEmpty()
                            importDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (previewModeActive) R.string.compose_manager_finish_preview
                                    else R.string.compose_manager_preview
                                )
                            )
                        },
                        onClick = {
                            moreMenuExpanded = false
                            if (previewModeActive) {
                                previewModeActive = false
                                previewSnapshot = null
                                previewSelectedGroupId = null
                            } else {
                                previewSnapshot = groups.map { it.copy(symbols = it.symbols.toList()) }
                                previewSelectedGroupId = selectedGroupId
                                previewModeActive = true
                            }
                        }
                    )
                }
            }
            if (onClose != null) {
                TextButton(onClick = onClose) {
                    Text(stringResource(R.string.compose_manager_close))
                }
            }
        }

        if (previewModeActive) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            stringResource(R.string.compose_manager_preview),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = {
                                val snapshot = previewSnapshot ?: return@TextButton
                                runCatching {
                                    manager.replaceAll(snapshot.map { it.copy(symbols = it.symbols.toList()) })
                                    selectedGroupId = previewSelectedGroupId
                                    refresh()
                                }.onFailure { errorMessage = it.message ?: "Unable to restore preview snapshot" }
                            }) { Text(stringResource(R.string.compose_manager_restore_preview)) }
                            TextButton(onClick = {
                                previewModeActive = false
                                previewSnapshot = null
                                previewSelectedGroupId = null
                            }) { Text(stringResource(R.string.compose_manager_finish_preview)) }
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(144.dp)) {
                        SymbolDrawer(
                            groups = groups,
                            modifier = Modifier.fillMaxSize(),
                            state = previewDrawerState,
                            onSymbolClick = {},
                            onSymbolLongClick = null
                        )
                    }
                }
            }
        }

        val selectedGroupIndex = groups.indexOfFirst { it.id == selectedGroupId }
            .coerceIn(0, (groups.size - 1).coerceAtLeast(0))
        val groupPagerState = rememberPagerState(
            initialPage = selectedGroupIndex,
            pageCount = { groups.size }
        )

        LaunchedEffect(selectedGroupId, groups) {
            if (groups.isNotEmpty()) {
                val target = groups.indexOfFirst { it.id == selectedGroupId }
                    .coerceIn(0, groups.lastIndex)
                if (groupPagerState.currentPage != target) {
                    groupPagerState.animateScrollToPage(target)
                }
            }
        }
        LaunchedEffect(groupPagerState.currentPage) {
            val pageGroup = groups.getOrNull(groupPagerState.currentPage)
            if (pageGroup != null && pageGroup.id != selectedGroupId) {
                selectedGroupId = pageGroup.id
                selectedSymbolIds = emptySet()
                dragWorkingOrder = null
            }
        }

        if (groups.isEmpty()) {
            Text(stringResource(R.string.compose_manager_create_group_hint))
        } else {
            ScrollableTabRow(
                selectedTabIndex = selectedGroupIndex,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 0.dp
            ) {
                groups.forEachIndexed { index, group ->
                    Tab(
                        selected = index == selectedGroupIndex,
                        onClick = {
                            selectedGroupId = group.id
                            selectedSymbolIds = emptySet()
                        },
                        text = {
                            Box {
                                Text(
                                    group.title,
                                    maxLines = 1,
                                    modifier = Modifier.combinedClickable(
                                        onClick = {
                                            selectedGroupId = group.id
                                            selectedSymbolIds = emptySet()
                                        },
                                        onLongClick = { groupMenuId = group.id }
                                    )
                                )
                                DropdownMenu(
                                    expanded = groupMenuId == group.id,
                                    onDismissRequest = { groupMenuId = null }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("上移分组") },
                                        enabled = index > 0,
                                        onClick = {
                                            groupMenuId = null
                                            runUndoable { manager.moveGroup(index, index - 1) }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("下移分组") },
                                        enabled = index < groups.lastIndex,
                                        onClick = {
                                            groupMenuId = null
                                            runUndoable { manager.moveGroup(index, index + 1) }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("添加分组") },
                                        onClick = {
                                            groupMenuId = null
                                            editingGroupId = null
                                            groupName = ""
                                            groupDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("复制分组内容到…") },
                                        enabled = groups.size > 1 && group.symbols.isNotEmpty(),
                                        onClick = {
                                            groupMenuId = null
                                            copyGroupSourceId = group.id
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("复制分组") },
                                        onClick = {
                                            groupMenuId = null
                                            runUndoable {
                                                val duplicate = manager.duplicateGroup(group.id, index + 1)
                                                selectedGroupId = duplicate.id
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.compose_manager_rename_group)) },
                                        onClick = {
                                            groupMenuId = null
                                            editingGroupId = group.id
                                            groupName = group.title
                                            groupDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.compose_manager_delete)) },
                                        enabled = groups.size > 1,
                                        onClick = {
                                            groupMenuId = null
                                            deleteGroupId = group.id
                                        }
                                    )
                                }
                            }
                        }
                    )
                }
            }

        if (searchVisible && searchFeatureVisible) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.symbol_drawer_135)) }
            )
        }
            HorizontalPager(
                state = groupPagerState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                key = { page -> groups[page].id }
            ) { page ->
                val group = groups[page]
                val symbolListState = rememberLazyListState()
                val pageVisibleSymbols = remember(
                    group,
                    searchQuery,
                    activeFilter,
                    searchIncludesActionPayload,
                    usageSnapshot,
                    favoritesFeatureVisible,
                    recentFeatureVisible
                ) {
                    usageState.filter(
                        group.symbols,
                        searchQuery,
                        if (activeFilter == SymbolManagerFilter.FAVORITES && !favoritesFeatureVisible ||
                            activeFilter == SymbolManagerFilter.RECENT && !recentFeatureVisible
                        ) SymbolManagerFilter.ALL else activeFilter,
                        searchIncludesActionPayload
                    )
                }
                val pageDisplayedSymbols = if (group.id == selectedGroupId) {
                    dragWorkingOrder ?: pageVisibleSymbols
                } else {
                    pageVisibleSymbols
                }

                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // The selected group is already identified by its tab.
                    // Keep the list page free of a duplicate title/action row.
                    if (group.id == selectedGroupId && group.symbols.isNotEmpty() && batchMode) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                stringResource(R.string.symbol_drawer_102, selectedSymbolIds.size.toString()),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            TextButton(
                                enabled = selectedSymbolIds.isNotEmpty(),
                                onClick = { targetDialogOperation = "copy" }
                            ) { Text(stringResource(R.string.symbol_drawer_070)) }
                            TextButton(
                                enabled = selectedSymbolIds.isNotEmpty() &&
                                    groups.any { it.id != group.id },
                                onClick = { targetDialogOperation = "move" }
                            ) { Text(stringResource(R.string.symbol_drawer_183)) }
                            TextButton(
                                enabled = selectedSymbolIds.isNotEmpty(),
                                onClick = {
                                    if (runUndoable { manager.removeSymbols(group.id, selectedSymbolIds) }) {
                                        selectedSymbolIds = emptySet()
                                    }
                                }
                            ) { Text(stringResource(R.string.compose_manager_delete_selected)) }
                        }
                    }

                    if (pageVisibleSymbols.isEmpty()) {
                        Text(
                            if (group.symbols.isEmpty()) {
                                stringResource(R.string.compose_manager_create_group_hint)
                            } else {
                                stringResource(R.string.symbol_drawer_115)
                            },
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        LazyColumn(
                            state = symbolListState,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(pageDisplayedSymbols, key = { _, symbol -> symbol.id }) { _, symbol ->
                                Card(
                                    modifier = Modifier.pointerInput(
                                        symbol.id, group.id, searchQuery, activeFilter, batchMode
                                    ) {
                                        if (searchQuery.isNotBlank() ||
                                            activeFilter != SymbolManagerFilter.ALL || batchMode
                                        ) return@pointerInput

                                        var pointerY = 0f
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                symbolListState.layoutInfo.visibleItemsInfo
                                                    .firstOrNull { it.key == symbol.id }
                                                    ?.let { item ->
                                                        pointerY = item.offset + item.size / 2f
                                                        dragWorkingOrder = group.symbols.toList()
                                                    }
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                pointerY += dragAmount.y
                                                val targetId = symbolListState.layoutInfo.visibleItemsInfo
                                                    .minByOrNull {
                                                        abs(it.offset + it.size / 2f - pointerY)
                                                    }?.key as? Long
                                                val working = dragWorkingOrder
                                                if (targetId != null && working != null) {
                                                    val from = working.indexOfFirst { it.id == symbol.id }
                                                    val to = working.indexOfFirst { it.id == targetId }
                                                    if (from >= 0 && to >= 0 && from != to) {
                                                        dragWorkingOrder = working.toMutableList().apply {
                                                            add(to, removeAt(from))
                                                        }
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                val reordered = dragWorkingOrder
                                                dragWorkingOrder = null
                                                if (reordered != null &&
                                                    reordered.map { it.id } != group.symbols.map { it.id }
                                                ) {
                                                    runUndoable {
                                                        manager.reorderSymbols(group.id, reordered.map { it.id })
                                                    }
                                                }
                                            },
                                            onDragCancel = { dragWorkingOrder = null }
                                        )
                                    }
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (group.id == selectedGroupId && batchMode) {
                                            Checkbox(
                                                checked = symbol.id in selectedSymbolIds,
                                                onCheckedChange = { checked ->
                                                    selectedSymbolIds = if (checked) {
                                                        selectedSymbolIds + symbol.id
                                                    } else {
                                                        selectedSymbolIds - symbol.id
                                                    }
                                                }
                                            )
                                        }
                                        Column(Modifier.weight(1f)) {
                                            Text(symbol.label, style = MaterialTheme.typography.bodyLarge)
                                            Text(
                                                text = symbol.shortAction.toString(),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            "≡",
                                            modifier = Modifier.padding(horizontal = 4.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Box {
                                            TextButton(onClick = { symbolMenuId = symbol.id }) {
                                                Text("⋮")
                                            }
                                            DropdownMenu(
                                                expanded = symbolMenuId == symbol.id,
                                                onDismissRequest = { symbolMenuId = null }
                                            ) {
                                                val symbolIndex = group.symbols.indexOfFirst { it.id == symbol.id }
                                                DropdownMenuItem(
                                                    text = { Text("上移") },
                                                    enabled = symbolIndex > 0,
                                                    onClick = {
                                                        symbolMenuId = null
                                                        if (symbolIndex > 0) {
                                                            runUndoable {
                                                                manager.moveSymbol(group.id, symbolIndex, symbolIndex - 1)
                                                            }
                                                        }
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("下移") },
                                                    enabled = symbolIndex >= 0 && symbolIndex < group.symbols.lastIndex,
                                                    onClick = {
                                                        symbolMenuId = null
                                                        if (symbolIndex >= 0 && symbolIndex < group.symbols.lastIndex) {
                                                            runUndoable {
                                                                manager.moveSymbol(group.id, symbolIndex, symbolIndex + 1)
                                                            }
                                                        }
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            if (symbol.id in usageSnapshot.favorites) "取消收藏" else "收藏"
                                                        )
                                                    },
                                                    onClick = {
                                                        symbolMenuId = null
                                                        usageSnapshot = usageState.toggleFavorite(symbol.id)
                                                        undoAction = null
                                                        undoLabel = null
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("复制到其他分组…") },
                                                    enabled = groups.any { it.id != group.id },
                                                    onClick = {
                                                        symbolMenuId = null
                                                        selectedGroupId = group.id
                                                        selectedSymbolIds = setOf(symbol.id)
                                                        targetDialogOperation = "copy"
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("移动到其他分组…") },
                                                    enabled = groups.any { it.id != group.id },
                                                    onClick = {
                                                        symbolMenuId = null
                                                        selectedGroupId = group.id
                                                        selectedSymbolIds = setOf(symbol.id)
                                                        targetDialogOperation = "move"
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.compose_manager_edit)) },
                                                    onClick = {
                                                        symbolMenuId = null
                                                        selectedGroupId = group.id
                                                        editingSymbol = symbol
                                                        symbolDialog = true
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.compose_manager_delete)) },
                                                    onClick = {
                                                        symbolMenuId = null
                                                        runUndoable { manager.removeSymbol(group.id, symbol.id) }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    copyGroupSourceId?.let { sourceId ->
        val source = groups.firstOrNull { it.id == sourceId }
        AlertDialog(
            onDismissRequest = { copyGroupSourceId = null },
            title = { Text("复制分组内容") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    groups.filter { it.id != sourceId }.forEach { target ->
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (source != null) {
                                    runUndoable {
                                        manager.copySymbols(
                                            source.id,
                                            source.symbols.mapTo(mutableSetOf()) { it.id },
                                            target.id
                                        )
                                    }
                                }
                                copyGroupSourceId = null
                            }
                        ) { Text(target.title) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { copyGroupSourceId = null }) {
                    Text(stringResource(R.string.compose_manager_cancel))
                }
            }
        )
    }


    if (undoAction != null && undoLabel != null) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(undoLabel.orEmpty(), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = {
                runCatching { undoAction?.invoke() }
                    .onSuccess { refresh() }
                    .onFailure { errorMessage = it.message ?: "Unable to undo operation" }
                undoAction = null
                undoLabel = null
            }) { Text(stringResource(R.string.compose_manager_undo)) }
        }
    }

    deleteGroupId?.let { groupId ->
        val group = groups.firstOrNull { it.id == groupId }
        AlertDialog(
            onDismissRequest = { deleteGroupId = null },
            title = { Text(stringResource(R.string.compose_manager_confirm_delete_group)) },
            text = { Text(group?.title.orEmpty()) },
            confirmButton = {
                TextButton(onClick = {
                    if (runUndoable { manager.removeGroup(groupId) }) deleteGroupId = null
                }) { Text(stringResource(R.string.compose_manager_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteGroupId = null }) {
                    Text(stringResource(R.string.compose_manager_cancel))
                }
            }
        )
    }

    if (targetDialogOperation != null && selectedGroup != null) {
        AlertDialog(
            onDismissRequest = { targetDialogOperation = null },
            title = {
                Text(if (targetDialogOperation == "copy") stringResource(R.string.symbol_drawer_070) else stringResource(R.string.symbol_drawer_183))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    groups.filter { it.id != selectedGroup.id }.forEach { target ->
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val operation = targetDialogOperation
                                val completed = runUndoable {
                                    if (operation == "copy") {
                                        manager.copySymbols(
                                            selectedGroup.id,
                                            selectedSymbolIds,
                                            target.id
                                        )
                                    } else {
                                        manager.moveSymbolsToGroup(
                                            selectedGroup.id,
                                            selectedSymbolIds,
                                            target.id
                                        )
                                    }
                                }
                                if (completed) {
                                    selectedSymbolIds = emptySet()
                                    targetDialogOperation = null
                                }
                            }
                        ) {
                            Text(target.title, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { targetDialogOperation = null }) {
                    Text(stringResource(R.string.compose_manager_cancel))
                }
            }
        )
    }

    if (groupDialog) {
        AlertDialog(
            onDismissRequest = { groupDialog = false },
            title = {
                Text(
                    if (editingGroupId == null) stringResource(R.string.compose_manager_add_group)
                    else stringResource(R.string.compose_manager_rename_group)
                )
            },
            text = {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.compose_manager_group_name)) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = groupName.trim()
                    if (name.isNotEmpty()) {
                        runUndoable {
                            val currentId = editingGroupId
                            if (currentId == null) {
                                selectedGroupId = manager.addGroup(name).id
                            } else manager.renameGroup(currentId, name)
                        }
                        groupDialog = false
                    }
                }) { Text(stringResource(R.string.compose_manager_save)) }
            },
            dismissButton = {
                TextButton(onClick = { groupDialog = false }) { Text(stringResource(R.string.compose_manager_cancel)) }
            }
        )
    }

    if (symbolDialog && selectedGroup != null) {
        val targetGroupId = selectedGroup.id
        SymbolActionEditorDialog(
            existing = editingSymbol,
            extensions = effectiveActions,
            onDismiss = { symbolDialog = false },
            onSave = { symbol ->
                runUndoable {
                    if (editingSymbol == null) {
                        manager.addSymbol(targetGroupId, symbol)
                    } else {
                        manager.updateSymbol(targetGroupId, symbol)
                    }
                }
                symbolDialog = false
            }
        )
    }

    if (importDialog) {
        AlertDialog(
            onDismissRequest = { importDialog = false },
            title = {
                Text(
                    stringResource(
                        if (importLegacyJson) R.string.compose_manager_import_legacy
                        else R.string.compose_manager_import_title
                    )
                )
            },
            text = {
                OutlinedTextField(
                    value = importJson,
                    onValueChange = { importJson = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    maxLines = 12,
                    label = { Text(stringResource(R.string.compose_manager_json)) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val imported = runCatching {
                        if (importLegacyJson) LegacySymbolDataCodec.decode(importJson)
                        else SymbolGroupBundleCodec.decode(importJson)
                    }.onFailure { errorMessage = it.message ?: "Invalid import data" }.getOrNull()
                    if (imported != null && runUndoable { manager.replaceAll(imported) }) {
                        selectedGroupId = imported.firstOrNull()?.id
                        refresh()
                        importDialog = false
                        importLegacyJson = false
                    }
                }) { Text(stringResource(R.string.compose_manager_import)) }
            },
            dismissButton = {
                TextButton(onClick = { importDialog = false }) { Text(stringResource(R.string.compose_manager_cancel)) }
            }
        )
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text(stringResource(R.string.compose_manager_error)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) { Text(stringResource(R.string.compose_manager_ok)) }
            }
        )
    }
}
