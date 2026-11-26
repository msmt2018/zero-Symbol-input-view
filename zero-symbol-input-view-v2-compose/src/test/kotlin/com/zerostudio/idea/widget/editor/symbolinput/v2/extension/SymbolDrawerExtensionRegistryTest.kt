package com.zerostudio.idea.widget.editor.symbolinput.v2.extension

import androidx.compose.runtime.Composable
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionHost
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
import org.junit.Assert.*
import org.junit.Test

class SymbolDrawerExtensionRegistryTest {
    private class Host : SymbolActionHost {
        var last: Pair<String, String?>? = null
        override fun insertText(template: String) = true
        override fun replaceSelection(text: String) = true
        override fun surroundSelection(prefix: String, suffix: String) = true
        override fun moveCursor(target: com.zerostudio.idea.widget.editor.symbolinput.v2.model.CursorTarget) = true
        override fun delete(kind: com.zerostudio.idea.widget.editor.symbolinput.v2.model.DeleteKind) = true
        override fun transformSelection(transform: com.zerostudio.idea.widget.editor.symbolinput.v2.model.SelectionTransform) = true
        override fun executeEditorCommand(commandId: String, payload: String?) = true
        override fun performClipboard(operation: com.zerostudio.idea.widget.editor.symbolinput.v2.model.ClipboardOperation) = true
        override fun openManager() = true
        override fun executeExtension(extensionId: String, payload: String?): Boolean {
            last = extensionId to payload
            return false
        }
    }

    @Test fun registeredActionDispatchesAndRegistrationHandleUnregisters() {
        val registry = SymbolDrawerExtensionRegistry()
        val host = Host()
        var called = false
        val handle = registry.registerAction(
            SymbolDrawerActionExtension("sample.insert", "Sample", handler = { _, payload ->
                called = payload == "hello"
                true
            })
        )
        val wrapped = registry.wrapActionHost(host)
        assertTrue(wrapped.executeExtension("sample.insert", "hello"))
        assertTrue(called)
        handle.close()
        assertFalse(wrapped.executeExtension("sample.insert", "fallback"))
        assertEquals("sample.insert", host.last?.first)
    }

    @Test fun staleRegistrationHandleCannotRemoveReplacement() {
        val registry = SymbolDrawerExtensionRegistry()
        val old = registry.registerAction(SymbolDrawerActionExtension("same.id", "Old", handler = { _, _ -> false }))
        val current = registry.registerAction(SymbolDrawerActionExtension("same.id", "Current", handler = { _, _ -> true }))
        old.close()
        assertEquals("Current", registry.snapshot().actions.single().title)
        current.close()
        assertTrue(registry.snapshot().actions.isEmpty())
    }

    @Test fun composeItemRendererAndIndicatorAreRegisteredAsContentSlots() {
        val registry = SymbolDrawerExtensionRegistry()
        val renderer = object : SymbolDrawerItemRenderer {
            override val id = "sample.renderer"
            override val title = "Sample renderer"
            override fun accepts(entry: SymbolEntry) = entry.label.startsWith("@")
            @Composable
            override fun Content(
                entry: SymbolEntry,
                state: SymbolDrawerItemRenderState,
                onClick: () -> Unit,
                onLongClick: (() -> Unit)?
            ) = Unit
        }
        val indicator = object : SymbolGroupIndicatorExtension {
            override val id = "sample.indicator"
            override val title = "Sample indicator"
            @Composable override fun Content(state: SymbolGroupIndicatorState) = Unit
        }
        val rendererHandle = registry.registerItemRenderer(renderer)
        val indicatorHandle = registry.registerIndicator(indicator)
        assertEquals("sample.renderer", registry.snapshot().itemRenderers.single().id)
        assertEquals("sample.indicator", registry.snapshot().indicators.single().id)
        rendererHandle.close()
        indicatorHandle.close()
        assertTrue(registry.snapshot().itemRenderers.isEmpty())
        assertTrue(registry.snapshot().indicators.isEmpty())
    }

    @Test fun settingsAndThemeExtensionsArePlatformNeutral() {
        val registry = SymbolDrawerExtensionRegistry()
        val tab = object : SymbolDrawerSettingsTabExtension {
            override val id = "sample.settings.tab"
            override val title = "Sample tab"
            @Composable override fun Content(scope: SymbolDrawerSettingsScope) = Unit
        }
        val setting = object : SymbolDrawerSettingExtension {
            override val id = "sample.settings.item"
            override val tabId = "sample.settings.tab"
            override val title = "Sample setting"
            @Composable override fun Content(scope: SymbolDrawerSettingsScope) = Unit
        }
        val theme = object : SymbolDrawerThemeExtension {
            override val id = "sample.theme"
            override val title = "Sample theme"
            override fun resolve(settings: SymbolDrawerSettings) = SymbolDrawerTheme(
                primary = 1,
                onPrimary = 2,
                surface = 3,
                onSurface = 4,
                secondaryText = 5,
                outline = 6
            )
        }
        val handles = listOf(
            registry.registerSettingsTab(tab),
            registry.registerSetting(setting),
            registry.registerTheme(theme)
        )
        assertEquals("sample.settings.tab", registry.snapshot().settingsTabs.single().id)
        assertEquals("sample.settings.item", registry.snapshot().settingsItems.single().id)
        assertEquals("sample.theme", registry.snapshot().themes.single().id)
        handles.forEach { it.close() }
        assertTrue(registry.snapshot().settingsTabs.isEmpty())
        assertTrue(registry.snapshot().settingsItems.isEmpty())
        assertTrue(registry.snapshot().themes.isEmpty())
    }

