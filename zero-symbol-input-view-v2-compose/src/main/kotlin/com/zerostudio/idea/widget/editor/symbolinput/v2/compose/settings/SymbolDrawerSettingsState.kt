/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.compose.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettingsRepository

/**
 * Transactional Compose state backed by the existing V2 settings repository.
 *
 * A candidate is published only after its synchronous durable save succeeds,
 * so failed writes never leave the UI displaying settings that were not stored.
 */
class SymbolDrawerSettingsState internal constructor(
    private val repository: SymbolDrawerSettingsRepository
) {
    private val initial = repository.load()

    var settings by mutableStateOf(initial.settings)
        private set

    var recoveredFromInvalidData by mutableStateOf(initial.recoveredFromInvalidData)
        private set

    var lastError by mutableStateOf<String?>(null)
        private set

    fun update(candidate: SymbolDrawerSettings): Boolean {
        return runCatching {
            repository.save(candidate)
            settings = candidate
            recoveredFromInvalidData = false
            lastError = null
        }.onFailure {
            lastError = it.message ?: it::class.java.simpleName
        }.isSuccess
    }

    fun reset(): Boolean {
        return runCatching {
            repository.reset()
            val restored = repository.load()
            settings = restored.settings
            recoveredFromInvalidData = restored.recoveredFromInvalidData
            lastError = null
        }.onFailure {
            lastError = it.message ?: it::class.java.simpleName
        }.isSuccess
    }

    fun reload(): Boolean {
        return runCatching {
            val restored = repository.load()
            settings = restored.settings
            recoveredFromInvalidData = restored.recoveredFromInvalidData
            lastError = null
        }.onFailure {
            lastError = it.message ?: it::class.java.simpleName
        }.isSuccess
    }

    fun clearError() {
        lastError = null
    }
}

@Composable
fun rememberSymbolDrawerSettingsState(
    repository: SymbolDrawerSettingsRepository
): SymbolDrawerSettingsState = remember(repository) {
    SymbolDrawerSettingsState(repository)
}
