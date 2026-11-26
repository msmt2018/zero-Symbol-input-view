/*
 * This file is part of zero-Symbol-input-view-V2.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.drawer.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SymbolTextEditingPublicApiTest {
    @Test
    fun macroExpansionUsesCanonicalEditorNeutralTypes() {
        assertEquals(
            SymbolTextMacroExpansion("aXY", 1, 3),
            SymbolTextEditing.expandMacro("a\$S\$T\$E", "XY")
        )
    }

    @Test
    fun codePointRangesRemainSurrogateSafe() {
        assertEquals(
            SymbolTextRange(1, 3),
            SymbolTextEditing.codePointDeletionRange("A😀B", 3, backwards = true)
        )
        assertNull(SymbolTextEditing.codePointDeletionRange("A😀B", 0, backwards = true))
    }

    @Test
    fun duplicateLineInsertionPreservesExplicitSeparator() {
        assertEquals(
            SymbolTextInsertion(0, 3, "\r\nabc"),
            SymbolTextEditing.duplicateLineInsertion(0, 1, 3, "abc", "\r\n")
        )
    }
}
