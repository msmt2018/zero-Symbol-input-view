/*
 * This file is part of zero-Symbol-input-view-V2.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.extension

import androidx.compose.runtime.Composable
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.RegistryBackedSymbolActionHost
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionHandler
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionHost
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionId
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionRegistry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.legacy.SymbolItem
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/** Stable identifiers for built-in drawer features. */
object SymbolDrawerBuiltInFeatures {
    const val SEARCH = "builtin.search"
    const val FAVORITES = "builtin.favorites"
    const val RECENT = "builtin.recent"
    const val SETTINGS = "builtin.settings"
    const val MANAGER = "builtin.manager"
}

/** Session registry for editor-neutral actions and Compose content extensions. */
class SymbolDrawerExtensionRegistry {
    private data class Registered<T>(val token: Any, val value: T)
    private val lock = Any()
    private val notificationLock = Any()
    private val actions = LinkedHashMap<String, Registered<SymbolDrawerActionExtension>>()
    private val indicators = LinkedHashMap<String, Registered<SymbolGroupIndicatorExtension>>()
    private val itemRenderers = LinkedHashMap<String, Registered<SymbolDrawerItemRenderer>>()
    private val groupProviders = LinkedHashMap<String, Registered<SymbolDrawerLegacyGroupProvider>>()
    private val settingsTabs = LinkedHashMap<String, Registered<SymbolDrawerSettingsTabExtension>>()
    private val settingsItems = LinkedHashMap<String, Registered<SymbolDrawerSettingExtension>>()
    private val themes = LinkedHashMap<String, Registered<SymbolDrawerThemeExtension>>()
    private val builtInVisibility = LinkedHashMap<String, Boolean>()
    private val listeners = CopyOnWriteArrayList<(SymbolDrawerExtensionSnapshot) -> Unit>()
    @Volatile private var revision: Long = 0L
    @Volatile var onExtensionErrorListener: ((extensionId: String, error: Throwable) -> Unit)? = null

    internal fun reportError(extensionId: String, error: Throwable) {
        runCatching { onExtensionErrorListener?.invoke(extensionId, error) }
    }

    fun revision(): Long = revision

    internal fun observe(
        emitCurrent: Boolean = true,
        listener: (SymbolDrawerExtensionSnapshot) -> Unit
    ): AutoCloseable {
        listeners += listener
        if (emitCurrent) runCatching { listener(snapshot()) }
        return AutoCloseable { listeners.remove(listener) }
    }

    fun snapshot(): SymbolDrawerExtensionSnapshot = synchronized(lock) {
        SymbolDrawerExtensionSnapshot(
            revision = revision,
            actions = actions.values.map { it.value }.sortedWith(compareBy({ it.priority }, { it.id })),
            indicators = indicators.values.map { it.value }.sortedWith(compareBy({ it.priority }, { it.id })),
            itemRenderers = itemRenderers.values.map { it.value }.sortedWith(compareBy({ it.priority }, { it.id })),
            groupProviders = groupProviders.values.map { it.value }.sortedWith(compareBy({ it.priority }, { it.id })),
            settingsTabs = settingsTabs.values.map { it.value }.sortedWith(compareBy({ it.priority }, { it.id })),
            settingsItems = settingsItems.values.map { it.value }.sortedWith(compareBy({ it.priority }, { it.id })),
            themes = themes.values.map { it.value }.sortedWith(compareBy({ it.priority }, { it.id })),
            builtInVisibility = builtInVisibility.toMap()
        )
    }

    private fun notifyChanged() = synchronized(notificationLock) {
        val next = synchronized(lock) {
            revision += 1
            snapshot()
        }
        listeners.forEach { callback -> runCatching { callback(next) } }
    }

    val actionHandlers = SymbolActionRegistry<SymbolActionHost>()

    fun registerAction(extension: SymbolDrawerActionExtension): AutoCloseable =
        register(actions, extension.id, extension) {
            actionHandlers.register(SymbolActionId.extension(extension.id), extension.handler)
        }

