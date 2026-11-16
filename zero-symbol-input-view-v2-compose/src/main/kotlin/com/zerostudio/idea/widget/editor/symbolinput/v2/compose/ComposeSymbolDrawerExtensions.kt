/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 * Author: android_zero · https://github.com/msmt2018
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.util.IdentityHashMap
import androidx.compose.material3.ColorScheme
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings

/** Compose-native renderer extension; it never creates or retains an Android View. */
interface SymbolDrawerItemRenderer {
    val id: String
    val priority: Int get() = 0
    fun accepts(symbol: SymbolEntry): Boolean = true

    @Composable
    fun Content(
        symbol: SymbolEntry,
        selected: Boolean,
        modifier: Modifier,
        onClick: () -> Unit,
        onLongClick: () -> Unit
    )
}

/** Compose-native group indicator contribution. */
interface SymbolDrawerGroupIndicator {
    val id: String
    val priority: Int get() = 0

    @Composable
    fun Content(
        groups: List<SymbolGroup>,
        selectedIndex: Int,
        onSelect: (Int) -> Unit,
        modifier: Modifier
    )
}

fun interface ComposeSymbolActionHandler {
    fun onAction(action: SymbolAction, symbol: SymbolEntry): Boolean
}

data class ComposeSymbolDrawerActionExtension(
    val id: String,
    val title: String,
    val priority: Int = 0,
    val handler: ComposeSymbolActionHandler
)

interface ComposeSymbolDrawerGroupProvider {
    val id: String
    val priority: Int get() = 0
    suspend fun loadGroups(): List<SymbolGroup>
}

data class ComposeDrawerTheme(val colorScheme: ColorScheme)
interface ComposeSymbolDrawerThemeExtension {
    val id: String
    val priority: Int get() = 0
    fun resolve(): ComposeDrawerTheme
}

interface ComposeSymbolDrawerLifecycleExtension {
    val id: String
    val priority: Int get() = 0
    fun onAttached() = Unit
    fun onSettingsChanged(settings: SymbolDrawerSettings) = Unit
    fun onGroupsChanged(groups: List<SymbolGroup>) = Unit
    fun onGroupSelected(index: Int, group: SymbolGroup?) = Unit
    fun onExpansionChanged(fraction: Float) = Unit
    fun onSymbolInvoked(symbol: SymbolEntry, longPress: Boolean) = Unit
    fun onDetached() = Unit
}

interface ComposeSymbolDrawerSettingsTabExtension {
    val id: String
    val title: String
    val priority: Int get() = 0

    @Composable
    fun Content(
        settings: SymbolDrawerSettings,
        onSettingsChange: (SymbolDrawerSettings) -> Unit,
        modifier: Modifier
    )
}

interface ComposeSymbolDrawerSettingExtension {
    val id: String
    val tabId: String
    val priority: Int get() = 0

    @Composable
    fun Content(
        settings: SymbolDrawerSettings,
        onSettingsChange: (SymbolDrawerSettings) -> Unit,
        modifier: Modifier
    )
}

/** Immutable ordered extension snapshot consumed by the Compose UI. */
@Immutable
data class ComposeSymbolDrawerExtensions(
    val actions: List<ComposeSymbolDrawerActionExtension> = emptyList(),
    val itemRenderers: List<SymbolDrawerItemRenderer> = emptyList(),
    val groupIndicators: List<SymbolDrawerGroupIndicator> = emptyList(),
    val groupProviders: List<ComposeSymbolDrawerGroupProvider> = emptyList(),
    val themes: List<ComposeSymbolDrawerThemeExtension> = emptyList(),
    val lifecycle: List<ComposeSymbolDrawerLifecycleExtension> = emptyList(),
    val settingsTabs: List<ComposeSymbolDrawerSettingsTabExtension> = emptyList(),
    val settingsItems: List<ComposeSymbolDrawerSettingExtension> = emptyList()
)

/**
 * Session-scoped registry for Compose extensions. All contributions are runtime
 * objects; persist their stable IDs/configuration in the host and register them
 * again when a drawer session is recreated.
 */
class ComposeSymbolDrawerExtensionRegistry {
    private val lock = Any()
    private val actions = LinkedHashMap<String, ComposeSymbolDrawerActionExtension>()
    private val renderers = LinkedHashMap<String, SymbolDrawerItemRenderer>()
    private val indicators = LinkedHashMap<String, SymbolDrawerGroupIndicator>()
    private val providers = LinkedHashMap<String, ComposeSymbolDrawerGroupProvider>()
    private val themes = LinkedHashMap<String, ComposeSymbolDrawerThemeExtension>()
    private val lifecycle = LinkedHashMap<String, ComposeSymbolDrawerLifecycleExtension>()
    private val settingsTabs = LinkedHashMap<String, ComposeSymbolDrawerSettingsTabExtension>()
    private val settingsItems = LinkedHashMap<String, ComposeSymbolDrawerSettingExtension>()
    private val observers = LinkedHashSet<(Long) -> Unit>()
    // Per-registry-map registration tokens make replacement handles safe even
    // when the same extension instance is registered more than once.
    private val registrationTokens = IdentityHashMap<Any, MutableMap<String, Any>>()
    private var revision = 0L