    @Test fun legacyGroupProviderAndBuiltInVisibilityRemainAvailable() {
        val registry = SymbolDrawerExtensionRegistry()
        val provider = object : SymbolDrawerLegacyGroupProvider {
            override val id = "sample.groups"
            override val title = "Sample groups"
            override suspend fun loadGroups() = listOf(
                com.zerostudio.idea.widget.editor.symbolinput.v2.model.legacy.SymbolGroup("sample")
            )
        }
        val handle = registry.registerGroupProvider(provider)
        registry.setBuiltInVisible("builtin.manager", false)
        assertEquals("sample.groups", registry.snapshot().groupProviders.single().id)
        assertFalse(registry.isBuiltInVisible("builtin.manager"))
        handle.close()
        assertTrue(registry.snapshot().groupProviders.isEmpty())
    }

    @Test fun snapshotsAdvanceAndObserverHandleUnsubscribes() {
        val registry = SymbolDrawerExtensionRegistry()
        val revisions = mutableListOf<Long>()
        val handle = registry.observe { revisions += it.revision }
        registry.registerAction(SymbolDrawerActionExtension("one", "One", handler = { _, _ -> true }))
        registry.registerAction(SymbolDrawerActionExtension("two", "Two", handler = { _, _ -> true }))
        handle.close()
        assertTrue(revisions.zipWithNext().all { (before, after) -> after > before })
        assertEquals(registry.revision(), registry.snapshot().revision)
    }

    @Test fun extensionErrorCallbackIsIsolated() {
        val registry = SymbolDrawerExtensionRegistry()
        var reported = false
        registry.onExtensionErrorListener = { id, error ->
            assertEquals("test.extension", id)
            assertEquals("boom", error.message)
            reported = true
        }
        registry.reportError("test.extension", IllegalStateException("boom"))
        assertTrue(reported)
        registry.onExtensionErrorListener = { _, _ -> error("diagnostic callback failure") }
        registry.reportError("test.extension", IllegalStateException("isolated"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidExtensionIdIsRejected() {
        SymbolDrawerExtensionRegistry().registerAction(
            SymbolDrawerActionExtension("bad id", "Bad", handler = { _, _ -> true })
        )
    }

    @Test fun reentrantMutationPreservesNotificationOrderForEveryObserver() {
        val registry = SymbolDrawerExtensionRegistry()
        val observedBySecondListener = mutableListOf<List<String>>()
        var reentered = false

        registry.observe(emitCurrent = false) { snapshot ->
            if (!reentered && snapshot.actions.any { it.id == "first.action" }) {
                reentered = true
                registry.registerAction(
                    SymbolDrawerActionExtension(
                        "second.action",
                        "Second",
                        handler = { _, _ -> true }
                    )
                )
            }
        }
        registry.observe(emitCurrent = false) { snapshot ->
            observedBySecondListener += snapshot.actions.map { it.id }
        }

        registry.registerAction(
            SymbolDrawerActionExtension(
                "first.action",
                "First",
                handler = { _, _ -> true }
            )
        )

        assertEquals(
            listOf(
                listOf("first.action"),
                listOf("first.action", "second.action")
            ),
            observedBySecondListener
        )
    }

    @Test fun observerAddedDuringDispatchDoesNotReceiveOlderQueuedSnapshots() {
        val registry = SymbolDrawerExtensionRegistry()
        val lateObserverRevisions = mutableListOf<Long>()
        var subscribed = false

        registry.observe(emitCurrent = false) { snapshot ->
            if (!subscribed && snapshot.actions.any { it.id == "first.action" }) {
                subscribed = true
                registry.observe { current -> lateObserverRevisions += current.revision }
                registry.registerAction(
                    SymbolDrawerActionExtension(
                        "second.action",
                        "Second",
                        handler = { _, _ -> true }
                    )
                )
            }
        }

        registry.registerAction(
            SymbolDrawerActionExtension(
                "first.action",
                "First",
                handler = { _, _ -> true }
            )
        )

        assertEquals(listOf(registry.revision() - 1, registry.revision()), lateObserverRevisions)
    }
}