    fun registerLegacyAction(
        actionId: Int,
        handler: SymbolActionHandler<SymbolActionHost>
    ): AutoCloseable {
        require(actionId >= 0) { "Legacy action ID must be non-negative" }
        val registration = actionHandlers.register(actionId, handler)
        notifyChanged()
        val closed = AtomicBoolean(false)
        return AutoCloseable {
            if (closed.compareAndSet(false, true)) {
                registration.close()
                notifyChanged()
            }
        }
    }

    fun registerIndicator(extension: SymbolGroupIndicatorExtension): AutoCloseable =
        register(indicators, extension.id, extension)

    fun registerItemRenderer(extension: SymbolDrawerItemRenderer): AutoCloseable =
        register(itemRenderers, extension.id, extension)

    fun registerGroupProvider(extension: SymbolDrawerLegacyGroupProvider): AutoCloseable =
        register(groupProviders, extension.id, extension)

    fun registerSettingsTab(extension: SymbolDrawerSettingsTabExtension): AutoCloseable =
        register(settingsTabs, extension.id, extension)

    fun registerSetting(extension: SymbolDrawerSettingExtension): AutoCloseable =
        register(settingsItems, extension.id, extension)

    fun registerTheme(extension: SymbolDrawerThemeExtension): AutoCloseable =
        register(themes, extension.id, extension)

    private fun <T> register(
        target: LinkedHashMap<String, Registered<T>>,
        id: String,
        value: T,
        afterInsert: (() -> AutoCloseable)? = null
    ): AutoCloseable {
        require(id.matches(EXTENSION_ID)) { "Invalid extension ID: $id" }
        val token = Any()
        val secondaryHandle = synchronized(lock) {
            val previous = target[id]
            target[id] = Registered(token, value)
            try {
                afterInsert?.invoke()
            } catch (error: Throwable) {
                if (target[id]?.token === token) {
                    if (previous == null) target.remove(id) else target[id] = previous
                }
                throw error
            }
        }
        notifyChanged()
        var closed = false
        return AutoCloseable {
            val shouldClose = synchronized(lock) {
                if (closed) false else { closed = true; true }
            }
            if (shouldClose) {
                val removed = synchronized(lock) {
                    if (target[id]?.token === token) {
                        target.remove(id)
                        true
                    } else false
                }
                if (removed) notifyChanged()
                secondaryHandle?.close()
            }
        }
    }

    fun actions(): List<SymbolDrawerActionExtension> =
        snapshot().actions

    fun indicators(): List<SymbolGroupIndicatorExtension> =
        snapshot().indicators

    fun itemRenderers(): List<SymbolDrawerItemRenderer> =
        snapshot().itemRenderers

    fun groupProviders(): List<SymbolDrawerLegacyGroupProvider> =
        snapshot().groupProviders

    fun settingsTabs(): List<SymbolDrawerSettingsTabExtension> =
        snapshot().settingsTabs

    fun settingsItems(): List<SymbolDrawerSettingExtension> =
        snapshot().settingsItems

    fun themes(): List<SymbolDrawerThemeExtension> =
        snapshot().themes

    fun setBuiltInVisible(featureId: String, visible: Boolean) {
        require(featureId.matches(EXTENSION_ID))
        val changed = synchronized(lock) { builtInVisibility.put(featureId, visible) != visible }
        if (changed) notifyChanged()
    }

    fun isBuiltInVisible(featureId: String, defaultVisible: Boolean = true): Boolean =
        synchronized(lock) { builtInVisibility[featureId] ?: defaultVisible }

    fun builtInVisibilitySnapshot(): Map<String, Boolean> =
        synchronized(lock) { builtInVisibility.toMap() }

    fun wrapActionHost(delegate: SymbolActionHost): SymbolActionHost =
        RegistryBackedSymbolActionHost(delegate, actionHandlers)

