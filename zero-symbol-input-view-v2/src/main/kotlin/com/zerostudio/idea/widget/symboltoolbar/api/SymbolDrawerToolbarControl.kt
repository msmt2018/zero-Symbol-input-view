/*
 * Runtime adapter connecting the public toolbar API to the actual V2 drawer View.
 * Public callers use the canonical legacy SymbolGroup/SymbolItem data contract;
 * the adapter delegates rendering to the existing V2 drawer model and codec.
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.symboltoolbar.api

import com.google.gson.Gson
import com.zerostudio.idea.widget.editor.symbolinput.v2.SymbolDrawerView
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.LegacySymbolDataCodec
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup as RuntimeSymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.legacy.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettingsRepository
import com.zerostudio.idea.widget.symboltoolbar.api.action.SymbolToolbarActionHost
import com.zerostudio.idea.widget.symboltoolbar.api.event.SymbolToolbarGroupEvent
import com.zerostudio.idea.widget.symboltoolbar.api.event.SymbolToolbarItemEvent
import com.zerostudio.idea.widget.symboltoolbar.api.extension.SymbolToolbarExtensionRegistry
import com.zerostudio.idea.widget.symboltoolbar.api.extension.SymbolToolbarExtensionRuntime
import com.zerostudio.idea.widget.symboltoolbar.api.model.SymbolToolbarInvocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Concrete bridge to SymbolDrawerView. It does not retain or require an editor
 * core object; the host supplies only action capabilities and a coroutine scope.
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

    private var legacyGroups: List<SymbolGroup> = emptyList()
    private var runtimeGroups: List<RuntimeSymbolGroup> = emptyList()
    private var currentState = SymbolToolbarState()
    private var settings = settingsRepository.load().settings

    init {
        drawer.onGroupChangedListener = { index, _ ->
            if (!closed.get()) {
                val selected = legacyGroups.getOrNull(index)
                if (selected != null) {
                    val event = SymbolToolbarGroupEvent.GroupSelected(
                        controlId = controlId,
                        group = selected,
                        previousGroupIndex = currentState.selectedGroupIndex,
                        selectedGroupIndex = index,
                        animated = false,
                        timestampMillis = runtime.events.timestamp(),
                        sequence = runtime.events.nextSequence()
                    )
                    runtime.events.publish(event)
                    listeners.forEach { listener -> runCatching { listener.onGroupSelected(selected) } }
                }
                updateState(index)
            }
        }
        drawer.onSymbolActionListener = SymbolDrawerView.OnSymbolActionListener { symbol, longPress ->
            dispatchSymbol(symbol, longPress)
        }
        updateState(drawer.getCurrentGroupIndex())
    }

    override val state: SymbolToolbarState get() = currentState

    override fun submitGroups(groups: List<SymbolGroup>) {
        checkActive()
        // Decode through the established strict legacy codec; no duplicate item/group schema.
        val decoded = LegacySymbolDataCodec.decode(gson.toJson(groups))
        legacyGroups = groups.toList()
        runtimeGroups = decoded
        runtime.events.publish(SymbolToolbarGroupEvent.GroupsChanged(
            controlId, legacyGroups, runtime.events.timestamp(), runtime.events.nextSequence()
        ))
        drawer.submitGroups(decoded)
        updateState(drawer.getCurrentGroupIndex())
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
        return AutoCloseable {
            if (disposed.compareAndSet(false, true)) listeners.remove(listener)
        }
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
            group = group,
            item = item,
            gesture = if (longPress) SymbolToolbarInvocation.Gesture.LONG_PRESS
                else SymbolToolbarInvocation.Gesture.TAP
        )
        runtime.events.publish(SymbolToolbarItemEvent.Invoked(
            controlId, group, item, invocation, runtime.events.timestamp(), runtime.events.nextSequence()
        ))
        listeners.forEach { listener -> runCatching { listener.onItemInvoked(group, item, invocation) } }
        coroutineScope.launch { runtime.invoke(invocation) }
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
        drawer.onGroupChangedListener = null
        drawer.onSymbolActionListener = null
        extensions.clear()
        runtime.close()
        listeners.clear()
    }
}
