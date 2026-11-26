/*
 * This file is part of zero-Symbol-input-view-V2.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.drawer.action;

/** Compile-only proof that the editor-neutral text utility exposes Java ABI. */
final class JavaEditorNeutralTextEditingCompileFixture {
    static boolean verify() {
        SymbolTextMacroExpansion expansion = SymbolTextEditing.expandMacro("a$S$T$E", "x");
        SymbolTextRange range = SymbolTextEditing.codePointDeletionRange("A😀B", 3, true);
        SymbolTextInsertion insertion =
            SymbolTextEditing.duplicateLineInsertion(0, 1, 3, "abc", "\r\n");
        return expansion.getText().equals("ax")
            && expansion.getInsertionOffset() == 1
            && range != null && range.getStart() == 1 && range.getEnd() == 3
            && insertion.getLine() == 0 && insertion.getColumn() == 3;
    }
}
