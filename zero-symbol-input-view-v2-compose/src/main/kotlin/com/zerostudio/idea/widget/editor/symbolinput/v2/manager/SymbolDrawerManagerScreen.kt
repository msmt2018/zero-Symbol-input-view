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

import android.net.Uri

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.zerostudio.idea.widget.editor.symbolinput.v2.compose.R
import com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.AdaptiveContentLayoutResolver
import com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.WindowAdaptiveInfo
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupBundleCodec
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.LegacySymbolDataCodec
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.LegacySymbolActionCatalog
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
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.CursorTarget
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DeleteKind
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SelectionTransform
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.ClipboardOperation
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.SymbolDrawer
import com.zerostudio.idea.widget.editor.symbolinput.v2.rememberSymbolDrawerState
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.rememberJsonDocumentPicker
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
    extensionRegistry: SymbolDrawerExtensionRegistry? = null,
    onOpenSettings: (() -> Unit)? = null,
    requestOpenDocument: (((Uri?) -> Unit) -> Unit)? = null,
    requestCreateDocument: ((String, (Uri?) -> Unit) -> Unit)? = null
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
    var pendingFileImportLegacy by remember { mutableStateOf(false) }
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
    var favoritesDialog by remember { mutableStateOf(false) }
    var recentDialog by remember { mutableStateOf(false) }
    var draggingSymbolId by remember { mutableStateOf<Long?>(null) }
    var batchMode by remember { mutableStateOf(false) }
    var undoLabel by remember { mutableStateOf<String?>(null) }
    var operationMessage by remember { mutableStateOf<String?>(null) }
    var undoAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var targetDialogOperation by remember { mutableStateOf<String?>(null) }
    var deleteGroupId by remember { mutableStateOf<String?>(null) }
    var deleteSymbolTarget by remember { mutableStateOf<Pair<String, Long>?>(null) }
    var groupMenuId by remember { mutableStateOf<String?>(null) }
    var symbolMenuId by remember { mutableStateOf<Long?>(null) }
    var copyGroupSourceId by remember { mutableStateOf<String?>(null) }
    var moreMenuExpanded by remember { mutableStateOf(false) }
    var previewModeActive by remember { mutableStateOf(false) }
    var previewSnapshot by remember { mutableStateOf<List<SymbolGroup>?>(null) }
    var previewSelectedGroupId by remember { mutableStateOf<String?>(null) }
    val previewDrawerState = rememberSymbolDrawerState(initiallyExpanded = true)
    val undoMessage = context.getString(R.string.compose_manager_undo_message)
    // Resolve localized V1 action labels once per manager composition rather
    // than rebuilding the 33-label table for every visible grid item.
    val legacyActionLabels = listOf(
        context.getString(R.string.legacy_action_insert_text),
        context.getString(R.string.legacy_action_delete_line),
        context.getString(R.string.legacy_action_clear_line),
        context.getString(R.string.legacy_action_convert_to_uppercase),
        context.getString(R.string.legacy_action_convert_to_lowercase),
        context.getString(R.string.legacy_action_indent_tab),
        context.getString(R.string.legacy_action_toggle_comment),
        context.getString(R.string.legacy_action_go_to_line_start),
        context.getString(R.string.legacy_action_go_to_line_end),
        context.getString(R.string.legacy_action_move_cursor_left),
        context.getString(R.string.legacy_action_move_cursor_right),
        context.getString(R.string.legacy_action_move_cursor_up),
        context.getString(R.string.legacy_action_move_cursor_down),
        context.getString(R.string.legacy_action_open_symbol_toolbar_manager),
        context.getString(R.string.legacy_action_go_to_document_start),
        context.getString(R.string.legacy_action_go_to_document_end),
        context.getString(R.string.legacy_action_select_all),
        context.getString(R.string.legacy_action_copy),
        context.getString(R.string.legacy_action_cut),
        context.getString(R.string.legacy_action_paste),
        context.getString(R.string.legacy_action_format_code),
        context.getString(R.string.legacy_action_copy_line),
        context.getString(R.string.legacy_action_cut_line),
        context.getString(R.string.legacy_action_duplicate_line),
        context.getString(R.string.legacy_action_replace_line),
        context.getString(R.string.legacy_action_enter_key),
        context.getString(R.string.legacy_action_tab_key),
        context.getString(R.string.legacy_action_shift_tab),
        context.getString(R.string.legacy_action_clipboard_copy),
        context.getString(R.string.legacy_action_clipboard_cut),
        context.getString(R.string.legacy_action_clipboard_paste),
        context.getString(R.string.legacy_action_undo),
        context.getString(R.string.legacy_action_redo)
    )

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
            .onFailure { errorMessage = it.message ?: context.getString(R.string.symbol_drawer_unknown_error) }
            .isSuccess
    }
    fun runUndoable(message: String = undoMessage, command: () -> Unit): Boolean {
        return runCatching {
            val previousGroups = manager.groups.toList()
            val previousUsage = usageState.snapshot()
            val previousSelection = selectedGroupId
            val completed = runCommand(command)
            val usageRevisionAfterCommand = usageState.revision()
            if (completed) {
                undoLabel = message
                undoAction = {
                    runCatching {
                        manager.replaceAll(previousGroups)
                        usageSnapshot = if (usageState.revision() == usageRevisionAfterCommand) {
                            usageState.restore(previousUsage)
                        } else usageState.snapshot()
                        selectedGroupId = previousSelection
                    }.onFailure { errorMessage = it.message ?: context.getString(R.string.compose_ui_undo_failed) }
                }
            }
            completed
        }.onFailure { errorMessage = it.message ?: context.getString(R.string.compose_ui_group_operation_failed) }
            .getOrDefault(false)
    }

    val documentPicker = rememberJsonDocumentPicker(
        contentResolver = context.contentResolver,
        requestOpenDocument = { onResult ->
            requestOpenDocument?.invoke(onResult)
                ?: error("The host must provide an OpenDocument launcher")
        },
        requestCreateDocument = { fileName, onResult ->
            requestCreateDocument?.invoke(fileName, onResult)
                ?: error("The host must provide a CreateDocument launcher")
        },
        onImported = { json ->
            val isLegacyImport = pendingFileImportLegacy
            pendingFileImportLegacy = false
            runCatching {
                val imported = if (isLegacyImport) {
                    LegacySymbolDataCodec.decode(json)
                } else {
                    SymbolGroupBundleCodec.decode(json)
                }
                if (runUndoable(context.getString(R.string.compose_ui_groups_imported)) { manager.replaceAll(imported) }) {
                    selectedGroupId = imported.firstOrNull()?.id
                    pendingFileImportLegacy = false
                    refresh()
                }
            }.onFailure { errorMessage = it.message ?: context.getString(R.string.compose_ui_json_import_failed) }
        },
        onExported = { operationMessage = context.getString(R.string.compose_ui_v1_json_exported) },
        onError = { errorMessage = it.message ?: context.getString(R.string.compose_ui_json_operation_failed) }
    )

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
                    .onFailure { errorMessage = it.message ?: context.getString(R.string.compose_ui_groups_reload_failed) }
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            if (onOpenSettings != null) {
                TextButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = context.getString(R.string.compose_manager_settings)
                    )
                    Text(context.getString(R.string.compose_manager_settings))
                }
            } else {
                androidx.compose.foundation.layout.Spacer(Modifier.width(4.dp))
            }
            if (searchFeatureVisible) {
                OutlinedButton(onClick = {
                    searchVisible = !searchVisible
                    if (!searchVisible) searchQuery = ""
                }) {
                    Text(context.getString(if (searchVisible) R.string.symbol_interaction_collapse else R.string.symbol_search_search))
                }
            }
            Box {
                OutlinedButton(onClick = { moreMenuExpanded = true }) {
                    Text(context.getString(R.string.compose_manager_more))
                }
                DropdownMenu(expanded = moreMenuExpanded, onDismissRequest = { moreMenuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(context.getString(R.string.compose_manager_add_group)) },
                        onClick = {
                            moreMenuExpanded = false
                            editingGroupId = null
                            groupName = ""
                            groupDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(context.getString(R.string.compose_manager_add_symbol)) },
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
                                if (batchMode) context.getString(R.string.compose_manager_batch_done)
                                else context.getString(R.string.compose_manager_batch_select)
                            )
                        },
                        onClick = {
                            moreMenuExpanded = false
                            if (batchMode) selectedSymbolIds = emptySet()
                            batchMode = !batchMode
                        }
                    )
                    if (favoritesFeatureVisible) {
                        DropdownMenuItem(
                            text = { Text(context.getString(R.string.compose_manager_filter_favorites)) },
                            onClick = {
                                moreMenuExpanded = false
                                favoritesDialog = true
                            }
                        )
                    }
                    if (recentFeatureVisible) {
                        DropdownMenuItem(
                            text = { Text(context.getString(R.string.compose_manager_filter_recent)) },
                            onClick = {
                                moreMenuExpanded = false
                                recentDialog = true
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(context.getString(R.string.symbol_group_duplicate_group)) },
                        enabled = selectedGroup != null &&
                            groups.size < SymbolGroupBundleCodec.MAX_GROUPS &&
                            groups.sumOf { it.symbols.size.toLong() } +
                                (selectedGroup?.symbols?.size?.toLong() ?: 0L) <=
                                SymbolGroupBundleCodec.MAX_TOTAL_SYMBOLS,
                        onClick = {
                            moreMenuExpanded = false
                            selectedGroup?.let { group ->
                                runUndoable(context.getString(R.string.compose_ui_group_copied)) {
                                    val duplicate = manager.duplicateGroup(group.id)
                                    selectedGroupId = duplicate.id
                                }
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(context.getString(R.string.compose_manager_export)) },
                        onClick = {
                            moreMenuExpanded = false
                            runCatching { SymbolGroupBundleCodec.encode(groups) }
                                .onSuccess {
                                    clipboard.setText(AnnotatedString(it))
                                    operationMessage = context.getString(R.string.compose_ui_group_json_copied)
                                }
                                .onFailure { errorMessage = it.message ?: context.getString(R.string.compose_ui_groups_export_failed) }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(context.getString(R.string.compose_manager_import)) },
                        onClick = {
                            moreMenuExpanded = false
                            importDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(context.getString(R.string.compose_manager_export_legacy)) },
                        onClick = {
                            moreMenuExpanded = false
                            runCatching { LegacySymbolDataCodec.encode(groups) }
                                .onSuccess { documentPicker.saveJson("symbol-groups-v1.json", it) }
                                .onFailure { errorMessage = it.message ?: context.getString(R.string.compose_ui_v1_export_failed) }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(context.getString(R.string.compose_manager_import_legacy)) },
                        onClick = {
                            moreMenuExpanded = false
                            pendingFileImportLegacy = true
                            documentPicker.openJson()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                context.getString(
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
                    Text(context.getString(R.string.compose_manager_close))
                }
            }
        }

        operationMessage?.let { message ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(message, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { operationMessage = null }) { Text(context.getString(R.string.compose_ui_close)) }
                }
            }
        }

        val dragPreviewGroupId = draggingSymbolId?.let { activeId ->
            groups.firstOrNull { group -> group.symbols.any { it.id == activeId } }?.id
        }
        val embeddedPreviewGroups = if (
            draggingSymbolId != null && dragWorkingOrder != null && dragPreviewGroupId != null
        ) {
            groups.map { group ->
                if (group.id == dragPreviewGroupId) {
                    group.copy(symbols = dragWorkingOrder.orEmpty())
                } else {
                    group
                }
            }
        } else {
            groups
        }

        // The manager preview and the live reorder preview share one in-dialog
        // surface. Dragging only swaps the preview snapshot; it never overlays
        // the dialog with a Popup, and the surface disappears on end/cancel.
        if (previewModeActive || draggingSymbolId != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            context.getString(R.string.compose_manager_preview),
                            style = MaterialTheme.typography.titleSmall
                        )
                        if (previewModeActive && draggingSymbolId == null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = {
                                    val snapshot = previewSnapshot ?: return@TextButton
                                    runCatching {
                                        manager.replaceAll(snapshot.map { it.copy(symbols = it.symbols.toList()) })
                                        selectedGroupId = previewSelectedGroupId
                                        refresh()
                                    }.onFailure { errorMessage = it.message ?: context.getString(R.string.compose_ui_preview_restore_failed) }
                                }) { Text(context.getString(R.string.compose_manager_restore_preview)) }
                                TextButton(onClick = {
                                    previewModeActive = false
                                    previewSnapshot = null
                                    previewSelectedGroupId = null
                                }) { Text(context.getString(R.string.compose_manager_finish_preview)) }
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(144.dp)) {
                        SymbolDrawer(
                            groups = embeddedPreviewGroups,
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
                draggingSymbolId = null
            }
        }

        if (groups.isEmpty()) {
            Text(context.getString(R.string.compose_manager_create_group_hint))
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
                                        text = { Text(context.getString(R.string.compose_ui_group_move_up)) },
                                        enabled = index > 0,
                                        onClick = {
                                            groupMenuId = null
                                            runUndoable(context.getString(R.string.compose_ui_groups_reordered)) { manager.moveGroup(index, index - 1) }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(context.getString(R.string.compose_ui_group_move_down)) },
                                        enabled = index < groups.lastIndex,
                                        onClick = {
                                            groupMenuId = null
                                            runUndoable(context.getString(R.string.compose_ui_groups_reordered)) { manager.moveGroup(index, index + 1) }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(context.getString(R.string.compose_ui_group_add)) },
                                        onClick = {
                                            groupMenuId = null
                                            editingGroupId = null
                                            groupName = ""
                                            groupDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(context.getString(R.string.compose_ui_group_copy_contents_to)) },
                                        enabled = groups.size > 1 && group.symbols.isNotEmpty(),
                                        onClick = {
                                            groupMenuId = null
                                            copyGroupSourceId = group.id
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(context.getString(R.string.compose_ui_group_copy)) },
                                        enabled = groups.size < SymbolGroupBundleCodec.MAX_GROUPS &&
                                            groups.sumOf { it.symbols.size.toLong() } +
                                                group.symbols.size.toLong() <=
                                                SymbolGroupBundleCodec.MAX_TOTAL_SYMBOLS,
                                        onClick = {
                                            groupMenuId = null
                                            runUndoable(context.getString(R.string.compose_ui_group_copied)) {
                                                val duplicate = manager.duplicateGroup(group.id, index + 1)
                                                selectedGroupId = duplicate.id
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(context.getString(R.string.compose_manager_rename_group)) },
                                        onClick = {
                                            groupMenuId = null
                                            editingGroupId = group.id
                                            groupName = group.title
                                            groupDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(context.getString(R.string.compose_manager_delete)) },
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
                label = { Text(context.getString(R.string.symbol_search_search_symbol_names_and_actions)) }
            )
        }
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val density = LocalDensity.current
                val dragHandleCenterInsetPx = with(density) { 20.dp.toPx() }
                val widthDp = maxWidth.value.takeIf { it.isFinite() } ?: 600f
                val heightDp = maxHeight.value.takeIf { it.isFinite() } ?: 800f
                val windowInfo = remember(widthDp, heightDp, density.density) {
                    WindowAdaptiveInfo(
                        widthDp = widthDp,
                        heightDp = heightDp,
                        density = density.density
                    )
                }
                val managerCardColumns = remember(windowInfo) {
                    AdaptiveContentLayoutResolver.resolve(windowInfo).cardColumns.coerceIn(1, 4)
                }
                HorizontalPager(
                    state = groupPagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { page -> groups[page].id }
                ) { page ->
                val group = groups[page]
                val symbolListState = rememberLazyGridState()
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
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                context.getString(R.string.symbol_manager_selected, selectedSymbolIds.size.toString()),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                TextButton(
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 3.dp, vertical = 6.dp),
                                    enabled = selectedSymbolIds.isNotEmpty(),
                                    onClick = { targetDialogOperation = "copy" }
                                ) {
                                    Text(context.getString(R.string.symbol_item_copy_selected_symbols_to),
                                        style = MaterialTheme.typography.labelSmall, maxLines = 2)
                                }
                                TextButton(
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 3.dp, vertical = 6.dp),
                                    enabled = selectedSymbolIds.isNotEmpty() && groups.any { it.id != group.id },
                                    onClick = { targetDialogOperation = "move" }
                                ) {
                                    Text(context.getString(R.string.symbol_item_move_selected_symbols_to),
                                        style = MaterialTheme.typography.labelSmall, maxLines = 2)
                                }
                                TextButton(
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 3.dp, vertical = 6.dp),
                                    enabled = selectedSymbolIds.isNotEmpty(),
                                    onClick = {
                                        if (runUndoable(context.getString(R.string.compose_ui_symbols_deleted, selectedSymbolIds.size)) {
                                            manager.removeSymbols(group.id, selectedSymbolIds)
                                        }) selectedSymbolIds = emptySet()
                                    }
                                ) {
                                    Text(context.getString(R.string.compose_manager_delete_selected),
                                        style = MaterialTheme.typography.labelSmall, maxLines = 2)
                                }
                            }
                        }
                    }

                    if (pageVisibleSymbols.isEmpty()) {
                        Text(
                            if (group.symbols.isEmpty()) {
                                context.getString(R.string.compose_manager_create_group_hint)
                            } else {
                                context.getString(R.string.symbol_group_no_symbols_in_this_group_tap_add_and_choose_new_symbol)
                            },
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        LazyVerticalGrid(
                            state = symbolListState,
                            columns = GridCells.Fixed(managerCardColumns),
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(pageDisplayedSymbols, key = { _, symbol -> symbol.id }) { _, symbol ->
                                val dragScale by animateFloatAsState(
                                    targetValue = if (draggingSymbolId == symbol.id) 1.045f else 1f,
                                    animationSpec = spring(dampingRatio = 0.62f, stiffness = 520f),
                                    label = "symbol-reorder-scale"
                                )
                                Card(
                                    modifier = Modifier
                                        .animateItem()
                                        .graphicsLayer {
                                            scaleX = dragScale
                                            scaleY = dragScale
                                            shadowElevation = if (dragScale > 1f) 10.dp.toPx() else 0f
                                        }

                                ) {
                                    Box {
                                    Row(
                                        Modifier.fillMaxWidth().padding(8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                    ) {
                                        if (group.id == selectedGroupId && batchMode) {
                                            Checkbox(
                                                checked = symbol.id in selectedSymbolIds,
                                                onCheckedChange = { checked ->
                                                    selectedSymbolIds = if (checked) selectedSymbolIds + symbol.id
                                                    else selectedSymbolIds - symbol.id
                                                }
                                            )
                                        }
                                        Box(
                                            Modifier.width(3.dp).heightIn(min = 56.dp)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                        Column(
                                            Modifier.weight(1f).combinedClickable(
                                                onClick = {
                                                    if (batchMode) {
                                                        selectedSymbolIds = if (symbol.id in selectedSymbolIds) selectedSymbolIds - symbol.id
                                                        else selectedSymbolIds + symbol.id
                                                    } else {
                                                        selectedGroupId = group.id
                                                        editingSymbol = symbol
                                                        symbolDialog = true
                                                    }
                                                },
                                                onLongClick = {
                                                    if (batchMode) {
                                                        selectedSymbolIds = if (symbol.id in selectedSymbolIds) selectedSymbolIds - symbol.id
                                                        else selectedSymbolIds + symbol.id
                                                    } else symbolMenuId = symbol.id
                                                }
                                            ),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = (if (batchMode && symbol.id in selectedSymbolIds) "☑ " else "") +
                                                    (symbol.icon?.takeIf(String::isNotBlank)?.let { "$it  " }.orEmpty()) +
                                                    symbol.label,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                            )
                                            Text(
                                                text = symbolActionSummary(symbol, legacyActionLabels),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Box(
                                            Modifier.width(40.dp).heightIn(min = 56.dp)
                                                .pointerInput(symbol.id, group.id, searchQuery, activeFilter, batchMode) {
                                                    if (searchQuery.isNotBlank() ||
                                                        activeFilter != SymbolManagerFilter.ALL || batchMode
                                                    ) return@pointerInput
                                                    var pointerX = 0f
                                                    var pointerY = 0f
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = {
                                                            symbolListState.layoutInfo.visibleItemsInfo
                                                                .firstOrNull { it.key == symbol.id }?.let { info ->
                                                                    pointerX = info.offset.x + info.size.width - dragHandleCenterInsetPx
                                                                    pointerY = info.offset.y + info.size.height / 2f
                                                                    dragWorkingOrder = group.symbols.toList()
                                                                    draggingSymbolId = symbol.id
                                                                }
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            pointerX += dragAmount.x
                                                            pointerY += dragAmount.y
                                                            val targetId = symbolListState.layoutInfo.visibleItemsInfo
                                                                .minByOrNull { info ->
                                                                    val dx = info.offset.x + info.size.width / 2f - pointerX
                                                                    val dy = info.offset.y + info.size.height / 2f - pointerY
                                                                    dx * dx + dy * dy
                                                                }
                                                                ?.key as? Long
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
                                                            draggingSymbolId = null
                                                            if (reordered != null &&
                                                                reordered.map { it.id } != group.symbols.map { it.id }
                                                            ) runUndoable(context.getString(R.string.compose_ui_symbols_reordered)) {
                                                                manager.reorderSymbols(group.id, reordered.map { it.id })
                                                            }
                                                        },
                                                        onDragCancel = {
                                                            dragWorkingOrder = null
                                                            draggingSymbolId = null
                                                        }
                                                    )
                                                },
                                            contentAlignment = androidx.compose.ui.Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DragHandle,
                                                contentDescription = context.getString(R.string.compose_ui_drag_handle),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Box(Modifier.width(1.dp).height(1.dp)) {
                                            DropdownMenu(
                                                expanded = symbolMenuId == symbol.id,
                                                onDismissRequest = { symbolMenuId = null }
                                            ) {
                                                val symbolIndex = group.symbols.indexOfFirst { it.id == symbol.id }
                                                DropdownMenuItem(
                                                    text = { Text(if (symbol.id in usageSnapshot.favorites) context.getString(R.string.compose_ui_favorite_remove) else context.getString(R.string.compose_ui_favorite_add)) },
                                                    onClick = {
                                                        symbolMenuId = null
                                                        val wasFavorite = symbol.id in usageSnapshot.favorites
                                                        usageSnapshot = usageState.toggleFavorite(symbol.id)
                                                        operationMessage = if (wasFavorite) context.getString(R.string.compose_ui_favorite_removed) else context.getString(R.string.compose_ui_favorite_added)
                                                        undoAction = null
                                                        undoLabel = null
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(context.getString(R.string.compose_ui_mark_recent)) },
                                                    onClick = {
                                                        symbolMenuId = null
                                                        usageSnapshot = usageState.recordRecentlyUsed(symbol.id)
                                                        operationMessage = context.getString(R.string.compose_ui_marked_recent)
                                                        undoAction = null
                                                        undoLabel = null
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(context.getString(R.string.compose_ui_copy_to_current_group)) },
                                                    onClick = {
                                                        symbolMenuId = null
                                                        runUndoable(context.getString(R.string.compose_ui_symbol_copied)) {
                                                            manager.copySymbols(group.id, setOf(symbol.id), group.id)
                                                        }
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(context.getString(R.string.compose_ui_copy_to_other_group)) },
                                                    enabled = groups.any { it.id != group.id },
                                                    onClick = {
                                                        symbolMenuId = null
                                                        selectedGroupId = group.id
                                                        selectedSymbolIds = setOf(symbol.id)
                                                        targetDialogOperation = "copy"
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(context.getString(R.string.compose_ui_move_to_other_group)) },
                                                    enabled = groups.any { it.id != group.id },
                                                    onClick = {
                                                        symbolMenuId = null
                                                        selectedGroupId = group.id
                                                        selectedSymbolIds = setOf(symbol.id)
                                                        targetDialogOperation = "move"
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(context.getString(R.string.compose_ui_item_move_up)) },
                                                    enabled = symbolIndex > 0,
                                                    onClick = {
                                                        symbolMenuId = null
                                                        if (symbolIndex > 0) runUndoable(context.getString(R.string.compose_ui_symbols_reordered)) {
                                                            manager.moveSymbol(group.id, symbolIndex, symbolIndex - 1)
                                                        }
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(context.getString(R.string.compose_ui_item_move_down)) },
                                                    enabled = symbolIndex >= 0 && symbolIndex < group.symbols.lastIndex,
                                                    onClick = {
                                                        symbolMenuId = null
                                                        if (symbolIndex >= 0 && symbolIndex < group.symbols.lastIndex) {
                                                            runUndoable(context.getString(R.string.compose_ui_symbols_reordered)) {
                                                                manager.moveSymbol(group.id, symbolIndex, symbolIndex + 1)
                                                            }
                                                        }
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(context.getString(R.string.compose_manager_edit)) },
                                                    onClick = {
                                                        symbolMenuId = null
                                                        selectedGroupId = group.id
                                                        editingSymbol = symbol
                                                        symbolDialog = true
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(context.getString(R.string.compose_manager_delete)) },
                                                    onClick = {
                                                        symbolMenuId = null
                                                        deleteSymbolTarget = group.id to symbol.id
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
            }
        }

    copyGroupSourceId?.let { sourceId ->
        val source = groups.firstOrNull { it.id == sourceId }
        AlertDialog(
            onDismissRequest = { copyGroupSourceId = null },
            title = { Text(context.getString(R.string.compose_ui_group_copy_contents_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    groups.filter { it.id != sourceId }.forEach { target ->
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (source != null) {
                                    runUndoable(context.getString(R.string.compose_ui_group_contents_copied)) {
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
                    Text(context.getString(R.string.compose_manager_cancel))
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
                    .onFailure { errorMessage = it.message ?: context.getString(R.string.compose_ui_undo_operation_failed) }
                undoAction = null
                undoLabel = null
            }) { Text(context.getString(R.string.compose_manager_undo)) }
        }
    }

    deleteGroupId?.let { groupId ->
        val group = groups.firstOrNull { it.id == groupId }
        AlertDialog(
            onDismissRequest = { deleteGroupId = null },
            title = { Text(context.getString(R.string.compose_manager_confirm_delete_group)) },
            text = { Text(group?.title.orEmpty()) },
            confirmButton = {
                TextButton(onClick = {
                    if (runUndoable(context.getString(R.string.compose_ui_group_deleted)) { manager.removeGroup(groupId) }) deleteGroupId = null
                }) { Text(context.getString(R.string.compose_manager_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteGroupId = null }) {
                    Text(context.getString(R.string.compose_manager_cancel))
                }
            }
        )
    }

    deleteSymbolTarget?.let { (groupId, symbolId) ->
        val symbol = groups.firstOrNull { it.id == groupId }?.symbols?.firstOrNull { it.id == symbolId }
        AlertDialog(
            onDismissRequest = { deleteSymbolTarget = null },
            title = { Text(context.getString(R.string.compose_ui_symbol_delete_title)) },
            text = { Text(symbol?.label.orEmpty()) },
            confirmButton = {
                TextButton(onClick = {
                    if (runUndoable(context.getString(R.string.compose_ui_symbol_deleted, symbol?.label.orEmpty())) {
                        manager.removeSymbol(groupId, symbolId)
                    }) deleteSymbolTarget = null
                }) { Text(context.getString(R.string.compose_manager_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteSymbolTarget = null }) {
                    Text(context.getString(R.string.compose_manager_cancel))
                }
            }
        )
    }

    if (targetDialogOperation != null && selectedGroup != null) {
        AlertDialog(
            onDismissRequest = { targetDialogOperation = null },
            title = {
                Text(if (targetDialogOperation == "copy") context.getString(R.string.symbol_item_copy_selected_symbols_to) else context.getString(R.string.symbol_item_move_selected_symbols_to))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    groups.filter { it.id != selectedGroup.id }.forEach { target ->
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val operation = targetDialogOperation
                                val completed = runUndoable(
                                    if (operation == "copy") context.getString(R.string.compose_ui_symbol_copied_to_group) else context.getString(R.string.compose_ui_symbol_moved_to_group)
                                ) {
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
                    Text(context.getString(R.string.compose_manager_cancel))
                }
            }
        )
    }

    if (groupDialog) {
        AlertDialog(
            onDismissRequest = { groupDialog = false },
            title = {
                Text(
                    if (editingGroupId == null) context.getString(R.string.compose_manager_add_group)
                    else context.getString(R.string.compose_manager_rename_group)
                )
            },
            text = {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    singleLine = true,
                    label = { Text(context.getString(R.string.compose_manager_group_name)) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = groupName.trim()
                    if (name.isNotEmpty()) {
                        runUndoable(if (editingGroupId == null) context.getString(R.string.compose_ui_group_added) else context.getString(R.string.compose_ui_group_renamed)) {
                            val currentId = editingGroupId
                            if (currentId == null) {
                                selectedGroupId = manager.addGroup(name).id
                            } else manager.renameGroup(currentId, name)
                        }
                        groupDialog = false
                    }
                }) { Text(context.getString(R.string.compose_manager_save)) }
            },
            dismissButton = {
                TextButton(onClick = { groupDialog = false }) { Text(context.getString(R.string.compose_manager_cancel)) }
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
                runUndoable(if (editingSymbol == null) context.getString(R.string.compose_ui_symbol_added) else context.getString(R.string.compose_ui_symbol_updated)) {
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

    if (favoritesDialog) {
        val favoriteEntries = groups.flatMap { group ->
            group.symbols.filter { it.id in usageSnapshot.favorites }.map { group.title to it }
        }
        AlertDialog(
            onDismissRequest = { favoritesDialog = false },
            title = { Text(context.getString(R.string.compose_manager_favorites_title)) },
            text = {
                if (favoriteEntries.isEmpty()) {
                    Text(context.getString(R.string.compose_manager_empty_usage))
                } else {
                    Column(
                        Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        favoriteEntries.forEach { (groupTitle, symbol) ->
                            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(symbol.label, style = MaterialTheme.typography.titleSmall)
                                Text(groupTitle, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            HorizontalDivider(thickness = 0.5.dp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { favoritesDialog = false }) {
                    Text(context.getString(R.string.compose_manager_close))
                }
            }
        )
    }

    if (recentDialog) {
        val groupTitlesBySymbol = groups.flatMap { group ->
            group.symbols.map { it.id to group.title }
        }.toMap()
        val symbolsById = groups.flatMap { it.symbols }.associateBy { it.id }
        val recentEntries = usageSnapshot.recent.mapNotNull { id ->
            symbolsById[id]?.let { symbol -> (groupTitlesBySymbol[id].orEmpty()) to symbol }
        }
        AlertDialog(
            onDismissRequest = { recentDialog = false },
            title = { Text(context.getString(R.string.compose_manager_recent_title)) },
            text = {
                if (recentEntries.isEmpty()) {
                    Text(context.getString(R.string.compose_manager_empty_usage))
                } else {
                    Column(
                        Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recentEntries.forEach { (groupTitle, symbol) ->
                            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(symbol.label, style = MaterialTheme.typography.titleSmall)
                                Text(groupTitle, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            HorizontalDivider(thickness = 0.5.dp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { recentDialog = false }) {
                    Text(context.getString(R.string.compose_manager_close))
                }
            }
        )
    }

    if (importDialog) {
        AlertDialog(
            onDismissRequest = { importDialog = false },
            title = { Text(context.getString(R.string.compose_manager_clipboard_import)) },
            text = {},
            confirmButton = {
                TextButton(onClick = {
                    val json = clipboard.getText()?.text.orEmpty()
                    if (json.isBlank()) {
                        errorMessage = context.getString(R.string.compose_ui_clipboard_empty_json)
                    } else {
                        val imported = runCatching { SymbolGroupBundleCodec.decode(json) }
                            .onFailure { errorMessage = it.message ?: context.getString(R.string.compose_ui_clipboard_invalid_json) }
                            .getOrNull()
                        if (imported != null &&
                            runUndoable(context.getString(R.string.compose_ui_clipboard_groups_imported)) { manager.replaceAll(imported) }
                        ) {
                            selectedGroupId = imported.firstOrNull()?.id
                            refresh()
                            importDialog = false
                        }
                    }
                }) { Text(context.getString(R.string.compose_manager_paste_clipboard)) }
            },
            dismissButton = {
                TextButton(onClick = { importDialog = false }) {
                    Text(context.getString(R.string.compose_manager_cancel))
                }
            }
        )
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text(context.getString(R.string.compose_manager_error)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) { Text(context.getString(R.string.compose_manager_ok)) }
            }
        )
    }
}


@Composable
private fun symbolActionSummary(
    symbol: SymbolEntry,
    legacyLabels: List<String>
): String {
    val shortName = symbolActionDisplayName(symbol.shortAction, symbol.legacyShortActionId, legacyLabels)
    val longName = symbol.longAction?.let {
        symbolActionDisplayName(it, symbol.legacyLongActionId, legacyLabels)
    } ?: LocalContext.current.getString(R.string.symbol_manager_none)
    return LocalContext.current.getString(R.string.symbol_interaction_tap_long_press, shortName, longName)
}

@Composable
private fun symbolActionDisplayName(
    action: SymbolAction,
    legacyId: Int?,
    legacyLabels: List<String>
): String {
    if (legacyId != null) {
        val index = LegacySymbolActionCatalog.values.indexOf(legacyId)
        legacyLabels.getOrNull(index)?.let { return it }
    }
    return when (action) {
        is SymbolAction.InsertText -> LocalContext.current.getString(R.string.compose_ui_action_insert_text)
        is SymbolAction.ReplaceSelection -> LocalContext.current.getString(R.string.compose_ui_action_replace_selection)
        is SymbolAction.SurroundSelection -> LocalContext.current.getString(R.string.compose_ui_action_surround_selection)
        is SymbolAction.MoveCursor -> LocalContext.current.getString(R.string.compose_ui_action_move_cursor, cursorTargetLabel(action.target))
        is SymbolAction.Delete -> LocalContext.current.getString(R.string.compose_ui_action_delete, deleteKindLabel(action.kind))
        is SymbolAction.TransformSelection -> LocalContext.current.getString(R.string.compose_ui_action_transform, selectionTransformLabel(action.transform))
        is SymbolAction.EditorCommand -> localizedBuiltinSymbolActionOptions(LocalContext.current).firstOrNull { it.commandId == action.commandId }?.label
            ?: LocalContext.current.getString(R.string.compose_ui_action_editor_command, action.commandId)
        is SymbolAction.Clipboard -> LocalContext.current.getString(R.string.compose_ui_action_clipboard, clipboardOperationLabel(action.operation))
        SymbolAction.OpenManager -> LocalContext.current.getString(R.string.compose_ui_action_open_manager)
        is SymbolAction.Composite -> LocalContext.current.getString(R.string.compose_ui_action_composite, action.actions.size)
        is SymbolAction.Extension -> LocalContext.current.getString(R.string.compose_ui_action_extension, action.extensionId)
    }
}


@Composable
private fun cursorTargetLabel(target: CursorTarget): String = when (target) {
    CursorTarget.LEFT -> LocalContext.current.getString(R.string.compose_ui_cursor_left)
    CursorTarget.RIGHT -> LocalContext.current.getString(R.string.compose_ui_cursor_right)
    CursorTarget.UP -> LocalContext.current.getString(R.string.compose_ui_cursor_up)
    CursorTarget.DOWN -> LocalContext.current.getString(R.string.compose_ui_cursor_down)
    CursorTarget.LINE_START -> LocalContext.current.getString(R.string.compose_ui_cursor_line_start)
    CursorTarget.LINE_END -> LocalContext.current.getString(R.string.compose_ui_cursor_line_end)
    CursorTarget.DOCUMENT_START -> LocalContext.current.getString(R.string.compose_ui_cursor_document_start)
    CursorTarget.DOCUMENT_END -> LocalContext.current.getString(R.string.compose_ui_cursor_document_end)
}

@Composable
private fun deleteKindLabel(kind: DeleteKind): String = when (kind) {
    DeleteKind.BACKWARD -> LocalContext.current.getString(R.string.compose_ui_delete_backward)
    DeleteKind.FORWARD -> LocalContext.current.getString(R.string.compose_ui_delete_forward)
    DeleteKind.CURRENT_LINE -> LocalContext.current.getString(R.string.compose_ui_delete_current_line)
    DeleteKind.TO_LINE_START -> LocalContext.current.getString(R.string.compose_ui_delete_to_line_start)
    DeleteKind.TO_LINE_END -> LocalContext.current.getString(R.string.compose_ui_delete_to_line_end)
}

@Composable
private fun selectionTransformLabel(transform: SelectionTransform): String = when (transform) {
    SelectionTransform.UPPERCASE -> LocalContext.current.getString(R.string.compose_ui_transform_uppercase)
    SelectionTransform.LOWERCASE -> LocalContext.current.getString(R.string.compose_ui_transform_lowercase)
    SelectionTransform.TOGGLE_CASE -> LocalContext.current.getString(R.string.compose_ui_transform_toggle_case)
}

@Composable
private fun clipboardOperationLabel(operation: ClipboardOperation): String = when (operation) {
    ClipboardOperation.COPY -> LocalContext.current.getString(R.string.compose_ui_clipboard_copy)
    ClipboardOperation.CUT -> LocalContext.current.getString(R.string.compose_ui_clipboard_cut)
    ClipboardOperation.PASTE -> LocalContext.current.getString(R.string.compose_ui_clipboard_paste)
}
