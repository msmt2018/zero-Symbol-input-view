package com.zerostudio.idea.widget.editor.symbolinput.v2.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SymbolTextEditingTest {
    @Test fun macroOffsetsAreMeasuredAfterTextAndSelectionSubstitution() {
        assertEquals(
            SymbolTextMacroExpansion("abcdef", 1, 3),
            SymbolTextEditing.expandMacro("a\$Sbc\$Edef", "")
        )
        assertEquals(
            SymbolTextMacroExpansion("aXY", 1, 3),
            SymbolTextEditing.expandMacro("a\$S\$T\$E", "XY")
        )
        assertEquals(
            SymbolTextMacroExpansion("[long]", 6, 6),
            SymbolTextEditing.expandMacro("[\$T]\$S\$E", "long")
        )
    }

    @Test fun macroKeepsEscapedDollarAndUnknownTokensLiteral() {
        assertEquals(
            SymbolTextMacroExpansion("\$Sx", 2, 3),
            SymbolTextEditing.expandMacro("\$\$S\$Sx\$E", "")
        )
        assertEquals(
            SymbolTextMacroExpansion("before\$Qafter", 13, 13),
            SymbolTextEditing.expandMacro("before\$Qafter", "")
        )
    }

    @Test fun macroWithoutMarkersLeavesCursorAtExpandedOutputEnd() {
        assertEquals(
            SymbolTextMacroExpansion("a<selected>b", 12, 12),
            SymbolTextEditing.expandMacro("a\$Tb", "<selected>")
        )
    }

    @Test fun backwardAndForwardDeletionKeepSurrogatePairsIntact() {
        val emoji = "A😀B"
        assertEquals(SymbolTextRange(1, 3),
            SymbolTextEditing.codePointDeletionRange(emoji, 3, backwards = true))
        assertEquals(SymbolTextRange(1, 3),
            SymbolTextEditing.codePointDeletionRange(emoji, 1, backwards = false))
        assertEquals(SymbolTextRange(0, 1),
            SymbolTextEditing.codePointDeletionRange(emoji, 1, backwards = true))
        assertEquals(SymbolTextRange(3, 4),
            SymbolTextEditing.codePointDeletionRange(emoji, 3, backwards = false))
        assertNull(SymbolTextEditing.codePointDeletionRange(emoji, 0, backwards = true))
        assertNull(SymbolTextEditing.codePointDeletionRange(emoji, emoji.length, backwards = false))
    }

    @Test fun duplicateLineInsertionHandlesFinalAndNonFinalLines() {
        assertEquals(
            SymbolTextInsertion(0, 3, "\nabc"),
            SymbolTextEditing.duplicateLineInsertion(0, 1, 3, "abc")
        )
        assertEquals(
            SymbolTextInsertion(1, 0, "abc\n"),
            SymbolTextEditing.duplicateLineInsertion(0, 2, 3, "abc")
        )
    }
}
