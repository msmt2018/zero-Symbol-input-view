package com.zerostudio.idea.widget.drawer.extension

import com.zerostudio.idea.widget.drawer.action.SymbolAction
import com.zerostudio.idea.widget.drawer.action.SymbolActionId
import com.zerostudio.idea.widget.drawer.action.SymbolActionRegistry
import com.zerostudio.idea.widget.drawer.action.SymbolActionHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SymbolDrawerPublicApiTest {
    @Test
    fun builtInFeatureIdsAreExposedFromPublicPackage() {
        assertEquals("builtin.search", SymbolDrawerBuiltInFeatures.SEARCH)
        assertEquals("builtin.favorites", SymbolDrawerBuiltInFeatures.FAVORITES)
        assertEquals("builtin.recent", SymbolDrawerBuiltInFeatures.RECENT)
        assertEquals("builtin.settings", SymbolDrawerBuiltInFeatures.SETTINGS)
        assertEquals("builtin.manager", SymbolDrawerBuiltInFeatures.MANAGER)
    }

    @Test
    fun publicActionApiTypesAreResolvable() {
        assertNotNull(SymbolAction::class)
        assertNotNull(SymbolActionId::class)
        assertNotNull(SymbolActionRegistry::class)
        assertNotNull(SymbolActionHandler::class)
    }

    @Test
    fun publicRegistryAndExtensionTypesAreResolvable() {
        assertNotNull(SymbolDrawerExtensionRegistry::class)
        assertNotNull(SymbolDrawerActionExtension::class)
        assertNotNull(SymbolDrawerItemViewExtension::class)
        assertNotNull(SymbolGroupIndicatorExtension::class)
        assertNotNull(SymbolDrawerSettingsTabExtension::class)
        assertNotNull(SymbolDrawerSettingExtension::class)
        assertNotNull(SymbolDrawerThemeExtension::class)
        assertNotNull(SymbolDrawerLifecycleExtension::class)
        assertNotNull(SymbolDrawerLegacyGroupProvider::class)
        assertNotNull(SymbolDrawerSettingsScope::class)
        assertNotNull(SymbolDrawerTheme::class)
        assertNotNull(SymbolDrawerItemViewState::class)
        assertNotNull(LegacySymbolGroup::class)
        assertNotNull(LegacySymbolItem::class)
    }
}
