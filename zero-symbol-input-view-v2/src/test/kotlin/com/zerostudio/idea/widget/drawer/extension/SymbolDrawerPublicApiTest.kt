package com.zerostudio.idea.widget.drawer.extension

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import com.zerostudio.idea.widget.drawer.action.SymbolActionId
import com.zerostudio.idea.widget.drawer.action.SymbolActionRegistry
import com.zerostudio.idea.widget.drawer.action.SymbolActionHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
    fun publicRegistryExecutesExtensionAndLegacyHandlers() {
        val registry = SymbolActionRegistry<Any>()
        val context = Any()
        var extensionPayload: String? = null
        var legacyPayload: String? = null

        registry.register(
            SymbolActionId.extension("sample.insert"),
            SymbolActionHandler { actualContext, payload ->
                assertTrue(actualContext === context)
                extensionPayload = payload
                true
            }
        )
        registry.register(
            73,
            SymbolActionHandler { actualContext, payload ->
                assertTrue(actualContext === context)
                legacyPayload = payload
                true
            }
        )

        assertEquals(true, registry.execute(
            SymbolActionId.extension("sample.insert"),
            context,
            "extension-payload"
        ))
        assertEquals(true, registry.execute(73, context, "legacy-payload"))
        assertEquals("extension-payload", extensionPayload)
        assertEquals("legacy-payload", legacyPayload)
    }

    @Test
    fun closingReplacedPublicRegistrationDoesNotRemoveNewHandler() {
        val registry = SymbolActionRegistry<Any>()
        val id = SymbolActionId.extension("sample.replace")
        val context = Any()
        val oldHandle = registry.register(id, SymbolActionHandler { _, _ -> false })
        val newHandle = registry.register(id, SymbolActionHandler { _, _ -> true })

        oldHandle.close()

        assertEquals(true, registry.execute(id, context))
        newHandle.close()
        assertEquals(null, registry.execute(id, context))
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