    /** Removes registrations without invoking any UI lifecycle callback. */
    fun clear() {
        synchronized(lock) {
            actions.clear()
            indicators.clear()
            itemRenderers.clear()
            groupProviders.clear()
            settingsTabs.clear()
            settingsItems.clear()
            themes.clear()
            builtInVisibility.clear()
            actionHandlers.clear()
        }
        notifyChanged()
    }

    private companion object {
        val EXTENSION_ID = Regex("[A-Za-z0-9][A-Za-z0-9_.:/-]{0,255}")
    }
}

data class SymbolDrawerActionExtension(
    val id: String,
    val title: String,
    val description: String = "",
    val payloadHint: String? = null,
    val priority: Int = 0,
    val handler: SymbolActionHandler<SymbolActionHost>
) {
    init { require(id.isNotBlank()); require(title.isNotBlank()) }
}

interface SymbolDrawerLegacyGroupProvider {
    val id: String
    val title: String
    val priority: Int get() = 0
    suspend fun loadGroups(): List<com.zerostudio.idea.widget.editor.symbolinput.v2.model.legacy.SymbolGroup>
}

data class SymbolDrawerItemRenderState(
    val groupIndex: Int,
    val position: Int,
    val selected: Boolean = false,
    val pressed: Boolean = false,
    val enabled: Boolean = true,
    val theme: SymbolDrawerTheme? = null
)

/** Compose slot for replacing one symbol cell's visual content. */
interface SymbolDrawerItemRenderer {
    val id: String
    val title: String
    val description: String get() = ""
    val priority: Int get() = 0
    fun accepts(entry: SymbolEntry): Boolean = true
    @Composable
    fun Content(
        entry: SymbolEntry,
        state: SymbolDrawerItemRenderState,
        onClick: () -> Unit,
        onLongClick: (() -> Unit)?
    )
}

/** Stable navigation snapshot supplied to custom indicator content. */
data class SymbolGroupIndicatorState(
    val groups: List<SymbolGroup>,
    val selectedIndex: Int,
    val onSelectGroup: (Int) -> Unit
)

/** Compose slot for a custom group indicator. */
interface SymbolGroupIndicatorExtension {
    val id: String
    val title: String
    val description: String get() = ""
    val priority: Int get() = 0
    @Composable
    fun Content(state: SymbolGroupIndicatorState)
}

/** Host-owned composable settings surface contributed as a settings tab. */
interface SymbolDrawerSettingsTabExtension {
    val id: String
    val title: String
    val priority: Int get() = 0
    @Composable
    fun Content(scope: SymbolDrawerSettingsScope)
}

/** Host-owned composable setting row/card placed in a built-in tab. */
interface SymbolDrawerSettingExtension {
    val id: String
    val tabId: String
    val title: String
    val priority: Int get() = 0
    @Composable
    fun Content(scope: SymbolDrawerSettingsScope)
}

/** Read/write boundary for host-defined settings contributions. */
interface SymbolDrawerSettingsScope {
    val current: SymbolDrawerSettings
    fun update(transform: (SymbolDrawerSettings) -> SymbolDrawerSettings)
}

/** Public theme aliases. */
typealias SymbolDrawerTheme = com.zerostudio.idea.widget.drawer.extension.SymbolDrawerTheme
typealias SymbolDrawerThemeExtension = com.zerostudio.idea.widget.drawer.extension.SymbolDrawerThemeExtension

data class SymbolDrawerExtensionSnapshot(
    val revision: Long,
    val actions: List<SymbolDrawerActionExtension>,
    val indicators: List<SymbolGroupIndicatorExtension>,
    val itemRenderers: List<SymbolDrawerItemRenderer>,
    val groupProviders: List<SymbolDrawerLegacyGroupProvider>,
    val settingsTabs: List<SymbolDrawerSettingsTabExtension>,
    val settingsItems: List<SymbolDrawerSettingExtension>,
    val themes: List<SymbolDrawerThemeExtension>,
    val builtInVisibility: Map<String, Boolean>
)
