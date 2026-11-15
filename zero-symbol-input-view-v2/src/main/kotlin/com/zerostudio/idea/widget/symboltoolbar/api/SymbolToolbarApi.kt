/*
 * Independent public API for symbol toolbar controls.
 * This package deliberately has no dependency on the drawer framework.
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.symboltoolbar.api

import com.zerostudio.idea.widget.symboltoolbar.api.model.SymbolToolbarGroup
import com.zerostudio.idea.widget.symboltoolbar.api.model.SymbolToolbarInvocation

/**
 * Host-facing contract implemented by a symbol toolbar control.
 *
 * Implementations own their own rendering and gesture policy. A drawer may host
 * such a control, but is not part of this API and is never required.
 */
interface SymbolToolbarControl {
    val state: SymbolToolbarState
    fun submitGroups(groups: List<SymbolToolbarGroup>)
    fun selectGroup(groupId: String, animated: Boolean = true): Boolean
    fun updateConfig(config: SymbolToolbarConfig)
    fun addListener(listener: SymbolToolbarListener): AutoCloseable
}

/** Immutable runtime state exposed by a toolbar implementation. */
data class SymbolToolbarState(
    val selectedGroupId: String? = null,
    val groupCount: Int = 0,
    val itemCount: Int = 0,
    val isEnabled: Boolean = true,
    val revision: Long = 0L
)

/** Host-neutral toolbar configuration; no drawer geometry is represented here. */
data class SymbolToolbarConfig(
    val columns: Int = 8,
    val showGroupTitles: Boolean = true,
    val showItemLabels: Boolean = true,
    val showFullItemLabels: Boolean = false,
    val itemTextSizeSp: Float = 14f,
    val itemSpacingDp: Float = 4f,
    val longPressEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true
) {
    init {
        require(columns in 1..32) { "columns must be in 1..32" }
        require(itemTextSizeSp in 6f..48f) { "itemTextSizeSp must be in 6..48" }
        require(itemSpacingDp in 0f..64f) { "itemSpacingDp must be in 0..64" }
    }
}

interface SymbolToolbarListener {
    fun onGroupSelected(group: SymbolToolbarGroup) {}
    fun onItemInvoked(event: SymbolToolbarInvocation) {}
    fun onStateChanged(state: SymbolToolbarState) {}
}
