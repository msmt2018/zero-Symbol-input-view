package com.zerostudio.idea.widget.drawer.extension

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import com.zerostudio.idea.widget.drawer.action.SymbolActionId
import com.zerostudio.idea.widget.drawer.action.SymbolActionRegistry
import com.zerostudio.idea.widget.drawer.action.SymbolActionHandler
import org.junit.Assert.*
import org.junit.Test

class SymbolDrawerPublicApiTest {
    @Test fun builtInFeatureIdsAreExposedFromPublicPackage() {
        assertEquals("builtin.search", SymbolDrawerBuiltInFeatures.SEARCH)
        assertEquals("builtin.favorites", SymbolDrawerBuiltInFeatures.FAVORITES)
        assertEquals("builtin.recent", SymbolDrawerBuiltInFeatures.RECENT)
        assertEquals("builtin.settings", SymbolDrawerBuiltInFeatures.SETTINGS)
        assertEquals("builtin.manager", SymbolDrawerBuiltInFeatures.MANAGER)
    }

    @Test fun publicActionTypesAreResolvable() {
        assertNotNull(SymbolAction::class)
        assertNotNull(SymbolActionId::class)
        assertNotNull(SymbolActionRegistry::class)
        assertNotNull(SymbolActionHandler::class)
    }

    @Test fun composableExtensionContractsAreResolvable() {
        assertNotNull(SymbolDrawerExtensionRegistry::class)
        assertNotNull(SymbolDrawerExtensionSnapshot::class)
        assertNotNull(SymbolDrawerActionExtension::class)
        assertNotNull(SymbolDrawerItemRenderer::class)
        assertNotNull(SymbolDrawerItemRenderState::class)
        assertNotNull(SymbolGroupIndicatorExtension::class)
        assertNotNull(SymbolGroupIndicatorState::class)
        assertNotNull(SymbolDrawerSettingsTabExtension::class)
        assertNotNull(SymbolDrawerSettingExtension::class)
        assertNotNull(SymbolDrawerSettingsScope::class)
        assertNotNull(SymbolDrawerThemeExtension::class)
        assertNotNull(SymbolDrawerTheme::class)
        assertNotNull(SymbolDrawerLegacyGroupProvider::class)
        assertNotNull(LegacySymbolGroup::class)
        assertNotNull(LegacySymbolItem::class)
    }
}
