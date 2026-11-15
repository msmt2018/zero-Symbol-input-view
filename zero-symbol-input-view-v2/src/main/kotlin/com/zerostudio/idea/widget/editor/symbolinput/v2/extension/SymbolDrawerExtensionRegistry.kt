/*
 * This file is part of ZeroStudio.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.extension

import android.content.Context
import android.view.View
import java.util.concurrent.CopyOnWriteArrayList
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.RegistryBackedSymbolActionHost
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionHandler
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionHost
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionId
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.SymbolActionRegistry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings

/**
 * Process/session scoped extension point registry for a V2 drawer instance.
 *
 * Registrations are runtime objects and are deliberately not serialized. Persist
 * stable IDs and configuration in the host application, then register extensions
 * again when the editor/session is recreated. Closing a registration handle
 * unregisters that contribution; built-in features are hidden, never removed.
 */
class SymbolDrawerExtensionRegistry {
    private data class Registered<T>(val token: Any, val value: T)
    private val lock = Any()
    private val actions = LinkedHashMap<String, Registered<SymbolDrawerActionExtension>>()
    private val indicators = LinkedHashMap<String, Registered<SymbolGroupIndicatorExtension>>()
    private val settingsTabs = LinkedHashMap<String, Registered<SymbolDrawerSettingsTabExtension>>()
    private val settingsItems = LinkedHashMap<String, Registered<SymbolDrawerSettingExtension>>()
    private val themes = LinkedHashMap<String, Registered<SymbolDrawerThemeExtension>>()
    private val builtInVisibility = LinkedHashMap<String, Boolean>()
    private val listeners = CopyOnWriteArrayList<(SymbolDrawerExtensionSnapshot) -> Unit>()
    @Volatile private var revision: Long = 0L

    /** Monotonic registry revision, useful for UI diffing and cache invalidation. */
    fun revision(): Long = revision

    /** Observe live contribution changes. The returned handle removes the listener. */
    fun observe(
        emitCurrent: Boolean = true,
        listener: (SymbolDrawerExtensionSnapshot) -> Unit
    ): AutoCloseable {
        listeners += listener
        if (emitCurrent) listener(snapshot())
        return AutoCloseable { listeners.remove(listener) }
    }

    /** Immutable snapshot for UI hosts to render without holding the registry lock. */
    fun snapshot(): SymbolDrawerExtensionSnapshot = synchronized(lock) {
        SymbolDrawerExtensionSnapshot(
            revision = revision,
            actions = actions.values.map { it.value }.sortedWith(compareBy({ it.priority }, { it.id })),
            indicators = indicators.values.map { it.value }.sortedWith(compareBy({ it.priority }, { it.id })),
            settingsTabs = settingsTabs.values.map { it.value }.sortedWith(compareBy({ it.priority }, { it.id })),
            settingsItems = settingsItems.values.map { it.value }.sortedWith(compareBy({ it.priority }, { it.id })),
            themes = themes.values.map { it.value }.sortedWith(compareBy({ it.priority }, { it.id })),
            builtInVisibility = builtInVisibility.toMap()
        )
    }

    private fun notifyChanged() {
        val next = synchronized(lock) { revision += 1; snapshot() }
        listeners.forEach { callback -> runCatching { callback(next) } }
    }

    /** Action registry used by the extension-aware SymbolActionHost decorator. */
    val actionHandlers = SymbolActionRegistry<SymbolActionHost>()

    fun registerAction(extension: SymbolDrawerActionExtension): AutoCloseable =
        register(actions, extension.id, extension) {
            actionHandlers.register(SymbolActionId.extension(extension.id), extension.handler)
        }

    /**
     * Registers a handler against the original V1 numeric action ID.
     *
     * Legacy SymbolItem.shortAction/longAction values are dispatched without
     * translating them into a second string-based action schema. The payload
     * is passed through unchanged from shortText/longText. This is the preferred
     * extension point when a symbol is authored with the legacy SymbolData model.
     */
    fun registerLegacyAction(
        actionId: Int,
        handler: SymbolActionHandler<SymbolActionHost>
    ): AutoCloseable {
        require(actionId >= 0) { "Legacy action ID must be non-negative" }
        val registration = actionHandlers.register(actionId, handler)
        notifyChanged()
        var closed = false
        return AutoCloseable {
            if (!closed) {
                closed = true
                registration.close()
                notifyChanged()
            }
        }
    }

