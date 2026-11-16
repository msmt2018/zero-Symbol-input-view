/*
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposeSymbolDrawerExtensionRegistryTest {
    @Test
    fun closingReplacedRegistrationDoesNotRemoveReplacementWhenInstanceIsReused() {
        val registry = ComposeSymbolDrawerExtensionRegistry()
        val extension = ComposeSymbolDrawerActionExtension(
            id = "test.action",
            title = "Test action",
            handler = ComposeSymbolActionHandler { _, _ -> true }
        )

        val first = registry.registerAction(extension)
        val replacement = registry.registerAction(extension)

        first.close()
        assertEquals(listOf(extension), registry.snapshot().actions)

        replacement.close()
        assertTrue(registry.snapshot().actions.isEmpty())
    }

    @Test
    fun closeIsIdempotentAndClearInvalidatesOutstandingHandles() {
        val registry = ComposeSymbolDrawerExtensionRegistry()
        val extension = ComposeSymbolDrawerActionExtension(
            id = "test.action",
            title = "Test action",
            handler = ComposeSymbolActionHandler { _, _ -> true }
        )
        val handle = registry.registerAction(extension)

        registry.clear()
        handle.close()
        handle.close()

        assertTrue(registry.snapshot().actions.isEmpty())
    }
}
