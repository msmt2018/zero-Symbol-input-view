/*
 * This file is part of zero-Symbol-input-view-V2.
 *
 * Copyright (C) 2026 android_zero
 *
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Composition-scoped JSON document operations.
 *
 * The host owns document-selection requests. Operations and their asynchronous
 * callbacks are ignored after this state leaves its Composition.
 */
@Stable
class JsonDocumentPickerState internal constructor(
    private val openAction: () -> Unit,
    private val saveAction: (fileName: String, json: String) -> Unit
) {
    fun openJson() = openAction()
    fun saveJson(fileName: String, json: String) = saveAction(fileName, json)
}

/**
 * Creates Composition-scoped JSON document operations.
 *
 * The host supplies document-selection bridges; this function does not own
 * Activity Result launchers or retain an Activity/Fragment. A result delivered
 * after disposal is discarded without touching Compose state or invoking UI
 * callbacks.
 */
@Composable
fun rememberJsonDocumentPicker(
    contentResolver: ContentResolver,
    requestOpenDocument: (onResult: (Uri?) -> Unit) -> Unit,
    requestCreateDocument: (fileName: String, onResult: (Uri?) -> Unit) -> Unit,
    onImported: (String) -> Unit,
    onExported: (() -> Unit)? = null,
    onError: (Throwable) -> Unit
): JsonDocumentPickerState {
    val latestOpenRequest = rememberUpdatedState(requestOpenDocument)
    val latestCreateRequest = rememberUpdatedState(requestCreateDocument)
    val latestImported = rememberUpdatedState(onImported)
    val latestExported = rememberUpdatedState(onExported)
    val latestError = rememberUpdatedState(onError)
    val active = remember(contentResolver) { AtomicBoolean(true) }

    DisposableEffect(active) {
        active.set(true)
        onDispose { active.set(false) }
    }

    fun reportError(error: Throwable) {
        if (active.get()) latestError.value(error)
    }

    return remember(contentResolver, active) {
        JsonDocumentPickerState(
            openAction = {
                if (active.get()) {
                    runCatching {
                        latestOpenRequest.value { uri ->
                            if (uri != null && active.get()) {
                                runCatching {
                                    contentResolver.openInputStream(uri)
                                        ?: error("Unable to open selected JSON document")
                                }.mapCatching { input ->
                                    input.use { stream ->
                                        val output = ByteArrayOutputStream()
                                        val buffer = ByteArray(8 * 1024)
                                        var total = 0
                                        while (true) {
                                            val count = stream.read(buffer)
                                            if (count < 0) break
                                            total += count
                                            require(total <= MAX_IMPORT_BYTES) {
                                                "Selected JSON file exceeds the 16 MiB import limit"
                                            }
                                            output.write(buffer, 0, count)
                                        }
                                        output.toString(Charsets.UTF_8.name())
                                            .removePrefix("\uFEFF")
                                    }
                                }.onSuccess { json ->
                                    if (active.get()) {
                                        runCatching { latestImported.value(json) }
                                            .onFailure(::reportError)
                                    }
                                }.onFailure(::reportError)
                            }
                        }
                    }.onFailure(::reportError)
                }
            },
            saveAction = { fileName, json ->
                if (active.get()) {
                    runCatching {
                        latestCreateRequest.value(fileName) { uri ->
                            if (uri != null && active.get()) {
                                runCatching {
                                    val output = contentResolver.openOutputStream(uri, "wt")
                                        ?: error("Unable to create selected JSON document")
                                    output.bufferedWriter(Charsets.UTF_8).use { it.write(json) }
                                }.onSuccess {
                                    if (active.get()) {
                                        runCatching { latestExported.value?.invoke() }
                                            .onFailure(::reportError)
                                    }
                                }.onFailure(::reportError)
                            }
                        }
                    }.onFailure(::reportError)
                }
            }
        )
    }
}

private const val MAX_IMPORT_BYTES = 16 * 1024 * 1024
