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

/**
 * Semantic drawer events delivered to extensions while their contribution is
 * active. Composition attachment and disposal are deliberately not exposed:
 * Compose owns UI lifetime and cancels composition-scoped work automatically.
 */
sealed interface ComposeSymbolDrawerEvent {
    data class SettingsChanged(val settings: SymbolDrawerSettings) : ComposeSymbolDrawerEvent
    data class GroupsChanged(val groups: List<SymbolGroup>) : ComposeSymbolDrawerEvent
    data class GroupSelected(val index: Int, val group: SymbolGroup?) : ComposeSymbolDrawerEvent
    data class ExpansionChanged(val fraction: Float) : ComposeSymbolDrawerEvent
    data class SymbolInvoked(val symbol: SymbolEntry, val longPress: Boolean) : ComposeSymbolDrawerEvent
}

fun interface ComposeSymbolDrawerEventHandler {
    fun onEvent(event: ComposeSymbolDrawerEvent)
}

data class ComposeSymbolDrawerEventExtension(
    val id: String,
    val priority: Int = 0,
    val handler: ComposeSymbolDrawerEventHandler
)

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
    val events: List<ComposeSymbolDrawerEventExtension> = emptyList(),
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
    // Serialize observer delivery and capture the revision only after entering this lock.
    // Mutations may race after releasing `lock`; without ordered delivery, a slower
    // older notification can otherwise arrive after a newer one and roll UI state back.
    private val notificationLock = Any()
    private class ObserverEntry(val callback: (Long) -> Unit) {
        private var active = true

        /** Serialize callback start with close so no callback can begin after close returns. */
        fun dispatch(revision: Long) {
            synchronized(this) {
                if (active) callback(revision)
            }
        }

        fun close() {
            synchronized(this) { active = false }
        }
    }
    private data class ObserverNotification(
        val revision: Long,
        val listeners: List<ObserverEntry>
    )
    private val pendingNotifications = java.util.ArrayDeque<ObserverNotification>()
    private var dispatchingNotifications = false
    private val actions = LinkedHashMap<String, ComposeSymbolDrawerActionExtension>()
    private val renderers = LinkedHashMap<String, SymbolDrawerItemRenderer>()
    private val indicators = LinkedHashMap<String, SymbolDrawerGroupIndicator>()
    private val providers = LinkedHashMap<String, ComposeSymbolDrawerGroupProvider>()
    private val themes = LinkedHashMap<String, ComposeSymbolDrawerThemeExtension>()
    private val events = LinkedHashMap<String, ComposeSymbolDrawerEventExtension>()
    private val settingsTabs = LinkedHashMap<String, ComposeSymbolDrawerSettingsTabExtension>()
    private val settingsItems = LinkedHashMap<String, ComposeSymbolDrawerSettingExtension>()
    private val observers = LinkedHashSet<ObserverEntry>()
    // Per-registry-map registration tokens make replacement handles safe even
    // when the same extension instance is registered more than once.
    private val registrationTokens = IdentityHashMap<Any, MutableMap<String, Any>>()
    private var revision = 0L

    fun registerAction(value: ComposeSymbolDrawerActionExtension) = register(actions, value.id, value)
    fun registerItemRenderer(value: SymbolDrawerItemRenderer) = register(renderers, value.id, value)
    fun registerGroupIndicator(value: SymbolDrawerGroupIndicator) = register(indicators, value.id, value)
    fun registerGroupProvider(value: ComposeSymbolDrawerGroupProvider) = register(providers, value.id, value)
    fun registerTheme(value: ComposeSymbolDrawerThemeExtension) = register(themes, value.id, value)
    fun registerEvent(value: ComposeSymbolDrawerEventExtension) = register(events, value.id, value)
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
            events = ordered(events.values, { it.priority }, { it.id }),
            settingsTabs = ordered(settingsTabs.values, { it.priority }, { it.id }),
            settingsItems = ordered(settingsItems.values, { it.priority }, { it.id })
        )
    }

    /**
     * Observe registry revisions. [emitCurrent] queues the current revision in
     * the same serialized delivery stream as later mutations, avoiding a direct
     * callback racing or overtaking an in-flight observer notification.
     */
    fun observe(
        emitCurrent: Boolean = false,
        listener: (Long) -> Unit
    ): AutoCloseable {
        val entry = ObserverEntry(listener)
        val shouldDispatch = synchronized(notificationLock) {
            val currentRevision = synchronized(lock) {
                observers += entry
                revision
            }
            if (emitCurrent) {
                pendingNotifications.addLast(
                    ObserverNotification(currentRevision, listOf(entry))
                )
            }
            if (emitCurrent && !dispatchingNotifications) {
                dispatchingNotifications = true
                true
            } else false
        }
        if (shouldDispatch) drainNotifications()
        return AutoCloseable {
            // Invalidate queued snapshots as well as removing the observer from
            // future snapshots. Closing must prevent callbacks not yet started.
            entry.close()
            synchronized(lock) { observers -= entry }
        }
    }

    private fun notifyObservers() {
        val shouldDispatch = synchronized(notificationLock) {
            val event = synchronized(lock) {
                ObserverNotification(revision, observers.toList())
            }
            pendingNotifications.addLast(event)
            if (dispatchingNotifications) false else {
                dispatchingNotifications = true
                true
            }
        }
        if (shouldDispatch) drainNotifications()
    }

    /**
     * Drain a FIFO of immutable listener snapshots. Callbacks execute outside
     * both locks, so listeners can safely register/unregister extensions or
     * observers without deadlocking or recursively reordering notifications.
     */
    private fun drainNotifications() {
        while (true) {
            val event = synchronized(notificationLock) {
                if (pendingNotifications.isEmpty()) {
                    dispatchingNotifications = false
                    null
                } else pendingNotifications.removeFirst()
            } ?: break
            event.listeners.forEach { observer ->
                runCatching { observer.dispatch(event.revision) }
            }
        }
    }

    fun clear() {
        synchronized(lock) {
            actions.clear(); renderers.clear(); indicators.clear(); providers.clear()
            themes.clear(); events.clear(); settingsTabs.clear(); settingsItems.clear()
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
        // Queue the initial revision atomically with future observer events so a
        // registration racing the first snapshot cannot leave this Compose state stale.
        val registration = registry.observe(emitCurrent = true) {
            revision = it
            snapshot = registry.snapshot()
        }
        onDispose { registration.close() }
    }
    // Reading revision makes the state dependency explicit for Compose.
    revision
    return snapshot
}
