/*
 * This file is part of zero-Symbol-input-view-V2.
 *
 * Copyright (C) 2026 android_zero
 *
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 *
 * This file is distributed under the terms of all licenses listed above.
 * See the corresponding license texts in the repository.
 *
 * Author: android_zero
 * https://github.com/msmt2018
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.AndroidLegacySymbolDataMigration
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.ObservableSymbolGroupRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SharedPreferencesSymbolGroupRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolDrawerDefaults
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.manager.SymbolDrawerManagerScreen
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SharedPreferencesSymbolDrawerSettingsRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettingsScreen

/**
 * Complete, ready-to-use Compose symbol toolbar.
 *
 * Owns its repositories, legacy-data migration, settings persistence, manager
 * and settings surfaces, and document pickers. The host only supplies the
 * symbol-action callback and places this composable in its UI.
 *
 * All UI and state are Composition-owned. This API does not require an
 * Activity, Fragment, Android View, ViewModel, or Lifecycle observer.
 */
@Composable
fun SymbolDrawerToolbar(
    onSymbolAction: (SymbolEntry) -> Unit,
    modifier: Modifier = Modifier,
    onSymbolLongAction: ((SymbolEntry) -> Unit)? = null,
    initiallyExpanded: Boolean = false
) {
    val context = LocalContext.current
    val groupRepository = remember(context) {
        SharedPreferencesSymbolGroupRepository(context.applicationContext)
    }
    val settingsRepository = remember(context) {
        SharedPreferencesSymbolDrawerSettingsRepository(context.applicationContext)
    }
    AndroidLegacySymbolDataMigration.importIfV2Empty(context, groupRepository)

    var groups by remember(groupRepository) {
        mutableStateOf(groupRepository.loadGroups().ifEmpty {
            SymbolDrawerDefaults.createGroups().also(groupRepository::saveGroups)
        })
    }
    var settings by remember(settingsRepository) {
        mutableStateOf(settingsRepository.load().settings)
    }
    var showManager by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var pendingOpenDocument by remember { mutableStateOf<(((Uri?) -> Unit))?>(null) }
    var pendingCreateDocument by remember { mutableStateOf<(((Uri?) -> Unit))?>(null) }

    val openDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        pendingOpenDocument?.invoke(uri)
        pendingOpenDocument = null
    }
    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        pendingCreateDocument?.invoke(uri)
        pendingCreateDocument = null
    }

    DisposableEffect(groupRepository) {
        val observer = (groupRepository as? ObservableSymbolGroupRepository)
            ?.observeGroups { groups = groupRepository.loadGroups() }
        onDispose { observer?.close() }
    }

    SymbolDrawer(
        groups = groups,
        modifier = modifier,
        settings = settings,
        onSymbolClick = { symbol ->
            if (symbol.shortAction == SymbolAction.OpenManager) {
                showManager = true
            } else {
                onSymbolAction(symbol)
            }
        },
        onSymbolLongClick = onSymbolLongAction,
        onOpenManager = { showManager = true },
        onOpenSettings = { showSettings = true }
    )

    if (showManager) {
        Dialog(onDismissRequest = { showManager = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
                shape = MaterialTheme.shapes.large
            ) {
                SymbolDrawerManagerScreen(
                    repository = groupRepository,
                    onClose = { showManager = false },
                    searchIncludesActionPayload = settings.searchIncludesActionPayload,
                    onOpenSettings = { showSettings = true },
                    requestOpenDocument = { callback ->
                        pendingOpenDocument = callback
                        openDocument.launch(arrayOf("application/json"))
                    },
                    requestCreateDocument = { fileName, callback ->
                        pendingCreateDocument = callback
                        createDocument.launch(fileName)
                    }
                )
            }
        }
    }

    if (showSettings) {
        Dialog(onDismissRequest = { showSettings = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
                shape = MaterialTheme.shapes.large
            ) {
                SymbolDrawerSettingsScreen(
                    settings = settings,
                    groupRepository = groupRepository,
                    onSettingsChange = { updated ->
                        settings = updated
                        settingsRepository.save(updated)
                    },
                    onReset = {
                        settingsRepository.reset()
                        settings = settingsRepository.load().settings
                    },
                    requestOpenDocument = { callback ->
                        pendingOpenDocument = callback
                        openDocument.launch(arrayOf("*/*"))
                    },
                    requestCreateDocument = { fileName, callback ->
                        pendingCreateDocument = callback
                        createDocument.launch(fileName)
                    }
                )
            }
        }
    }
}
