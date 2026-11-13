/*
 * This file is part of ZeroStudio.
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

package com.zerostudio.idea.widget.editor.symbolinput.v2.data

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacySymbolActionAdapterTest {
    @Test fun convertsKnownInsertActionToTypedAction() {
        assertEquals(
            SymbolAction.InsertText("abc"),
            LegacySymbolActionAdapter.convert(LegacySymbolAction(0, "abc"))
        )
    }

    @Test fun convertsManagerActionToDedicatedAction() {
        assertEquals(
            SymbolAction.OpenManager,
            LegacySymbolActionAdapter.convert(LegacySymbolAction(22))
        )
    }

    @Test fun retainsUnknownActionAsExtension() {
        val result = LegacySymbolActionAdapter.convert(LegacySymbolAction(987, "data"))
        assertTrue(result is SymbolAction.Extension)
        assertEquals("legacy.action.987", (result as SymbolAction.Extension).extensionId)
    }
    @Test fun mapsEveryBuiltInV1ActionToAnImplementedV2Contract() {
        val expected = mapOf(
            0 to SymbolAction.InsertText("payload"),
            1 to SymbolAction.Clipboard(ClipboardOperation.COPY),
            2 to SymbolAction.Clipboard(ClipboardOperation.CUT),
            3 to SymbolAction.Delete(DeleteKind.CURRENT_LINE),
            4 to SymbolAction.EditorCommand("line.clear"),
            7 to SymbolAction.TransformSelection(SelectionTransform.UPPERCASE),
            8 to SymbolAction.TransformSelection(SelectionTransform.LOWERCASE),
            9 to SymbolAction.EditorCommand("editor.indent"),
            11 to SymbolAction.EditorCommand("editor.toggleLineComment"),
            16 to SymbolAction.MoveCursor(CursorTarget.LINE_START),
            17 to SymbolAction.MoveCursor(CursorTarget.LINE_END),
            18 to SymbolAction.MoveCursor(CursorTarget.LEFT),
            19 to SymbolAction.MoveCursor(CursorTarget.RIGHT),
            20 to SymbolAction.MoveCursor(CursorTarget.UP),
            21 to SymbolAction.MoveCursor(CursorTarget.DOWN),
            22 to SymbolAction.OpenManager,
            23 to SymbolAction.MoveCursor(CursorTarget.DOCUMENT_START),
            24 to SymbolAction.MoveCursor(CursorTarget.DOCUMENT_END),
            25 to SymbolAction.EditorCommand("selection.selectAll"),
            28 to SymbolAction.Clipboard(ClipboardOperation.COPY),
            29 to SymbolAction.Clipboard(ClipboardOperation.CUT),
            30 to SymbolAction.Clipboard(ClipboardOperation.PASTE),
            32 to SymbolAction.EditorCommand("editor.format"),
            33 to SymbolAction.EditorCommand("line.duplicate"),
            34 to SymbolAction.EditorCommand("line.cut"),
            35 to SymbolAction.EditorCommand("line.duplicate"),
            36 to SymbolAction.EditorCommand("line.replace", "payload"),
            37 to SymbolAction.InsertText("\n"),
            38 to SymbolAction.InsertText("\t"),
            39 to SymbolAction.EditorCommand("editor.unindent"),
            52 to SymbolAction.Clipboard(ClipboardOperation.COPY),
            53 to SymbolAction.Clipboard(ClipboardOperation.CUT),
            54 to SymbolAction.Clipboard(ClipboardOperation.PASTE),
            55 to SymbolAction.EditorCommand("history.undo"),
            56 to SymbolAction.EditorCommand("history.redo")
        )
        expected.forEach { (id, action) ->
            assertEquals("legacy action $id", action,
                LegacySymbolActionAdapter.convert(LegacySymbolAction(id, if (id == 0 || id == 36) "payload" else null)))
        }
        for (id in 40..51) {
            assertEquals(SymbolAction.InsertText("<F${id - 39}>"),
                LegacySymbolActionAdapter.convert(LegacySymbolAction(id)))
        }
    }

    @Test fun preservesDollarMacroPayloadForLegacyInsert() {
        assertEquals(
            SymbolAction.InsertText("val name = \$Svalue\$E"),
            LegacySymbolActionAdapter.convert(LegacySymbolAction(0, "val name = \$Svalue\$E"))
        )
    }

}