    fun registerAction(value: ComposeSymbolDrawerActionExtension) = register(actions, value.id, value)
    fun registerItemRenderer(value: SymbolDrawerItemRenderer) = register(renderers, value.id, value)
    fun registerGroupIndicator(value: SymbolDrawerGroupIndicator) = register(indicators, value.id, value)
    fun registerGroupProvider(value: ComposeSymbolDrawerGroupProvider) = register(providers, value.id, value)
    fun registerTheme(value: ComposeSymbolDrawerThemeExtension) = register(themes, value.id, value)
    fun registerLifecycle(value: ComposeSymbolDrawerLifecycleExtension) = register(lifecycle, value.id, value)
    fun registerSettingsTab(value: ComposeSymbolDrawerSettingsTabExtension) = register(settingsTabs, value.id, value)
    fun registerSetting(value: ComposeSymbolDrawerSettingExtension) = register(settingsItems, value.id, value)

    private fun <T : Any> register(target: LinkedHashMap<String, T>, id: String, value: T): AutoCloseable {
        require(id.matches(Regex("[A-Za-z0-9][A-Za-z0-9_.:/-]{0,255}"))) { "Invalid extension ID: $id" }
        val token = Any()
        synchronized(lock) {
            target[id] = value
            registrationTokens.getOrPut(target) { LinkedHashMap() }[id] = token
            revision++
        }
        notifyObservers()
        var closed = false
        return AutoCloseable {
            val removed = synchronized(lock) {
                if (closed) false else {
                    closed = true
                    val tokens = registrationTokens[target]
                    if (tokens?.get(id) === token) {
                        target.remove(id)
                        tokens.remove(id)
                        if (tokens.isEmpty()) registrationTokens.remove(target)
                        revision++
                        true
                    } else false
                }
            }
            if (removed) notifyObservers()
        }
    }

    fun snapshot(): ComposeSymbolDrawerExtensions = synchronized(lock) {
        fun <T> ordered(values: Collection<T>, priority: (T) -> Int, id: (T) -> String) =
            values.sortedWith(compareByDescending<T> { priority(it) }.thenBy { id(it) })
        ComposeSymbolDrawerExtensions(
            actions = ordered(actions.values, { it.priority }, { it.id }),
            itemRenderers = ordered(renderers.values, { it.priority }, { it.id }),
            groupIndicators = ordered(indicators.values, { it.priority }, { it.id }),
            groupProviders = ordered(providers.values, { it.priority }, { it.id }),
            themes = ordered(themes.values, { it.priority }, { it.id }),
            lifecycle = ordered(lifecycle.values, { it.priority }, { it.id }),
            settingsTabs = ordered(settingsTabs.values, { it.priority }, { it.id }),
            settingsItems = ordered(settingsItems.values, { it.priority }, { it.id })
        )
    }

    fun observe(listener: (Long) -> Unit): AutoCloseable {
        synchronized(lock) { observers += listener }
        return AutoCloseable { synchronized(lock) { observers -= listener } }
    }

    private fun notifyObservers() {
        val event = synchronized(lock) { revision to observers.toList() }
        event.second.forEach { runCatching { it(event.first) } }
    }

    fun clear() {
        synchronized(lock) {
            actions.clear(); renderers.clear(); indicators.clear(); providers.clear()
            themes.clear(); lifecycle.clear(); settingsTabs.clear(); settingsItems.clear()
            registrationTokens.clear()
            revision++
        }
        notifyObservers()
    }
}

/** Recompose when runtime extension registrations change. */
@Composable
fun rememberComposeSymbolDrawerExtensions(
    registry: ComposeSymbolDrawerExtensionRegistry
): ComposeSymbolDrawerExtensions {
    var revision by remember(registry) { mutableStateOf(0L) }
    var snapshot by remember(registry) { mutableStateOf(registry.snapshot()) }
    DisposableEffect(registry) {
        val registration = registry.observe {
            revision = it
            snapshot = registry.snapshot()
        }
        onDispose { registration.close() }
    }
    // Reading revision makes the state dependency explicit for Compose.
    revision
    return snapshot
}
