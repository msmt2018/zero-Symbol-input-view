/*
 * This file is part of ZeroStudio.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.action

import com.google.gson.JsonParser
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.*
import org.junit.Assert.*
import org.junit.Test

class SymbolActionJsonCodecContractTest {
    @Test fun roundTripsEveryActionVariantIncludingNullablePayloads() {
        val actions = listOf(
            SymbolAction.InsertText("text"),
            SymbolAction.ReplaceSelection("replace"),
            SymbolAction.SurroundSelection("(", ")"),
            SymbolAction.MoveCursor(CursorTarget.LINE_START),
            SymbolAction.Delete(DeleteKind.TO_LINE_END),
            SymbolAction.TransformSelection(SelectionTransform.LOWERCASE),
            SymbolAction.EditorCommand("editor.format"),
            SymbolAction.EditorCommand("editor.run", "payload"),
            SymbolAction.Clipboard(ClipboardOperation.CUT),
            SymbolAction.OpenManager,
            SymbolAction.Composite(listOf(SymbolAction.InsertText("nested"), SymbolAction.OpenManager)),
            SymbolAction.Extension("extension.run"),
            SymbolAction.Extension("extension.run", "payload")
        )
        actions.forEach { action ->
            assertEquals(action, SymbolActionJsonCodec.decode(SymbolActionJsonCodec.encode(action)))
        }
    }

    @Test fun encodedObjectsUseExplicitDiscriminatorAndDoNotEmitNullPayload() {
        val command = SymbolActionJsonCodec.encode(SymbolAction.EditorCommand("editor.run"))
        assertEquals("editorCommand", command.get("kind").asString)
        assertFalse(command.has("payload"))
        val extension = SymbolActionJsonCodec.encode(SymbolAction.Extension("plugin.run"))
        assertEquals("extension", extension.get("kind").asString)
        assertFalse(extension.has("payload"))
    }

    @Test fun rejectsMissingDiscriminatorAndRequiredFields() {
        assertDecodeFails("""{}""")
        assertDecodeFails("""{"kind":"insertText"}""")
        assertDecodeFails("""{"kind":"replaceSelection"}""")
        assertDecodeFails("""{"kind":"surroundSelection","prefix":"("}""")
        assertDecodeFails("""{"kind":"moveCursor"}""")
        assertDecodeFails("""{"kind":"delete"}""")
        assertDecodeFails("""{"kind":"transformSelection"}""")
        assertDecodeFails("""{"kind":"editorCommand"}""")
        assertDecodeFails("""{"kind":"clipboard"}""")
        assertDecodeFails("""{"kind":"extension"}""")
    }

    @Test fun rejectsInvalidEnumValuesAndUnknownActionKinds() {
        assertDecodeFails("""{"kind":"moveCursor","target":"SIDEWAYS"}""")
        assertDecodeFails("""{"kind":"delete","deleteKind":"EVERYTHING"}""")
        assertDecodeFails("""{"kind":"transformSelection","transform":"REVERSE"}""")
        assertDecodeFails("""{"kind":"clipboard","operation":"SHARE"}""")
        assertDecodeFails("""{"kind":"unrecognized"}""")
    }

    @Test fun rejectsCompositeMissingArrayEmptyArrayAndOversizedArray() {
        assertDecodeFails("""{"kind":"composite"}""")
        assertDecodeFails("""{"kind":"composite","actions":[]}""")
        val tooMany = (1..65).joinToString(",") { """{"kind":"openManager"}""" }
        assertDecodeFails("""{"kind":"composite","actions":[$tooMany]}""")
    }

    @Test fun rejectsCompositeNestingBeyondEightLevels() {
        var json = """{"kind":"openManager"}"""
        repeat(9) { json = """{"kind":"composite","actions":[$json]}""" }
        assertDecodeFails(json)
    }

    @Test fun rejectsWrongJsonNodeTypesForRequiredFields() {
        assertDecodeFails("""{"kind":"insertText","template":12}""")
        assertDecodeFails("""{"kind":"composite","actions":[null]}""")
    }

    @Test fun acceptsMaximumSupportedCompositeDepth() {
        var action: SymbolAction = SymbolAction.OpenManager
        repeat(8) { action = SymbolAction.Composite(listOf(action)) }
        val encoded = SymbolActionJsonCodec.encode(action)
        assertEquals(action, SymbolActionJsonCodec.decode(encoded))
    }

    private fun assertDecodeFails(json: String) {
        try {
            SymbolActionJsonCodec.decode(JsonParser.parseString(json).asJsonObject)
            fail("Expected malformed action JSON to be rejected: $json")
        } catch (_: IllegalArgumentException) {
            // Expected contract behavior.
        }
    }
}
