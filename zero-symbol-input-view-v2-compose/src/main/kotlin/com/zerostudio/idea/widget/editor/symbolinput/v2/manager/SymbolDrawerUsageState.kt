/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 * Author: android_zero · https://github.com/msmt2018
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.manager

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import java.util.concurrent.CopyOnWriteArrayList

/** Manager filters are domain state and do not depend on Compose or Android. */
enum class SymbolManagerFilter { ALL, FAVORITES, RECENT }

data class SymbolUsageSnapshot(
    val favorites: Set<Long> = emptySet(),
    /** Most-recent-first, unique IDs. */
    val recent: List<Long> = emptyList()
)

interface SymbolUsageRepository {
    fun load(): SymbolUsageSnapshot
    fun save(snapshot: SymbolUsageSnapshot)
}

/**
 * Platform-neutral manager usage state. It owns favorite/recent semantics;
 * Android persistence is supplied by an adapter and Compose only observes
 * immutable snapshots.
 */
class SymbolDrawerUsageState(
    private val repository: SymbolUsageRepository? = null,
    private val recentLimit: Int = 30
) {
    init { require(recentLimit > 0) }

    private var current = normalize(repository?.load() ?: SymbolUsageSnapshot())
    private val listeners = CopyOnWriteArrayList<(SymbolUsageSnapshot) -> Unit>()

    /** Observes immutable snapshots; the returned handle detaches the listener. */
    fun observe(listener: (SymbolUsageSnapshot) -> Unit): AutoCloseable {
        listeners += listener
        runCatching { listener(snapshot()) }
        return AutoCloseable { listeners.remove(listener) }
    }

    fun snapshot(): SymbolUsageSnapshot = current.copy(
        favorites = current.favorites.toSet(),
        recent = current.recent.toList()
    )

    fun toggleFavorite(symbolId: Long): SymbolUsageSnapshot {
        require(symbolId != 0L)
        val favorites = current.favorites.toMutableSet()
        if (!favorites.add(symbolId)) favorites.remove(symbolId)
        return commit(current.copy(favorites = favorites))
    }

    fun recordRecentlyUsed(symbolId: Long): SymbolUsageSnapshot {
        require(symbolId != 0L)
        val recent = buildList {
            add(symbolId)
            current.recent.forEach { if (it != symbolId && size < recentLimit) add(it) }
        }
        return commit(current.copy(recent = recent))
    }

    fun prune(validSymbolIds: Set<Long>): SymbolUsageSnapshot {
        val next = SymbolUsageSnapshot(
            favorites = current.favorites.filterTo(linkedSetOf()) { it in validSymbolIds },
            recent = current.recent.filter { it in validSymbolIds }.take(recentLimit)
        )
        return commit(next)
    }

    fun restore(snapshot: SymbolUsageSnapshot): SymbolUsageSnapshot = commit(normalize(snapshot))

    fun filter(
        symbols: List<SymbolEntry>,
        query: String,
        filter: SymbolManagerFilter,
        includeActionPayload: Boolean = true
    ): List<SymbolEntry> {
        val normalizedQuery = query.trim()
        val matches = symbols.filter { symbol ->
            val queryMatches = normalizedQuery.isEmpty() ||
                symbol.label.contains(normalizedQuery, ignoreCase = true) ||
                (includeActionPayload && (
                    symbol.shortAction.toString().contains(normalizedQuery, ignoreCase = true) ||
                        symbol.longAction?.toString()?.contains(normalizedQuery, ignoreCase = true) == true
                    ))
            val filterMatches = when (filter) {
                SymbolManagerFilter.ALL -> true
                SymbolManagerFilter.FAVORITES -> symbol.id in current.favorites
                SymbolManagerFilter.RECENT -> symbol.id in current.recent
            }
            queryMatches && filterMatches
        }
        if (filter != SymbolManagerFilter.RECENT) return matches
        val order = current.recent.withIndex().associate { it.value to it.index }
        return matches.sortedBy { order[it.id] ?: Int.MAX_VALUE }
    }

    private fun commit(value: SymbolUsageSnapshot): SymbolUsageSnapshot {
        val next = normalize(value)
        if (next != current) {
            repository?.save(next)
            current = next
            val published = snapshot()
            listeners.forEach { listener -> runCatching { listener(published) } }
        }
        return snapshot()
    }

    private fun normalize(value: SymbolUsageSnapshot): SymbolUsageSnapshot {
        val recent = LinkedHashSet<Long>()
        value.recent.forEach { id ->
            if (id != 0L && recent.size < recentLimit) recent += id
        }
        return SymbolUsageSnapshot(
            favorites = value.favorites.filterTo(linkedSetOf()) { it != 0L },
            recent = recent.toList()
        )
    }
}
