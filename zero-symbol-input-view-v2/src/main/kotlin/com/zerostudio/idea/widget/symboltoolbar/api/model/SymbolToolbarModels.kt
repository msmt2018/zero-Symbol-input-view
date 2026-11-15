/*
 * Independent symbol toolbar data contracts.
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.symboltoolbar.api.model

import com.zerostudio.idea.widget.symboltoolbar.api.action.SymbolToolbarAction

/** Stable, immutable group identity and ordered item snapshot. */
data class SymbolToolbarGroup(
    val id: String,
    val title: String,
    val items: List<SymbolToolbarItem>,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(id.isNotBlank()) { "group id must not be blank" }
        require(title.isNotBlank()) { "group title must not be blank" }
        require(items.map { it.id }.distinct().size == items.size) {
            "item IDs must be unique within a group"
        }
    }
}

/** A toolbar item is presentation data plus declarative actions, not a View. */
data class SymbolToolbarItem(
    val id: String,
    val label: String,
    val accessibilityLabel: String = label,
    val shortAction: SymbolToolbarAction,
    val longAction: SymbolToolbarAction? = null,
    val enabled: Boolean = true,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(id.isNotBlank()) { "item id must not be blank" }
        require(label.isNotEmpty()) { "item label must not be empty" }
        require(accessibilityLabel.isNotBlank()) { "accessibilityLabel must not be blank" }
    }
}

/** The complete semantic invocation; gesture details are explicit. */
data class SymbolToolbarInvocation(
    val groupId: String,
    val itemId: String,
    val action: SymbolToolbarAction,
    val gesture: Gesture,
    val eventTimeMillis: Long
) {
    enum class Gesture { TAP, LONG_PRESS, ACCESSIBILITY }
}
