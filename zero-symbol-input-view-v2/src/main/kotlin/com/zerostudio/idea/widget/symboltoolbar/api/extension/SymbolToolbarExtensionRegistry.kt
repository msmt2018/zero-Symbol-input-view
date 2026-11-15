/*
 * Scoped extension registry for symbol toolbar controls.
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.symboltoolbar.api.extension

import com.zerostudio.idea.widget.symboltoolbar.api.action.SymbolToolbarActionHandler
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/** Thread-safe registry. Action handlers are keyed by the numeric IDs in SymbolItem. */
class SymbolToolbarExtensionRegistry {
    private val handlers = ConcurrentHashMap<Int, SymbolToolbarActionHandler>()
    private val groupProviders = ConcurrentHashMap<String, SymbolToolbarGroupProvider>()
    private val itemViews = ConcurrentHashMap<String, SymbolToolbarItemViewExtension>()
    private val indicators = ConcurrentHashMap<String, SymbolToolbarGroupIndicatorExtension>()
    private val settings = ConcurrentHashMap<String, SymbolToolbarSettingsExtension>()
    private val themes = ConcurrentHashMap<String, SymbolToolbarThemeExtension>()
    private val interceptors = ConcurrentHashMap<String, SymbolToolbarInvocationInterceptor>()
    private val lifecycle = ConcurrentHashMap<String, SymbolToolbarLifecycleExtension>()
    private val actionContributions = ConcurrentHashMap<Int, SymbolToolbarActionContribution>()
    private val listeners = CopyOnWriteArrayList<(SymbolToolbarExtensionSnapshot) -> Unit>()
    private val revisionCounter = AtomicLong(0L)

    val revision: Long get() = revisionCounter.get()

    fun registerAction(actionId: Int, handler: SymbolToolbarActionHandler): AutoCloseable {
        require(actionId >= 0) { "Action ID must be non-negative" }
        check(handlers.putIfAbsent(actionId, handler) == null) { "Action ID already registered: $actionId" }
        publishChange()
        val closed = AtomicBoolean(false)
        return AutoCloseable {
            if (closed.compareAndSet(false, true) && handlers.remove(actionId, handler)) publishChange()
        }
    }

    fun findAction(actionId: Int): SymbolToolbarActionHandler? = handlers[actionId]
    fun registeredActionIds(): Set<Int> = handlers.keys.toSortedSet()

    fun registerGroupProvider(extension: SymbolToolbarGroupProvider) = registerExtension(groupProviders, extension)
    fun registerItemView(extension: SymbolToolbarItemViewExtension) = registerExtension(itemViews, extension)
    fun registerGroupIndicator(extension: SymbolToolbarGroupIndicatorExtension) = registerExtension(indicators, extension)
    fun registerSettings(extension: SymbolToolbarSettingsExtension) = registerExtension(settings, extension)
    fun registerTheme(extension: SymbolToolbarThemeExtension) = registerExtension(themes, extension)
    fun registerInvocationInterceptor(extension: SymbolToolbarInvocationInterceptor) = registerExtension(interceptors, extension)
    fun registerLifecycle(extension: SymbolToolbarLifecycleExtension) = registerExtension(lifecycle, extension)

    fun registerActionContribution(contribution: SymbolToolbarActionContribution): AutoCloseable =
        register(actionContributions, contribution.actionId, contribution)

    fun groupProviders(): List<SymbolToolbarGroupProvider> = ordered(groupProviders)
    fun itemViews(): List<SymbolToolbarItemViewExtension> = ordered(itemViews)
    fun groupIndicators(): List<SymbolToolbarGroupIndicatorExtension> = ordered(indicators)
    fun settingsExtensions(): List<SymbolToolbarSettingsExtension> = ordered(settings)
    fun themeExtensions(): List<SymbolToolbarThemeExtension> = ordered(themes)
    fun invocationInterceptors(): List<SymbolToolbarInvocationInterceptor> = ordered(interceptors)
    fun lifecycleExtensions(): List<SymbolToolbarLifecycleExtension> = ordered(lifecycle)
    fun actionContributions(): List<SymbolToolbarActionContribution> =
        actionContributions.values.sortedWith(compareBy({ it.priority }, { it.actionId }))

    fun observe(emitCurrent: Boolean = true, listener: (SymbolToolbarExtensionSnapshot) -> Unit): AutoCloseable {
        listeners += listener
        if (emitCurrent) runCatching { listener(snapshot()) }
        val closed = AtomicBoolean(false)
        return AutoCloseable { if (closed.compareAndSet(false, true)) listeners.remove(listener) }
    }

    fun snapshot() = SymbolToolbarExtensionSnapshot(
        revision, registeredActionIds(), groupProviders(), itemViews(), groupIndicators(),
        settingsExtensions(), themeExtensions(), invocationInterceptors(), lifecycleExtensions(), actionContributions()
    )

    fun clear() {
        handlers.clear(); groupProviders.clear(); itemViews.clear(); indicators.clear()
        settings.clear(); themes.clear(); interceptors.clear(); lifecycle.clear(); actionContributions.clear()
        publishChange()
    }

    private fun <T : SymbolToolbarExtension> registerExtension(
        target: ConcurrentHashMap<String, T>, extension: T
    ): AutoCloseable = register(target, extension.id, extension)

    private fun <T : Any> register(target: ConcurrentHashMap<String, T>, id: String, value: T): AutoCloseable {
        require(id.matches(EXTENSION_ID)) { "Invalid extension ID: $id" }
        check(target.putIfAbsent(id, value) == null) { "Extension already registered: $id" }
        publishChange()
        val closed = AtomicBoolean(false)
        return AutoCloseable { if (closed.compareAndSet(false, true) && target.remove(id, value)) publishChange() }
    }

    private fun <T : SymbolToolbarExtension> ordered(source: ConcurrentHashMap<String, T>): List<T> =
        source.values.sortedWith(compareBy({ it.priority }, { it.id }))

    private fun publishChange() {
        revisionCounter.incrementAndGet()
        val current = snapshot()
        listeners.forEach { callback -> runCatching { callback(current) } }
    }

    private companion object { val EXTENSION_ID = Regex("[A-Za-z0-9][A-Za-z0-9_.:/-]{0,127}") }
}

data class SymbolToolbarExtensionSnapshot(
    val revision: Long,
    val actionIds: Set<Int>,
    val groupProviders: List<SymbolToolbarGroupProvider>,
    val itemViews: List<SymbolToolbarItemViewExtension>,
    val groupIndicators: List<SymbolToolbarGroupIndicatorExtension>,
    val settings: List<SymbolToolbarSettingsExtension>,
    val themes: List<SymbolToolbarThemeExtension>,
    val interceptors: List<SymbolToolbarInvocationInterceptor>,
    val lifecycle: List<SymbolToolbarLifecycleExtension>,
    val actionContributions: List<SymbolToolbarActionContribution>
)
