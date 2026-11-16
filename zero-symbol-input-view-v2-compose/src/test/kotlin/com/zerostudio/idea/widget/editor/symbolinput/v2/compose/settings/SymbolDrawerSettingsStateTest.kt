package com.zerostudio.idea.widget.editor.symbolinput.v2.compose.settings

import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SettingsLoadResult
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolDrawerSettingsStateTest {
    @Test fun updatePersistsBeforePublishingSnapshot() {
        val repository = FakeSettingsRepository()
        val state = SymbolDrawerSettingsState(repository)
        val changed = state.settings.copy(columns = 7)

        assertTrue(state.update(changed))
        assertEquals(changed, state.settings)
        assertEquals(changed, repository.stored)
    }

    @Test fun failedWriteDoesNotPublishCandidate() {
        val repository = FakeSettingsRepository().apply { failWrites = true }
        val state = SymbolDrawerSettingsState(repository)
        val before = state.settings

        assertFalse(state.update(before.copy(columns = 8)))
        assertEquals(before, state.settings)
        assertEquals(before, repository.stored)
        assertTrue(state.lastError != null)
    }

    @Test fun resetReloadsRepositoryDefaults() {
        val repository = FakeSettingsRepository()
        val state = SymbolDrawerSettingsState(repository)
        assertTrue(state.update(state.settings.copy(columns = 4)))

        assertTrue(state.reset())
        assertEquals(SymbolDrawerSettings(), state.settings)
        assertEquals(SymbolDrawerSettings(), repository.stored)
    }

    private class FakeSettingsRepository : SymbolDrawerSettingsRepository {
        var stored = SymbolDrawerSettings()
        var failWrites = false

        override fun load() = SettingsLoadResult(stored)
        override fun save(settings: SymbolDrawerSettings) {
            check(!failWrites) { "write failed" }
            stored = settings
        }
        override fun reset() {
            check(!failWrites) { "reset failed" }
            stored = SymbolDrawerSettings()
        }
    }
}
