/*
 * This file is part of zero-Symbol-input-view-V2.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.drawer.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenericSymbolTextActionsPublicApiTest {
    @Test
    fun publicEditorNeutralPackageOwnsGenericActionRegistry() {
        val commands = GenericSymbolTextActions.supportedCommandIds()
        val registry = GenericSymbolTextActions.createRegistry()

        assertTrue(commands.size >= 30)
        assertEquals(commands.size, registry.size)
        assertTrue(commands.contains("document.removeBlankLines"))
        assertTrue(commands.contains("selection.wrapParentheses"))
    }
}
