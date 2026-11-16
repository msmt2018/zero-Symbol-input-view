/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.compose.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zerostudio.idea.widget.editor.symbolinput.v2.R
import com.zerostudio.idea.widget.editor.symbolinput.v2.compose.ComposeSymbolDrawerExtensions
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettingsRepository

/**
 * Ready-to-use settings route. Unlike the stateless screen, this route loads
 * from and commits to the supplied V2 repository automatically.
 */
@Composable
fun SymbolDrawerSettingsRoute(
    repository: SymbolDrawerSettingsRepository,
    modifier: Modifier = Modifier,
    extensions: ComposeSymbolDrawerExtensions = ComposeSymbolDrawerExtensions()
) {
    val state = rememberSymbolDrawerSettingsState(repository)
    SymbolDrawerSettingsScreen(
        settings = state.settings,
        onSettingsChange = state::update,
        modifier = modifier,
        extensions = extensions
    )
    val error = state.lastError
    if (error != null) {
        AlertDialog(
            onDismissRequest = state::clearError,
            title = { Text(stringResource(R.string.compose_operation_failed)) },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = state::clearError) {
                    Text(stringResource(R.string.compose_ok))
                }
            }
        )
    }
}
