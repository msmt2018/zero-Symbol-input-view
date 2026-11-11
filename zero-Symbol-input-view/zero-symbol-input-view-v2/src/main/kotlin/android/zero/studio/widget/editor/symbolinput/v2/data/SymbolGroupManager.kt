package android.zero.studio.widget.editor.symbolinput.v2.data

import android.zero.studio.widget.editor.symbolinput.v2.model.SymbolEntry
import android.zero.studio.widget.editor.symbolinput.v2.model.SymbolGroup
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
            group.copy(symbols = group.symbols.map { if (it.id == symbol.id) symbol else it })
        }
    }

    fun removeSymbol(groupId: String, symbolId: Long) {
        updateGroup(groupId) { group ->
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

    fun copySymbol(sourceGroupId: String, symbolId: Long, targetGroupId: String): SymbolEntry {
        val source = groups.firstOrNull { it.id == sourceGroupId }
            ?: error("Source group not found: $sourceGroupId")
        val symbol = source.symbols.firstOrNull { it.id == symbolId }
            ?: error("Symbol not found: $symbolId")
        val copy = symbol.copy(id = newSymbolId())
        addSymbol(targetGroupId, copy)
        return copy
    }

    fun moveSymbolToGroup(sourceGroupId: String, symbolId: Long, targetGroupId: String) {
        require(sourceGroupId != targetGroupId) { "Source and target groups must differ" }
        val source = groups.firstOrNull { it.id == sourceGroupId }
            ?: error("Source group not found: $sourceGroupId")
        val symbol = source.symbols.firstOrNull { it.id == symbolId }
            ?: error("Symbol not found: $symbolId")
        val next = groups.map { group ->
            when (group.id) {
                sourceGroupId -> group.copy(symbols = group.symbols.filterNot { it.id == symbolId })
                targetGroupId -> group.copy(symbols = group.symbols + symbol)
                else -> group
            }
        }
        require(next.any { it.id == targetGroupId }) { "Target group not found: $targetGroupId" }
        commit(next)
    }

    private fun updateGroup(groupId: String, transform: (SymbolGroup) -> SymbolGroup) {
        require(groups.any { it.id == groupId }) { "Group not found: $groupId" }
        commit(groups.map { if (it.id == groupId) transform(it) else it })
    }

    private fun commit(next: List<SymbolGroup>) {
        require(next.map { it.id }.distinct().size == next.size) { "Group IDs must be unique" }
        next.forEach { group ->
            require(group.symbols.map { it.id }.distinct().size == group.symbols.size) {
                "Symbol IDs must be unique within group ${group.id}"
            }
        }
        groups = next.toList()
        repository.saveGroups(groups)
    }
}
