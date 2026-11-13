package com.zerostudio.idea.widget.editor.symbolinput.v2.manager

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.LegacySymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LegacyActionPickerResolverTest {
    @Test fun matchingLegacyInsertPayloadResolvesToItsPickerEntry() {
        assertEquals(
            0,
            LegacyActionPickerResolver.legacyActionIndex(
                SymbolAction.InsertText("old"),
                0,
                "old"
            )
        )
    }

    @Test fun staleLegacyInsertPayloadCannotReplaceNewTypedAction() {
        assertNull(
            LegacyActionPickerResolver.legacyActionIndex(
                SymbolAction.InsertText("new"),
                0,
                "old"
            )
        )
    }

    @Test fun staleLegacyReplaceLinePayloadCannotReplaceNewTypedAction() {
        assertNull(
            LegacyActionPickerResolver.legacyActionIndex(
                SymbolAction.EditorCommand("line.replace", "new"),
                36,
                "old"
            )
        )
    }

    @Test fun v2InsertActionWithoutLegacyMetadataCanStillUseV1InsertEntry() {
        assertEquals(
            0,
            LegacyActionPickerResolver.legacyActionIndex(
                SymbolAction.InsertText("typed"),
                null,
                null
            )
        )
    }

    @Test fun historicalClipboardAndFunctionKeyIdsStayOutOfPicker() {
        assertNull(LegacyActionPickerResolver.legacyActionIndex(
            SymbolAction.Clipboard(com.zerostudio.idea.widget.editor.symbolinput.v2.model.ClipboardOperation.COPY),
            1,
            null
        ))
        assertNull(LegacyActionPickerResolver.legacyActionIndex(
            SymbolAction.InsertText("<F1>"),
            40,
            null
        ))
    }
}
