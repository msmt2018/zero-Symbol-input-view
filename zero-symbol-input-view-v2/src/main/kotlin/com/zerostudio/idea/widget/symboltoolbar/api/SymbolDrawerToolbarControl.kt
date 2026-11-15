/*
 * Runtime adapter connecting the public toolbar API to the actual V2 drawer View.
 * Public callers use the canonical legacy SymbolGroup/SymbolItem data contract;
 * rendering, settings and persistence remain owned by the V2 drawer components.
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.symboltoolbar.api

import android.content.Context
import android.view.View
import com.google.gson.Gson
import com.zerostudio.idea.widget.editor.symbolinput.v2.SymbolDrawerView
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.LegacySymbolDataCodec
import com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolDrawerSettingExtension
import com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolDrawerSettingsScope
import com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolDrawerSettingsTabExtension
import com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolDrawerTheme
import com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolDrawerThemeExtension
import com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolGroupIndicatorComponent
import com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolGroupIndicatorExtension
import com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolGroupIndicatorState
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup as RuntimeSymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettingsRepository
import com.zerostudio.idea.widget.symboltoolbar.api.action.SymbolToolbarActionHost
import com.zerostudio.idea.widget.symboltoolbar.api.event.SymbolToolbarGroupEvent
import com.zerostudio.idea.widget.symboltoolbar.api.event.SymbolToolbarItemEvent
import com.zerostudio.idea.widget.symboltoolbar.api.extension.SymbolToolbarExtensionRegistry
import com.zerostudio.idea.widget.symboltoolbar.api.extension.SymbolToolbarExtensionRuntime
import com.zerostudio.idea.widget.symboltoolbar.api.extension.SymbolToolbarExtensionSnapshot
import com.zerostudio.idea.widget.symboltoolbar.api.extension.SymbolToolbarGroupIndicatorExtension
import com.zerostudio.idea.widget.symboltoolbar.api.extension.SymbolToolbarSettingsExtension
import com.zerostudio.idea.widget.symboltoolbar.api.extension.SymbolToolbarThemeExtension
import com.zerostudio.idea.widget.symboltoolbar.api.extension.SymbolToolbarItemViewExtension
import com.zerostudio.idea.widget.symboltoolbar.api.extension.SymbolToolbarItemViewState
import com.zerostudio.idea.widget.symboltoolbar.api.extension.SymbolToolbarSettingsScope
import com.zerostudio.idea.widget.symboltoolbar.api.model.SymbolToolbarInvocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Concrete bridge to SymbolDrawerView. It does not retain or require an editor
 * core object; the host supplies only action capabilities and a coroutine scope.
 *
 * Action handlers, group indicators, themes and settings extensions are bridged
 * into the real drawer registry. Custom item renderers are exposed through
 * create/bind helpers for hosts that supply their own page-cell renderer.
 */
