package com.zerostudio.idea.widget.editor.symbolinput.v2.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LegacySymbolDataMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val legacyPreferences by lazy {
        context.getSharedPreferences("advanced_symbol_prefs", Context.MODE_PRIVATE)
    }

    private class MemoryRepository(initial: List<SymbolGroup> = emptyList()) : SymbolGroupRepository {
        var groups = initial
        var migrationComplete = false

        override fun loadGroups() = groups
        override fun saveGroups(groups: List<SymbolGroup>) {
            this.groups = groups.toList()
        }
        override fun isLegacyMigrationComplete() = migrationComplete
        override fun markLegacyMigrationComplete() {
            migrationComplete = true
        }
    }

    @Before
    fun clearLegacyPreferences() {
        legacyPreferences.edit().clear().commit()
    }

    @After
    fun cleanLegacyPreferences() {
        legacyPreferences.edit().clear().commit()
    }

    @Test
    fun importsV1ActionsWithoutMutatingTheLegacyPreference() {
        val json = """[{"n":"Legacy","d":[{"a":0,"b":"x","c":"x","d":36,"e":"replacement"},{"a":987,"b":"custom","c":"short payload","d":988,"e":"long payload"}]}]"""
        legacyPreferences.edit().putString("symbol_json_data", json).commit()
        val repository = MemoryRepository()

        assertTrue(LegacySymbolDataMigration.importIfV2Empty(context, repository))
        assertTrue(repository.migrationComplete)
        assertEquals("Legacy", repository.groups.single().title)
        assertEquals(SymbolAction.InsertText("x"), repository.groups.single().symbols.single().shortAction)
        assertEquals(
            SymbolAction.EditorCommand("line.replace", "replacement"),
            repository.groups.single().symbols.first().longAction
        )
        assertEquals(
            SymbolAction.Extension("legacy.action.987", "short payload"),
            repository.groups.single().symbols[1].shortAction
        )
        assertEquals(
            SymbolAction.Extension("legacy.action.988", "long payload"),
            repository.groups.single().symbols[1].longAction
        )
        assertEquals(json, legacyPreferences.getString("symbol_json_data", null))
    }

    @Test
    fun malformedLegacyDataIsNotMarkedMigratedOrPartiallyImported() {
        val json = """[{"n":"Legacy","d":[{"a":"not-an-action-id","b":"x"}]}]"""
        legacyPreferences.edit().putString("symbol_json_data", json).commit()
        val repository = MemoryRepository()

        assertFalse(LegacySymbolDataMigration.importIfV2Empty(context, repository))
        assertFalse(repository.migrationComplete)
        assertTrue(repository.groups.isEmpty())
        assertEquals(json, legacyPreferences.getString("symbol_json_data", null))
    }

    @Test
    fun existingV2DataWinsAndLegacyPreferenceRemainsUntouched() {
        val json = """[{"n":"Legacy","d":[{"a":0,"b":"legacy","c":"legacy"}]}]"""
        legacyPreferences.edit().putString("symbol_json_data", json).commit()
        val existing = listOf(
            SymbolGroup("v2", "V2", emptyList())
        )
        val repository = MemoryRepository(existing)

        assertFalse(LegacySymbolDataMigration.importIfV2Empty(context, repository))
        assertTrue(repository.migrationComplete)
        assertEquals(existing, repository.groups)
        assertEquals(json, legacyPreferences.getString("symbol_json_data", null))
    }
}
