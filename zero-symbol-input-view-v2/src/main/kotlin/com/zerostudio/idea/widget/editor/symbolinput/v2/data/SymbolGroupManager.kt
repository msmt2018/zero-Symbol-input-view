package com.zerostudio.idea.widget.editor.symbolinput.v2.data

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import java.util.UUID

/**
 * Repository-backed command model for a future manager UI.
 * All operations produce new immutable snapshots and persist once per command.
 */
class SymbolGroupManager(
    private val repository: SymbolGroupRepository,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    private val newSymbolId: () -> Long = { UUID.randomUUID().mostSignificantBits }
) {
    var groups: List<SymbolGroup> = repository.loadGroups()
        private set

    fun reload() {
        groups = repository.loadGroups()
    }

    fun addGroup(title: String, index: Int = groups.size): SymbolGroup {
        require(title.isNotBlank()) { "Group title cannot be blank" }
        val group = SymbolGroup(newId(), title.trim(), emptyList())
        val next = groups.toMutableList().apply { add(index.coerceIn(0, size), group) }
        commit(next)
        return group
    }

    fun replaceAll(value: List<SymbolGroup>) {
        commit(value.toList())
    }

    fun duplicateGroup(groupId: String, index: Int = groups.size): SymbolGroup {
        val source = groups.firstOrNull { it.id == groupId } ?: error("Group not found: $groupId")
        val duplicate = source.copy(
            id = newId(),
            title = "${source.title} copy",
            symbols = source.symbols.map { it.copy(id = newSymbolId()) }
        )
        val next = groups.toMutableList().apply { add(index.coerceIn(0, size), duplicate) }
        commit(next)
        return duplicate
    }

    fun renameGroup(groupId: String, title: String) {
        require(title.isNotBlank()) { "Group title cannot be blank" }
        require(groups.any { it.id == groupId }) { "Group not found: $groupId" }
        commit(groups.map { if (it.id == groupId) it.copy(title = title.trim()) else it })
    }

    fun removeGroup(groupId: String) {
        commit(groups.filterNot { it.id == groupId })
    }

    fun moveGroup(fromIndex: Int, toIndex: Int) {
        require(fromIndex in groups.indices && toIndex in groups.indices)
        val next = groups.toMutableList()
        val moved = next.removeAt(fromIndex)
        next.add(toIndex, moved)
        commit(next)
    }

    fun addSymbol(groupId: String, symbol: SymbolEntry, index: Int? = null) {
        val safeSymbol = if (symbol.id == 0L) {
            symbol.copy(id = newSymbolId().takeIf { it != 0L } ?: 1L)
        } else symbol
        updateGroup(groupId) { group ->
            val items = group.symbols.toMutableList()
            items.add((index ?: items.size).coerceIn(0, items.size), safeSymbol)
            group.copy(symbols = items)
        }
    }

    fun updateSymbol(groupId: String, symbol: SymbolEntry) {
        updateGroup(groupId) { group ->
            require(group.symbols.any { it.id == symbol.id }) {
                "Symbol not found: ${symbol.id}"
            }
            group.copy(symbols = group.symbols.map { if (it.id == symbol.id) symbol else it })
        }
    }

    fun removeSymbol(groupId: String, symbolId: Long) {
        updateGroup(groupId) { group ->
            require(group.symbols.any { it.id == symbolId }) {
                "Symbol not found: $symbolId"
            }
            group.copy(symbols = group.symbols.filterNot { it.id == symbolId })
        }
    }

    fun moveSymbol(groupId: String, fromIndex: Int, toIndex: Int) {
        updateGroup(groupId) { group ->
            require(fromIndex in group.symbols.indices && toIndex in group.symbols.indices)
            val items = group.symbols.toMutableList()
            val moved = items.removeAt(fromIndex)
            items.add(toIndex, moved)
            group.copy(symbols = items)
        }
    }

    fun copySymbol(sourceGroupId: String, symbolId: Long, targetGroupId: String): SymbolEntry =
        copySymbols(sourceGroupId, setOf(symbolId), targetGroupId).single()

    /** Copy a set of symbols in one validated, single-write transaction. */
    fun copySymbols(sourceGroupId: String, symbolIds: Set<Long>, targetGroupId: String): List<SymbolEntry> {
        require(symbolIds.isNotEmpty()) { "No symbols selected" }
        val source = groups.firstOrNull { it.id == sourceGroupId }
            ?: error("Source group not found: $sourceGroupId")
        val target = groups.firstOrNull { it.id == targetGroupId }
            ?: error("Target group not found: $targetGroupId")
        val selected = source.symbols.filter { it.id in symbolIds }
        require(selected.size == symbolIds.size) { "One or more selected symbols no longer exist" }
        val usedIds = target.symbols.mapTo(mutableSetOf()) { it.id }
        val copies = selected.map { symbol ->
            symbol.copy(id = nextUniqueSymbolId(usedIds)).also { usedIds += it.id }
        }
        commit(groups.map { if (it.id == targetGroupId) it.copy(symbols = it.symbols + copies) else it })
        return copies
    }

    /** Remove multiple symbols atomically; no write occurs for stale selection IDs. */
    fun removeSymbols(groupId: String, symbolIds: Set<Long>): Int {
        require(symbolIds.isNotEmpty()) { "No symbols selected" }
        val group = groups.firstOrNull { it.id == groupId } ?: error("Group not found: $groupId")
        require(group.symbols.count { it.id in symbolIds } == symbolIds.size) {
            "One or more selected symbols no longer exist"
        }
        commit(groups.map { current ->
            if (current.id == groupId) current.copy(symbols = current.symbols.filterNot { it.id in symbolIds })
            else current
        })
        return symbolIds.size
    }

    /** Move multiple symbols atomically while retaining IDs when the target has no collision. */
    fun moveSymbolsToGroup(sourceGroupId: String, symbolIds: Set<Long>, targetGroupId: String) {
        require(sourceGroupId != targetGroupId) { "Source and target groups must differ" }
        require(symbolIds.isNotEmpty()) { "No symbols selected" }
        val source = groups.firstOrNull { it.id == sourceGroupId }
            ?: error("Source group not found: $sourceGroupId")
        val target = groups.firstOrNull { it.id == targetGroupId }
            ?: error("Target group not found: $targetGroupId")
        val moved = source.symbols.filter { it.id in symbolIds }
        require(moved.size == symbolIds.size) { "One or more selected symbols no longer exist" }
        val usedIds = target.symbols.mapTo(mutableSetOf()) { it.id }
        val normalized = moved.map { symbol ->
            if (symbol.id in usedIds) symbol.copy(id = nextUniqueSymbolId(usedIds))
            else symbol.also { usedIds += it.id }
        }
        commit(groups.map { group ->
            when (group.id) {
                sourceGroupId -> group.copy(symbols = group.symbols.filterNot { it.id in symbolIds })
                targetGroupId -> group.copy(symbols = group.symbols + normalized)
                else -> group
            }
        })
    }

    fun moveSymbolToGroup(sourceGroupId: String, symbolId: Long, targetGroupId: String) =
        moveSymbolsToGroup(sourceGroupId, setOf(symbolId), targetGroupId)

    /** Reorder by stable IDs; the supplied order must contain every symbol exactly once. */
    fun reorderSymbols(groupId: String, orderedIds: List<Long>) {
        val group = groups.firstOrNull { it.id == groupId } ?: error("Group not found: $groupId")
        require(orderedIds.size == group.symbols.size &&
            orderedIds.toSet().size == orderedIds.size &&
            orderedIds.toSet() == group.symbols.mapTo(mutableSetOf()) { it.id }
        ) { "Order must contain every symbol ID exactly once" }
        val byId = group.symbols.associateBy { it.id }
        commit(groups.map { current ->
            if (current.id == groupId) current.copy(symbols = orderedIds.map { byId.getValue(it) })
            else current
        })
    }

    private fun nextUniqueSymbolId(usedIds: Set<Long>): Long {
        repeat(32) {
            val candidate = newSymbolId()
            if (candidate != 0L && candidate !in usedIds) return candidate
        }
        error("Unable to generate a unique symbol ID")
    }

    private fun updateGroup(groupId: String, transform: (SymbolGroup) -> SymbolGroup) {
        require(groups.any { it.id == groupId }) { "Group not found: $groupId" }
        commit(groups.map { if (it.id == groupId) transform(it) else it })
    }

    private fun commit(next: List<SymbolGroup>) {
        require(next.all { it.id.isNotBlank() && it.title.isNotBlank() }) {
            "Group IDs and titles must not be blank"
        }
        require(next.map { it.id }.distinct().size == next.size) { "Group IDs must be unique" }
        next.forEach { group ->
            require(group.symbols.all { it.id != 0L && it.label.isNotBlank() }) {
                "Symbol IDs and labels must not be blank in group ${group.id}"
            }
            require(group.symbols.map { it.id }.distinct().size == group.symbols.size) {
                "Symbol IDs must be unique within group ${group.id}"
            }
        }
        // Persist first: a failed write must not publish an in-memory snapshot
        // that the repository cannot reproduce after reload.
        val snapshot = next.toList()
        repository.saveGroups(snapshot)
        groups = snapshot
    }
}
