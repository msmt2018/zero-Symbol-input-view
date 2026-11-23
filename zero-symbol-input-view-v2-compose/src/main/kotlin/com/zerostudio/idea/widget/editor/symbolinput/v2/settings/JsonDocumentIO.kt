/*
 * This file is part of zero-Symbol-input-view-V2.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 * Author: android_zero · https://github.com/msmt2018
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.settings

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

/** Platform-neutral UTF-8 JSON stream codec. The caller owns and closes streams. */
object JsonDocumentIO {
    const val DEFAULT_MAX_IMPORT_BYTES: Int = 16 * 1024 * 1024

    /** Reads a bounded UTF-8 document, stripping one optional UTF-8 BOM. */
    fun readUtf8Json(
        input: InputStream,
        maxBytes: Int = DEFAULT_MAX_IMPORT_BYTES
    ): String {
        require(maxBytes >= 0) { "Maximum import size must not be negative" }
        val output = ByteArrayOutputStream(minOf(maxBytes, BUFFER_SIZE))
        val buffer = ByteArray(BUFFER_SIZE)
        var total = 0L

        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count == 0) {
                val single = input.read()
                if (single < 0) break
                total += 1
                require(total <= maxBytes.toLong()) {
                    "Selected JSON file exceeds the " + maxBytes + "-byte import limit"
                }
                output.write(single)
                continue
            }
            total += count.toLong()
            require(total <= maxBytes.toLong()) {
                "Selected JSON file exceeds the " + maxBytes + "-byte import limit"
            }
            output.write(buffer, 0, count)
        }
        return output.toString(Charsets.UTF_8.name()).removePrefix("\uFEFF")
    }

    /** Writes UTF-8 JSON bytes and flushes without closing the caller's stream. */
    fun writeUtf8Json(output: OutputStream, json: String) {
        output.write(json.toByteArray(Charsets.UTF_8))
        output.flush()
    }

    private const val BUFFER_SIZE = 8 * 1024
}