    fun registerIndicator(extension: SymbolGroupIndicatorExtension): AutoCloseable =
        register(indicators, extension.id, extension)

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
        synchronized(lock) { target[id] = Registered(token, value) }
        val secondaryHandle = afterInsert?.invoke()
        notifyChanged()
        var closed = false
        return AutoCloseable {
            val removed = synchronized(lock) {
                if (closed) {
                    false
                } else {
                    closed = true
                    if (target[id]?.token === token) {
                        target.remove(id)
                        true
                    } else false
                }
            }
            if (removed) notifyChanged()
            secondaryHandle?.close()
        }
    }

    /** Returns an immutable, priority-sorted snapshot of custom action entries. */
    fun actions(): List<SymbolDrawerActionExtension> =
        synchronized(lock) { actions.values.map { it.value }.sortedWith(compareBy({ it.priority }, { it.id })) }

    fun indicators(): List<SymbolGroupIndicatorExtension> =
        synchronized(lock) { indicators.values.map { it.value }.sortedWith(compareBy({ it.priority }, { it.id })) }

    fun settingsTabs(): List<SymbolDrawerSettingsTabExtension> =
        synchronized(lock) { settingsTabs.values.map { it.value }.sortedWith(compareBy({ it.priority }, { it.id })) }

    fun settingsItems(): List<SymbolDrawerSettingExtension> =
        synchronized(lock) { settingsItems.values.map { it.value }.sortedWith(compareBy({ it.priority }, { it.id })) }

    fun themes(): List<SymbolDrawerThemeExtension> =
        synchronized(lock) { themes.values.map { it.value }.sortedWith(compareBy({ it.priority }, { it.id })) }

    /**
     * Hides/shows a built-in feature by stable feature ID. This changes runtime
     * availability only; it never deletes code or rewrites symbol data.
     */
    fun setBuiltInVisible(featureId: String, visible: Boolean) {
        require(featureId.matches(EXTENSION_ID))
        val changed = synchronized(lock) { builtInVisibility.put(featureId, visible) != visible }
        if (changed) notifyChanged()
    }

    fun isBuiltInVisible(featureId: String, defaultVisible: Boolean = true): Boolean =
        synchronized(lock) { builtInVisibility[featureId] ?: defaultVisible }

    fun builtInVisibilitySnapshot(): Map<String, Boolean> =
        synchronized(lock) { builtInVisibility.toMap() }

    /** Decorates a host so SymbolAction.Extension resolves registered app handlers first. */
    fun wrapActionHost(delegate: SymbolActionHost): SymbolActionHost =
        RegistryBackedSymbolActionHost(delegate, actionHandlers)

    /** Removes all runtime contributions. Call when the owning drawer/session is disposed. */
    fun clear() {
        synchronized(lock) {
            actions.clear()
            indicators.clear()
            settingsTabs.clear()
            settingsItems.clear()
            themes.clear()
            builtInVisibility.clear()
        }
        actionHandlers.clear()
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
    init { require(title.isNotBlank()) }
}

/** Stable navigation contract supplied to custom indicator components. */
data class SymbolGroupIndicatorState(
    val groups: List<SymbolGroup>,
    val selectedIndex: Int,
    val onSelectGroup: (Int) -> Unit
)

/** A custom indicator owns its View but not the underlying group/page state. */
interface SymbolGroupIndicatorComponent {
    val view: View
    fun submitGroups(groups: List<SymbolGroup>)
    fun setSelectedIndex(index: Int)
    fun applySettings(settings: SymbolDrawerSettings)
    /** Optional theme tokens supplied by the owning drawer session. */
    fun applyTheme(theme: SymbolDrawerTheme?) = Unit
    /** Release component-owned animators/listeners when it is replaced or removed. */
    fun onDispose() = Unit
}

/** A custom indicator is automatically discoverable from the extension registry. */
interface SymbolGroupIndicatorExtension {
    val id: String
    val title: String
    val description: String get() = ""
    val priority: Int get() = 0
    fun create(context: Context, initialState: SymbolGroupIndicatorState): SymbolGroupIndicatorComponent
}

/** Host-owned settings surface contributed as a tab in the settings screen. */
interface SymbolDrawerSettingsTabExtension {
    val id: String
    val title: String
    val priority: Int get() = 0
    fun createView(context: Context, scope: SymbolDrawerSettingsScope): View
}

/** Host-owned setting row/card that can be placed in an existing built-in tab. */
interface SymbolDrawerSettingExtension {
    val id: String
    val tabId: String
    val title: String
    val priority: Int get() = 0
    fun createView(context: Context, scope: SymbolDrawerSettingsScope): View
}

/** Read/write boundary for host-defined settings contributions. */
interface SymbolDrawerSettingsScope {
    val current: SymbolDrawerSettings
    fun update(transform: (SymbolDrawerSettings) -> SymbolDrawerSettings)
}

/** Theme token snapshot; values are Android ARGB colors. */
data class SymbolDrawerTheme(
    val primary: Int,
    val onPrimary: Int,
    val surface: Int,
    val onSurface: Int,
    val secondaryText: Int,
    val outline: Int,
    val drawerBackground: Int = surface,
    val selectedCellBackground: Int = primary
)

/** Theme providers may implement static palettes or dynamic host/system palettes. */
interface SymbolDrawerThemeExtension {
    val id: String
    val title: String
    val priority: Int get() = 0
    fun resolve(context: Context, settings: SymbolDrawerSettings): SymbolDrawerTheme
}

/** Immutable point-in-time view of every extension contribution. */
data class SymbolDrawerExtensionSnapshot(
    val revision: Long,
    val actions: List<SymbolDrawerActionExtension>,
    val indicators: List<SymbolGroupIndicatorExtension>,
    val settingsTabs: List<SymbolDrawerSettingsTabExtension>,
    val settingsItems: List<SymbolDrawerSettingExtension>,
    val themes: List<SymbolDrawerThemeExtension>,
    val builtInVisibility: Map<String, Boolean>
)