class SymbolDrawerToolbarControl(
    private val drawer: SymbolDrawerView,
    actionHost: SymbolToolbarActionHost,
    private val coroutineScope: CoroutineScope,
    private val settingsRepository: SymbolDrawerSettingsRepository,
    private val controlId: String = "symbol-drawer-toolbar"
) : SymbolToolbarControl, AutoCloseable {
    private val gson = Gson()
    private val listeners = CopyOnWriteArrayList<SymbolToolbarListener>()
    private val closed = AtomicBoolean(false)
    val extensions = SymbolToolbarExtensionRegistry()
    val runtime = SymbolToolbarExtensionRuntime(controlId, extensions, actionHost)

    private val indicatorHandles = LinkedHashMap<String, AutoCloseable>()
    private val themeHandles = LinkedHashMap<String, AutoCloseable>()
    private val settingsTabHandles = LinkedHashMap<String, AutoCloseable>()
    private val extensionObserver: AutoCloseable

    private var legacyGroups: List<com.zerostudio.idea.widget.editor.symbolinput.v2.model.legacy.SymbolGroup> = emptyList()
    private var runtimeGroups: List<RuntimeSymbolGroup> = emptyList()
    private var currentState = SymbolToolbarState()
    private var settings = settingsRepository.load().settings

    init {
        drawer.onGroupChangedListener = { index, _ ->
            if (!closed.get()) {
                val selected = legacyGroups.getOrNull(index)
                if (selected != null) {
                    runtime.events.publish(SymbolToolbarGroupEvent.GroupSelected(
                        controlId, selected, currentState.selectedGroupIndex, index, false,
                        runtime.events.timestamp(), runtime.events.nextSequence()
                    ))
                    listeners.forEach { listener -> runCatching { listener.onGroupSelected(selected) } }
                }
                updateState(index)
            }
        }
        drawer.onSymbolActionListener = SymbolDrawerView.OnSymbolActionListener { symbol, longPress ->
            dispatchSymbol(symbol, longPress)
        }
        extensionObserver = extensions.observe { snapshot -> syncDrawerExtensions(snapshot) }
        updateState(drawer.getCurrentGroupIndex())
    }

    override val state: SymbolToolbarState get() = currentState

    override fun submitGroups(groups: List<com.zerostudio.idea.widget.editor.symbolinput.v2.model.legacy.SymbolGroup>) {
        checkActive()
        val decoded = LegacySymbolDataCodec.decode(gson.toJson(groups))
        legacyGroups = groups.toList()
        runtimeGroups = decoded
        runtime.events.publish(SymbolToolbarGroupEvent.GroupsChanged(
            controlId, legacyGroups, runtime.events.timestamp(), runtime.events.nextSequence()
        ))
        drawer.submitGroups(decoded)
        updateState(drawer.getCurrentGroupIndex())
    }

    /** Loads every registered group provider in deterministic priority/ID order. */
    suspend fun refreshGroupsFromProviders(): List<com.zerostudio.idea.widget.editor.symbolinput.v2.model.legacy.SymbolGroup> {
        checkActive()
        val supplied = extensions.groupProviders().flatMap { it.loadGroups() }
        submitGroups(supplied)
        return legacyGroups
    }

    override fun selectGroup(index: Int, animated: Boolean): Boolean {
        checkActive()
        if (index !in legacyGroups.indices) return false
        drawer.selectGroup(index, animated)
        updateState(index)
        return true
    }

    override fun updateSettings(settings: SymbolDrawerSettings) {
        checkActive()
        val previous = this.settings
        settingsRepository.save(settings)
        this.settings = settings
        drawer.applySettings(settings)
        runtime.onSettingsChanged(settings)
        runtime.events.publish(com.zerostudio.idea.widget.symboltoolbar.api.event.SymbolToolbarStateEvent.SettingsChanged(
            controlId, previous, settings, runtime.events.timestamp(), runtime.events.nextSequence()
        ))
    }

    override fun addListener(listener: SymbolToolbarListener): AutoCloseable {
        checkActive()
        listeners += listener
        val disposed = AtomicBoolean(false)
        return AutoCloseable { if (disposed.compareAndSet(false, true)) listeners.remove(listener) }
    }

    /** Returns the currently registered custom cell renderers in deterministic order. */
    fun itemViewExtensions(): List<SymbolToolbarItemViewExtension> = extensions.itemViews()

    fun createCustomItemView(
        extensionId: String,
        context: Context,
        item: com.zerostudio.idea.widget.editor.symbolinput.v2.model.legacy.SymbolItem,
        state: SymbolToolbarItemViewState
    ): View? {
        val extension = extensions.itemViews().firstOrNull { it.id == extensionId } ?: return null
        return extension.createView(context, item, state)
    }

    fun bindCustomItemView(
        extensionId: String,
        view: View,
        item: com.zerostudio.idea.widget.editor.symbolinput.v2.model.legacy.SymbolItem,
        state: SymbolToolbarItemViewState
    ): Boolean {
        val extension = extensions.itemViews().firstOrNull { it.id == extensionId } ?: return false
        extension.bindView(view, item, state)
        return true
    }

    fun recycleCustomItemView(extensionId: String, view: View) {
        extensions.itemViews().firstOrNull { it.id == extensionId }?.onViewRecycled(view)
    }

    private fun dispatchSymbol(symbol: SymbolEntry, longPress: Boolean) {
        if (closed.get()) return
        val groupIndex = runtimeGroups.indexOfFirst { group -> group.symbols.any { it.id == symbol.id } }
        if (groupIndex < 0) return
        val itemIndex = runtimeGroups[groupIndex].symbols.indexOfFirst { it.id == symbol.id }
        if (itemIndex < 0) return
        val group = legacyGroups.getOrNull(groupIndex) ?: return
        val item = group.items.getOrNull(itemIndex) ?: return
        val invocation = SymbolToolbarInvocation(
            group, item,
            if (longPress) SymbolToolbarInvocation.Gesture.LONG_PRESS else SymbolToolbarInvocation.Gesture.TAP
        )
        runtime.events.publish(SymbolToolbarItemEvent.Invoked(
            controlId, group, item, invocation, runtime.events.timestamp(), runtime.events.nextSequence()
        ))
        listeners.forEach { listener -> runCatching { listener.onItemInvoked(group, item, invocation) } }
        coroutineScope.launch { runtime.invoke(invocation) }
    }

    private fun syncDrawerExtensions(snapshot: SymbolToolbarExtensionSnapshot) {
        if (closed.get()) return
        sync(
            snapshot.groupIndicators.map { it.id }.toSet(), indicatorHandles,
            { extension -> drawer.extensions.registerIndicator(extension.asDrawerIndicator()) },
            snapshot.groupIndicators, { it.id }
        )
        sync(
            snapshot.themes.map { it.id }.toSet(), themeHandles,
            { extension -> drawer.extensions.registerTheme(extension.asDrawerTheme()) },
            snapshot.themes, { it.id }
        )
        sync(
            snapshot.settings.map { it.id }.toSet(), settingsTabHandles,
            { extension -> drawer.extensions.registerSettingsTab(extension.asDrawerSettingsTab()) },
            snapshot.settings, { it.id }
        )
    }

    private fun <T> sync(
        wanted: Set<String>,
        handles: MutableMap<String, AutoCloseable>,
        register: (T) -> AutoCloseable,
        values: List<T>,
        id: (T) -> String
    ) {
        handles.keys.filter { it !in wanted }.toList().forEach { handles.remove(it)?.close() }
        values.forEach { value ->
            val key = id(value)
            if (key !in handles) handles[key] = register(value)
        }
    }

    private fun SymbolToolbarGroupIndicatorExtension.asDrawerIndicator() =
        object : SymbolGroupIndicatorExtension {
            override val id = this@asDrawerIndicator.id
            override val title = this@asDrawerIndicator.title
            override val description = this@asDrawerIndicator.description
            override val priority = this@asDrawerIndicator.priority
            override fun create(context: Context, initialState: SymbolGroupIndicatorState): SymbolGroupIndicatorComponent {
                val component = this@asDrawerIndicator.createView(
                    context,
                    com.zerostudio.idea.widget.symboltoolbar.api.extension.SymbolToolbarGroupIndicatorState(
                        legacyGroups, initialState.selectedIndex, settings
                    )
                )
                return object : SymbolGroupIndicatorComponent {
                    override val view: View get() = component.view
                    override fun submitGroups(groups: List<RuntimeSymbolGroup>) = component.submitGroups(legacyGroups)
                    override fun setSelectedIndex(index: Int) = component.setSelectedGroup(index)
                    override fun applySettings(settings: SymbolDrawerSettings) = component.applySettings(settings)
                    override fun applyTheme(theme: SymbolDrawerTheme?) {
                        theme?.let { component.applyTheme(com.zerostudio.idea.widget.symboltoolbar.api.extension.SymbolToolbarTheme(
                            surface = it.surface, onSurface = it.onSurface,
                            selectedSurface = it.primary, selectedOnSurface = it.onPrimary, outline = it.outline
                        )) }
                    }
                    override fun onDispose() = component.onDispose()
                }
            }
        }

    private fun SymbolToolbarThemeExtension.asDrawerTheme() =
        object : SymbolDrawerThemeExtension {
            override val id = this@asDrawerTheme.id
            override val title = this@asDrawerTheme.title
            override val priority = this@asDrawerTheme.priority
            override fun resolve(context: Context, settings: SymbolDrawerSettings): SymbolDrawerTheme {
                val theme = this@asDrawerTheme.resolve(context)
                return SymbolDrawerTheme(
                    primary = theme.selectedSurface,
                    onPrimary = theme.selectedOnSurface,
                    surface = theme.surface,
                    onSurface = theme.onSurface,
                    secondaryText = theme.onSurface,
                    outline = theme.outline,
                    drawerBackground = theme.surface,
                    selectedCellBackground = theme.selectedSurface
                )
            }
        }

    private fun SymbolToolbarSettingsExtension.asDrawerSettingsTab() =
        object : SymbolDrawerSettingsTabExtension {
            override val id = this@asDrawerSettingsTab.id
            override val title = this@asDrawerSettingsTab.title
            override val priority = this@asDrawerSettingsTab.priority
            override fun createView(context: Context, scope: SymbolDrawerSettingsScope): View =
                this@asDrawerSettingsTab.createView(context, scope.asToolbarSettingsScope())
        }

    private fun SymbolToolbarSettingsExtension.asDrawerSetting() =
        object : SymbolDrawerSettingExtension {
            override val id = this@asDrawerSetting.id
            override val tabId = this@asDrawerSetting.id
            override val title = this@asDrawerSetting.title
            override val priority = this@asDrawerSetting.priority
            override fun createView(context: Context, scope: SymbolDrawerSettingsScope): View =
                this@asDrawerSetting.createView(context, scope.asToolbarSettingsScope())
        }

    private fun SymbolDrawerSettingsScope.asToolbarSettingsScope() =
        object : SymbolToolbarSettingsScope {
            override val settings: SymbolDrawerSettings get() = this@asToolbarSettingsScope.current
            override fun updateSettings(transform: (SymbolDrawerSettings) -> SymbolDrawerSettings) =
                this@asToolbarSettingsScope.update(transform)
        }

    private fun updateState(selectedIndex: Int) {
        val next = SymbolToolbarState(
            selectedGroupIndex = selectedIndex.takeIf { it in legacyGroups.indices } ?: -1,
            groupCount = legacyGroups.size,
            itemCount = legacyGroups.sumOf { it.items.size },
            isEnabled = drawer.isEnabled,
            revision = currentState.revision + 1
        )
        val previous = currentState
        currentState = next
        if (previous != next) listeners.forEach { listener -> runCatching { listener.onStateChanged(next) } }
    }

    private fun checkActive() = check(!closed.get()) { "SymbolDrawerToolbarControl is closed" }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        extensionObserver.close()
        indicatorHandles.values.forEach { runCatching { it.close() } }
        themeHandles.values.forEach { runCatching { it.close() } }
        settingsTabHandles.values.forEach { runCatching { it.close() } }
        indicatorHandles.clear(); themeHandles.clear(); settingsTabHandles.clear()
        drawer.onGroupChangedListener = null
        drawer.onSymbolActionListener = null
        extensions.clear()
        runtime.close()
        listeners.clear()
    }
}
