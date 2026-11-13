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

class SymbolGroupBundleCodecTest {
    @Test fun roundTripsGroupsSymbolsIconsAndTypedActions() {
        val groups = listOf(
            SymbolGroup(
                "kotlin",
                "Kotlin",
                listOf(
                    SymbolEntry(
                        7L,
                        "fun",
                        SymbolAction.InsertText("fun "),
                        SymbolAction.MoveCursor(CursorTarget.LINE_END),
                        icon = "ƒ"
                    )
                )
            )
        )
        val restored = SymbolGroupBundleCodec.decode(SymbolGroupBundleCodec.encode(groups))
        assertEquals(groups.single().title, restored.single().title)
        assertEquals(groups.single().symbols.single().label, restored.single().symbols.single().label)
        assertEquals(groups.single().symbols.single().shortAction, restored.single().symbols.single().shortAction)
        assertEquals(groups.single().symbols.single().longAction, restored.single().symbols.single().longAction)
    }


    @Test fun bundlePreservesV2ActionsAndLegacyMetadataWhenCompactV1CannotRepresentThem() {
        val source = listOf(
            SymbolGroup("modern", "Modern", listOf(
                SymbolEntry(
                    id = 42L,
                    label = "toggle",
                    shortAction = SymbolAction.TransformSelection(SelectionTransform.TOGGLE_CASE),
                    longAction = SymbolAction.Composite(listOf(
                        SymbolAction.InsertText("a"),
                        SymbolAction.SurroundSelection("[", "]")
                    )),
                    icon = "T",
                    legacyShortActionId = 33,
                    legacyShortText = "kept"
                )
            ))
        )
        val json = SymbolGroupBundleCodec.encode(source)
        assertTrue(json.contains("\\"format\\":\\"zero-symbol-drawer\\""))
        assertEquals(source, SymbolGroupBundleCodec.decode(json))
    }

    @Test fun storageAcceptsWhitespaceOnlyLabelsForControlKeys() {
        val groups = listOf(
            SymbolGroup(
                "controls",
                "Controls",
                listOf(
                    SymbolEntry(1L, "\n", SymbolAction.InsertText("\n")),
                    SymbolEntry(2L, "\t", SymbolAction.InsertText("\t"))
                )
            )
        )

        val encoded = SymbolGroupStorageCodec.encode(groups)
        assertEquals(groups.map { it.symbols.map(SymbolEntry::label) }, SymbolGroupStorageCodec.decode(encoded).map { it.symbols.map(SymbolEntry::label) })
    }

    @Test fun defaultGroupsCanBePersistedWithoutCrashing() {
        val defaults = SymbolDrawerDefaults.createGroups()

        val encoded = SymbolGroupStorageCodec.encode(defaults)
        val restored = SymbolGroupStorageCodec.decode(encoded)

        assertEquals(defaults.single().title, restored.single().title)
        assertEquals(defaults.single().symbols.map { it.label }, restored.single().symbols.map { it.label })
        assertEquals(defaults.single().symbols.map { it.shortAction to it.longAction }, restored.single().symbols.map { it.shortAction to it.longAction })
        assertTrue(restored.single().symbols.any { it.label == "\n" })
        assertTrue(restored.single().symbols.any { it.label == "\t" })
    }

    @Test fun emptyBackupIsValid() {
        assertTrue(SymbolGroupBundleCodec.decode(SymbolGroupBundleCodec.encode(emptyList())).isEmpty())
    }

    @Test fun storageWritesV1CompactSchemaAndReadsItBack() {
        val groups = listOf(
            SymbolGroup("kotlin", "Kotlin", listOf(
                SymbolEntry(1L, "fun", SymbolAction.InsertText("fun "))
            ))
        )
        val encoded = SymbolGroupStorageCodec.encode(groups)
        assertTrue(encoded.contains("\"n\":\"Kotlin\""))
        assertTrue(encoded.contains("\"d\":["))
        assertTrue(encoded.contains("\"a\":0"))
        assertTrue(encoded.contains("\"b\":\"fun\""))
        assertEquals("Kotlin", SymbolGroupStorageCodec.decode(encoded).single().title)
    }

