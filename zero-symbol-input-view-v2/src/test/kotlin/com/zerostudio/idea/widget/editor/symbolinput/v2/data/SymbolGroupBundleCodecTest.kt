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
        assertEquals(groups, SymbolGroupBundleCodec.decode(SymbolGroupBundleCodec.encode(groups)))
    }


    @Test fun storageAcceptsWhitespaceOnlyLabelsForControlKeys() {
        val groups = listOf(
            SymbolGroup(
                "controls",
                "Controls",
                listOf(
                    SymbolEntry(1L, "\\n", SymbolAction.InsertText("\\n")),
                    SymbolEntry(2L, "\\t", SymbolAction.InsertText("\\t"))
                )
            )
        )

        val encoded = SymbolGroupStorageCodec.encode(groups)
        assertEquals(groups, SymbolGroupStorageCodec.decode(encoded))
    }

    @Test fun defaultGroupsCanBePersistedWithoutCrashing() {
        val defaults = SymbolDrawerDefaults.createGroups()

        val encoded = SymbolGroupStorageCodec.encode(defaults)
        val restored = SymbolGroupStorageCodec.decode(encoded)

        assertEquals(defaults, restored)
        assertTrue(restored.single().symbols.any { it.label == "\\n" })
        assertTrue(restored.single().symbols.any { it.label == "\\t" })
    }

    @Test fun emptyBackupIsValid() {
        assertTrue(SymbolGroupBundleCodec.decode(SymbolGroupBundleCodec.encode(emptyList())).isEmpty())
    }

    @Test fun storageWritesVersionedEnvelopeAndReadsItBack() {
        val groups = listOf(
            SymbolGroup("kotlin", "Kotlin", listOf(
                SymbolEntry(1L, "fun", SymbolAction.InsertText("fun "))
            ))
        )
        val encoded = SymbolGroupStorageCodec.encode(groups)
        assertTrue(encoded.contains("\"format\":\"zero-symbol-drawer\""))
        assertTrue(encoded.contains("\"version\":1"))
        assertEquals(groups, SymbolGroupStorageCodec.decode(encoded))
    }

    @Test fun storageReadsPreviousUnversionedV2Array() {
        val legacy = """[{"id":"g","title":"Group","symbols":[{"id":1,"label":"x","shortAction":{"kind":"insertText","template":"x"}}]}]"""
        val decoded = SymbolGroupStorageCodec.decode(legacy)
        assertEquals("g", decoded.single().id)
        assertEquals(SymbolAction.InsertText("x"), decoded.single().symbols.single().shortAction)
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
}
