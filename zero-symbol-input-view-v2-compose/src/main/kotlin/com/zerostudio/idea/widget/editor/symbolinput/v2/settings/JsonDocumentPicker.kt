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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * Composition-scoped JSON document operations.
 *
 * This state does not register Activity Result launchers, observe lifecycle,
 * retain an Activity, or create an Activity/Fragment dependency. The host
 * supplies document-selection requests and returns the selected URI through
 * the callback. File I/O is performed with the supplied ContentResolver.
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
 * Creates lifecycle-independent JSON document operations.
 *
 * [requestOpenDocument] and [requestCreateDocument] are host-owned platform
 * bridges. They may use an Activity Result API outside this library, a custom
 * document provider, or another picker implementation. This composable only
 * receives the resulting [Uri]; it does not own the launcher's lifecycle.
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

    return remember(contentResolver) {
        JsonDocumentPickerState(
            openAction = {
                runCatching {
                    latestOpenRequest.value { uri ->
                        if (uri != null) {
                            runCatching {
                                contentResolver.openInputStream(uri)
                                    ?: error("Unable to open selected JSON document")
                            }.mapCatching { input ->
                                input.use { stream ->
                                    val output = java.io.ByteArrayOutputStream()
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
                                runCatching { latestImported.value(json) }
                                    .onFailure { latestError.value(it) }
                            }.onFailure { latestError.value(it) }
                        }
                    }
                }.onFailure { latestError.value(it) }
            },
            saveAction = { fileName, json ->
                runCatching {
                    latestCreateRequest.value(fileName) { uri ->
                        if (uri != null) {
                            runCatching {
                                val output = contentResolver.openOutputStream(uri, "wt")
                                    ?: error("Unable to create selected JSON document")
                                output.bufferedWriter(Charsets.UTF_8).use { it.write(json) }
                            }.onSuccess {
                                runCatching { latestExported.value?.invoke() }
                                    .onFailure { latestError.value(it) }
                            }.onFailure { latestError.value(it) }
                        }
                    }
                }.onFailure { latestError.value(it) }
            }
        )
    }
}

private const val MAX_IMPORT_BYTES = 16 * 1024 * 1024