    @Test fun storageReadsPreviousUnversionedV2Array() {
        val legacy = """[{"id":"g","title":"Group","symbols":[{"id":1,"label":"x","shortAction":{"kind":"insertText","template":"x"}}]}]"""
        val decoded = SymbolGroupStorageCodec.decode(legacy)
        assertEquals("g", decoded.single().id)
        assertEquals(SymbolAction.InsertText("x"), decoded.single().symbols.single().shortAction)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnrelatedJsonAsLegacyGroups() {
        LegacySymbolDataCodec.decode("""[{"name":"not-a-v1-group","items":[]}]""")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsLegacyGroupsWithWrongActionIdType() {
        LegacySymbolDataCodec.decode("""[{"n":"g","d":[{"a":2.5,"b":"x"}]}]""")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnknownBackupVersion() {
        SymbolGroupBundleCodec.decode("""{"format":"zero-symbol-drawer","version":99,"groups":[]}""")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDuplicateGroupIds() {
        val group = SymbolGroup("same", "A", emptyList())
        SymbolGroupBundleCodec.encode(listOf(group, group.copy(title = "B")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDuplicateSymbolIdsWithinGroup() {
        val first = SymbolEntry(1L, "a", SymbolAction.InsertText("a"))
        val second = SymbolEntry(1L, "b", SymbolAction.InsertText("b"))
        SymbolGroupBundleCodec.encode(listOf(SymbolGroup("g", "Group", listOf(first, second))))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOversizedActionPayloadsBeforePersistence() {
        val oversized = "x".repeat(SymbolGroupBundleCodec.MAX_ACTION_TEXT_CHARS + 1)
        val group = SymbolGroup(
            "g",
            "Group",
            listOf(SymbolEntry(1L, "large", SymbolAction.InsertText(oversized)))
        )
        SymbolGroupBundleCodec.encode(listOf(group))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsCompositeActionsBeyondSupportedDepth() {
        var action: SymbolAction = SymbolAction.InsertText("x")
        repeat(10) { action = SymbolAction.Composite(listOf(action)) }
        val group = SymbolGroup("g", "Group", listOf(SymbolEntry(1L, "nested", action)))
        SymbolGroupBundleCodec.encode(listOf(group))
    }

    @Test fun v1ActionIdsAndPayloadsSurviveCompactJsonRoundTrip() {
        val source = listOf(
            SymbolGroup(
                "legacy-actions",
                "Legacy actions",
                listOf(
                    SymbolEntry(
                        id = 1L,
                        label = "custom",
                        shortAction = LegacySymbolActionAdapter.convert(LegacySymbolAction(0, "val x = \$S\$T\$E")),
                        longAction = LegacySymbolActionAdapter.convert(LegacySymbolAction(36, "replacement")),
                        legacyShortActionId = 0,
                        legacyShortText = "val x = \$S\$T\$E",
                        legacyLongActionId = 36,
                        legacyLongText = "replacement"
                    ),
                    SymbolEntry(
                        id = 2L,
                        label = "duplicate",
                        shortAction = LegacySymbolActionAdapter.convert(LegacySymbolAction(33)),
                        legacyShortActionId = 33
                    )
                )
            )
        )
        val json = SymbolGroupStorageCodec.encode(source)
        assertTrue(json.contains("\"a\":0"))
        assertTrue(json.contains("\"c\":\"val x = \$S\$T\$E\""))
        assertTrue(json.contains("\"d\":36"))
        assertTrue(json.contains("\"e\":\"replacement\""))
        assertTrue(json.contains("\"a\":33"))
        val restored = SymbolGroupStorageCodec.decode(json)
        assertEquals(0, restored[0].symbols[0].legacyShortActionId)
        assertEquals("val x = \$S\$T\$E", restored[0].symbols[0].legacyShortText)
        assertEquals(36, restored[0].symbols[0].legacyLongActionId)
        assertEquals(33, restored[0].symbols[1].legacyShortActionId)
    }

}
