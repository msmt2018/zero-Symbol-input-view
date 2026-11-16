/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 * Author: android_zero · https://github.com/msmt2018
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings

/** Compose-native renderer extension; unlike the View API it never creates or retains an Android View. */
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

/** Lifecycle-independent immutable Compose extension snapshot. */
@Immutable
data class ComposeSymbolDrawerExtensions(
    val itemRenderers: List<SymbolDrawerItemRenderer> = emptyList(),
    val groupIndicators: List<SymbolDrawerGroupIndicator> = emptyList()
) {
    init {
        require(itemRenderers.map { it.id }.distinct().size == itemRenderers.size)
        require(groupIndicators.map { it.id }.distinct().size == groupIndicators.size)
    }
}


/** Thread-safe session registry. Registration handles are idempotent and snapshots are immutable. */
class ComposeSymbolDrawerExtensionRegistry {
    private val lock = Any()
    private val itemRenderers = LinkedHashMap<String, SymbolDrawerItemRenderer>()
    private val indicators = LinkedHashMap<String, SymbolDrawerGroupIndicator>()
    private var revision = 0L
    private val observers = LinkedHashSet<(Long) -> Unit>()

    fun registerItemRenderer(renderer: SymbolDrawerItemRenderer): AutoCloseable =
        register(itemRenderers, renderer.id, renderer)

    fun registerGroupIndicator(indicator: SymbolDrawerGroupIndicator): AutoCloseable =
        register(indicators, indicator.id, indicator)

    private fun <T> register(target: LinkedHashMap<String, T>, id: String, value: T): AutoCloseable {
        require(id.matches(Regex("[A-Za-z0-9][A-Za-z0-9_.:/-]{0,255}")))
        synchronized(lock) { target[id] = value; revision++ }
        notifyObservers()
        var closed = false
        return AutoCloseable {
            val removed = synchronized(lock) {
                if (closed) false else {
                    closed = true
                    if (target[id] === value) {
                        target.remove(id)
                        revision++
                        true
                    } else false
                }
            }
            if (removed) notifyObservers()
        }
    }

    fun snapshot(): ComposeSymbolDrawerExtensions = synchronized(lock) {
        ComposeSymbolDrawerExtensions(
            itemRenderers = itemRenderers.values.sortedWith(compareByDescending<SymbolDrawerItemRenderer> { it.priority }.thenBy { it.id }),
            groupIndicators = indicators.values.sortedWith(compareByDescending<SymbolDrawerGroupIndicator> { it.priority }.thenBy { it.id })
        )
    }

    fun observe(listener: (Long) -> Unit): AutoCloseable {
        synchronized(lock) { observers += listener }
        return AutoCloseable { synchronized(lock) { observers -= listener } }
    }

    private fun notifyObservers() {
        val next: Pair<Long, List<(Long) -> Unit>> = synchronized(lock) { revision to observers.toList() }
        next.second.forEach { runCatching { it(next.first) } }
    }

    fun clear() {
        synchronized(lock) {
            itemRenderers.clear()
            indicators.clear()
            revision++
        }
        notifyObservers()
    }
}
