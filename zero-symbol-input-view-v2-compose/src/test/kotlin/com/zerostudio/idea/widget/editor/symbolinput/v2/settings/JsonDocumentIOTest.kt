/*
 * This file is part of zero-Symbol-input-view-V2.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 * Author: android_zero · https://github.com/msmt2018
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.settings

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class JsonDocumentIOTest {
    @Test
    fun readsUtf8AndStripsOptionalBom() {
        val json = """{"label":"符号 👨‍👩‍👧‍👦"}"""
        val bytes = ("\uFEFF" + json).toByteArray(Charsets.UTF_8)
        assertEquals(json, JsonDocumentIO.readUtf8Json(ByteArrayInputStream(bytes)))
    }

    @Test
    fun acceptsDocumentExactlyAtByteLimit() {
        val json = "x".repeat(32)
        assertEquals(json, JsonDocumentIO.readUtf8Json(ByteArrayInputStream(json.toByteArray()), 32))
    }

    @Test
    fun rejectsDocumentOneByteOverLimit() {
        val input = ByteArrayInputStream(ByteArray(33) { 'x'.code.toByte() })
        assertThrows(IllegalArgumentException::class.java) {
            JsonDocumentIO.readUtf8Json(input, 32)
        }
    }

    @Test
    fun rejectsNegativeByteLimit() {
        assertThrows(IllegalArgumentException::class.java) {
            JsonDocumentIO.readUtf8Json(ByteArrayInputStream(byteArrayOf()), -1)
        }
    }

    @Test
    fun writesUtf8WithoutClosingOutputStream() {
        val json = """{"message":"你好 🌍"}"""
        val output = ByteArrayOutputStream()
        JsonDocumentIO.writeUtf8Json(output, json)
        assertEquals(json, output.toString(Charsets.UTF_8.name()))
        output.write('!'.code)
        assertEquals(json + "!", output.toString(Charsets.UTF_8.name()))
    }

    @Test
    fun decodesMalformedUtf8UsingJvmReplacementBehavior() {
        val input = ByteArrayInputStream(byteArrayOf(0xC3.toByte(), 0x28))
        assertEquals("�(", JsonDocumentIO.readUtf8Json(input))
    }
}
