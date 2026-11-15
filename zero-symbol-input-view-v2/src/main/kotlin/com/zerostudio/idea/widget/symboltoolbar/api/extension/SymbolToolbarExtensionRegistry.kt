/*
 * Scoped extension registry for independent symbol toolbar APIs.
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.symboltoolbar.api.extension

import com.zerostudio.idea.widget.symboltoolbar.api.action.SymbolToolbarActionHandler
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Thread-safe registry for toolbar-specific extension points.
 *
 * Each extension category is independent: registering a custom group indicator,
 * item renderer, theme or settings page never registers or configures a drawer.
 * IDs are unique within a category, allowing one extension to contribute to
 * several categories under the same stable namespace.
 */
class SymbolToolbarExtensionRegistry {
    private val handlers = ConcurrentHashMap<String, SymbolToolbarActionHandler>()
    private val groupProviders = ConcurrentHashMap<String, SymbolToolbarGroupProvider>()
    private val itemViews = ConcurrentHashMap<String, SymbolToolbarItemViewExtension>()
    private val indicators = ConcurrentHashMap<String, SymbolToolbarGroupIndicatorExtension>()
    private val settings = ConcurrentHashMap<String, SymbolToolbarSettingsExtension>()
    private val themes = ConcurrentHashMap<String, SymbolToolbarThemeExtension>()
    private val interceptors = ConcurrentHashMap<String, SymbolToolbarInvocationInterceptor>()
    private val lifecycle = ConcurrentHashMap<String, SymbolToolbarLifecycleExtension>()
    private val actionContributions = ConcurrentHashMap<String, SymbolToolbarActionContribution>()
    private val listeners = CopyOnWriteArrayList<(SymbolToolbarExtensionSnapshot) -> Unit>()
    private val revisionCounter = AtomicLong(0L)

    val revision: Long get() = revisionCounter.get()

    fun registerAction(id: String, handler: SymbolToolbarActionHandler): AutoCloseable =
        register(handlers, id, handler)

    fun findAction(id: String): SymbolToolbarActionHandler? = handlers[id]
    fun registeredActionIds(): Set<String> = handlers.keys.toSortedSet()

    fun registerGroupProvider(extension: SymbolToolbarGroupProvider): AutoCloseable =
        registerExtension(groupProviders, extension)

    fun registerItemView(extension: SymbolToolbarItemViewExtension): AutoCloseable =
        registerExtension(itemViews, extension)

    fun registerGroupIndicator(extension: SymbolToolbarGroupIndicatorExtension): AutoCloseable =
        registerExtension(indicators, extension)

    fun registerSettings(extension: SymbolToolbarSettingsExtension): AutoCloseable =
        registerExtension(settings, extension)

    fun registerTheme(extension: SymbolToolbarThemeExtension): AutoCloseable =
        registerExtension(themes, extension)

    fun registerInvocationInterceptor(extension: SymbolToolbarInvocationInterceptor): AutoCloseable =
        registerExtension(interceptors, extension)

    fun registerLifecycle(extension: SymbolToolbarLifecycleExtension): AutoCloseable =
        registerExtension(lifecycle, extension)

    fun registerActionContribution(contribution: SymbolToolbarActionContribution): AutoCloseable =
        register(actionContributions, contribution.id, contribution)

    fun groupProviders(): List<SymbolToolbarGroupProvider> = ordered(groupProviders)
    fun itemViews(): List<SymbolToolbarItemViewExtension> = ordered(itemViews)
    fun groupIndicators(): List<SymbolToolbarGroupIndicatorExtension> = ordered(indicators)
    fun settingsExtensions(): List<SymbolToolbarSettingsExtension> = ordered(settings)
    fun themeExtensions(): List<SymbolToolbarThemeExtension> = ordered(themes)
    fun invocationInterceptors(): List<SymbolToolbarInvocationInterceptor> = ordered(interceptors)
    fun lifecycleExtensions(): List<SymbolToolbarLifecycleExtension> = ordered(lifecycle)
    fun actionContributions(): List<SymbolToolbarActionContribution> =
        actionContributions.values.sortedWith(compareBy({ it.priority }, { it.id }))

    fun observe(
        emitCurrent: Boolean = true,
        listener: (SymbolToolbarExtensionSnapshot) -> Unit
    ): AutoCloseable {
        listeners += listener
        if (emitCurrent) runCatching { listener(snapshot()) }
        val closed = AtomicBoolean(false)
        return AutoCloseable {
            if (closed.compareAndSet(false, true)) listeners.remove(listener)
        }
    }

    fun snapshot(): SymbolToolbarExtensionSnapshot = SymbolToolbarExtensionSnapshot(
        revision = revision,
        actionIds = registeredActionIds(),
        groupProviders = groupProviders(),
        itemViews = itemViews(),
        groupIndicators = groupIndicators(),
        settings = settingsExtensions(),
        themes = themeExtensions(),
        interceptors = invocationInterceptors(),
        lifecycle = lifecycleExtensions(),
        actionContributions = actionContributions()
    )

    /** Removes all runtime registrations; call when the owning host/session is disposed. */
    fun clear() {
        handlers.clear()
        groupProviders.clear()
        itemViews.clear()
        indicators.clear()
        settings.clear()
        themes.clear()
        interceptors.clear()
        lifecycle.clear()
        actionContributions.clear()
        publishChange()
    }

    private fun <T : SymbolToolbarExtension> registerExtension(
        target: ConcurrentHashMap<String, T>,
        extension: T
    ): AutoCloseable = register(target, extension.id, extension)

    private fun <T : Any> register(
        target: ConcurrentHashMap<String, T>,
        id: String,
        value: T
    ): AutoCloseable {
        require(id.matches(EXTENSION_ID)) {
            "Extension ID must be a stable namespaced identifier: $id"
        }
        check(target.putIfAbsent(id, value) == null) {
            "Extension already registered: $id"
        }
        publishChange()
        val closed = AtomicBoolean(false)
        return AutoCloseable {
            if (closed.compareAndSet(false, true) && target.remove(id, value)) {
                publishChange()
            }
        }
    }

    private fun <T : SymbolToolbarExtension> ordered(
        source: ConcurrentHashMap<String, T>
    ): List<T> = source.values.sortedWith(compareBy({ it.priority }, { it.id }))

    private fun publishChange() {
        revisionCounter.incrementAndGet()
        val current = snapshot()
        listeners.forEach { callback -> runCatching { callback(current) } }
    }

    private companion object {
        val EXTENSION_ID = Regex("[A-Za-z0-9][A-Za-z0-9_.:/-]{0,127}")
    }
}

/** Immutable registry view; lists are point-in-time snapshots, not live collections. */
data class SymbolToolbarExtensionSnapshot(
    val revision: Long,
    val actionIds: Set<String>,
    val groupProviders: List<SymbolToolbarGroupProvider>,
    val itemViews: List<SymbolToolbarItemViewExtension>,
    val groupIndicators: List<SymbolToolbarGroupIndicatorExtension>,
    val settings: List<SymbolToolbarSettingsExtension>,
    val themes: List<SymbolToolbarThemeExtension>,
    val interceptors: List<SymbolToolbarInvocationInterceptor>,
    val lifecycle: List<SymbolToolbarLifecycleExtension>,
    val actionContributions: List<SymbolToolbarActionContribution>
)
