/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.data

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacySymbolDataMigrationCoreTest {
    private class MemoryRepository(initial: List<SymbolGroup> = emptyList()) : SymbolGroupRepository {
        var groups = initial
        var migrationComplete = false

        override fun loadGroups(): List<SymbolGroup> = groups
        override fun saveGroups(groups: List<SymbolGroup>) {
            this.groups = groups.toList()
        }
        override fun isLegacyMigrationComplete(): Boolean = migrationComplete
        override fun markLegacyMigrationComplete() {
            migrationComplete = true
        }
    }

    @Test
    fun importsLegacyJsonWithoutAnyAndroidRuntimeDependency() {
        val json = """[{"n":"Legacy","d":[{"a":0,"b":"x","c":"x","d":36,"e":"replacement"}]}]"""
        val repository = MemoryRepository()

        assertTrue(LegacySymbolDataMigration.importIfV2Empty(json, repository))
        assertTrue(repository.migrationComplete)
        val symbol = repository.groups.single().symbols.single()
        assertEquals(SymbolAction.InsertText("x"), symbol.shortAction)
        assertEquals(SymbolAction.EditorCommand("line.replace", "replacement"), symbol.longAction)
    }

    @Test
    fun malformedJsonLeavesMigrationPendingAndDoesNotPersistPartialData() {
        val repository = MemoryRepository()

        assertFalse(LegacySymbolDataMigration.importIfV2Empty(
            """[{"n":"Legacy","d":[{"a":"not-an-action-id","b":"x"}]}]""",
            repository
        ))
        assertFalse(repository.migrationComplete)
        assertTrue(repository.groups.isEmpty())
    }

    @Test
    fun existingV2DataWinsAndMissingLegacyJsonCompletesMigration() {
        val existing = listOf(SymbolGroup("v2", "V2", emptyList()))
        val repository = MemoryRepository(existing)

        assertFalse(LegacySymbolDataMigration.importIfV2Empty("legacy-json", repository))
        assertTrue(repository.migrationComplete)
        assertEquals(existing, repository.groups)

        val emptyRepository = MemoryRepository()
        assertFalse(LegacySymbolDataMigration.importIfV2Empty(null, emptyRepository))
        assertTrue(emptyRepository.migrationComplete)
    }
}
