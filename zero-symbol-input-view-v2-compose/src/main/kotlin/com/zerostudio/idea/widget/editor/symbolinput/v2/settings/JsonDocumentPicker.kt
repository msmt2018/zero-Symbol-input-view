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

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Composition-scoped JSON document picker actions. The launcher registration
 * and cleanup are handled by rememberLauncherForActivityResult.
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
 * Creates Storage Access Framework actions without retaining an Activity,
 * registering lifecycle observers or exposing launcher lifecycle to callers.
 */
@Composable
fun rememberJsonDocumentPicker(
    onImported: (String) -> Unit,
    onExported: (() -> Unit)? = null,
    onError: (Throwable) -> Unit
): JsonDocumentPickerState {
    val context = LocalContext.current
    val latestImported by rememberUpdatedState(onImported)
    val latestExported by rememberUpdatedState(onExported)
    val latestError by rememberUpdatedState(onError)
    var pendingExport by remember { mutableStateOf<String?>(null) }

    val openLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val input = context.contentResolver.openInputStream(uri)
                    ?: error("Unable to open selected JSON document")
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
                    output.toString(Charsets.UTF_8.name()).removePrefix("\uFEFF")
                }
            }.onSuccess { json ->
                runCatching { latestImported(json) }.onFailure(latestError)
            }.onFailure(latestError)
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val payload = pendingExport ?: error("No JSON export is pending")
                val output = context.contentResolver.openOutputStream(uri, "wt")
                    ?: error("Unable to create selected JSON document")
                output.bufferedWriter(Charsets.UTF_8).use { it.write(payload) }
            }.onSuccess {
                runCatching { latestExported?.invoke() }.onFailure(latestError)
            }.onFailure(latestError)
        }
        pendingExport = null
    }

    return remember(openLauncher, saveLauncher) {
        JsonDocumentPickerState(
            openAction = {
                runCatching { openLauncher.launch(arrayOf("*/*")) }.onFailure(latestError)
            },
            saveAction = { fileName, json ->
                pendingExport = json
                runCatching { saveLauncher.launch(fileName) }.onFailure {
                    pendingExport = null
                    latestError(it)
                }
            }
        )
    }
}

private const val MAX_IMPORT_BYTES = 16 * 1024 * 1024
