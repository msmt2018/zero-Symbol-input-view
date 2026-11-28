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
import androidx.compose.ui.platform.LocalContext
import com.zerostudio.idea.widget.editor.symbolinput.v2.compose.R
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
    val context = LocalContext.current.applicationContext
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

    return remember(contentResolver, active, context) {
        JsonDocumentPickerState(
            openAction = {
                if (active.get()) {
                    runCatching {
                        latestOpenRequest.value { uri ->
                            if (uri != null && active.get()) {
                                runCatching {
                                    contentResolver.openInputStream(uri)
                                        ?: error(context.getString(R.string.compose_ui_json_open_failed))
                                }.mapCatching { input ->
                                    input.use { stream ->
                                        JsonDocumentIO.readUtf8Json(stream, JsonDocumentIO.DEFAULT_MAX_IMPORT_BYTES) { limit ->
                                            context.getString(R.string.compose_ui_json_import_limit_exceeded, limit)
                                        }
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
                                        ?: error(context.getString(R.string.compose_ui_json_create_failed))
                                    output.use { JsonDocumentIO.writeUtf8Json(it, json) }
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

