/*
 * Public API for symbol toolbar controls. Symbol data and settings reuse V2's
 * canonical legacy data model and settings model.
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.symboltoolbar.api

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.legacy.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.legacy.SymbolItem
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
import com.zerostudio.idea.widget.symboltoolbar.api.model.SymbolToolbarInvocation

/** Host-facing contract implemented by a symbol toolbar control. */
interface SymbolToolbarControl {
    val state: SymbolToolbarState
    fun submitGroups(groups: List<SymbolGroup>)
    fun selectGroup(index: Int, animated: Boolean = true): Boolean
    fun updateSettings(settings: SymbolDrawerSettings)
    fun addListener(listener: SymbolToolbarListener): AutoCloseable
}

/** Runtime metadata only; symbol content remains in SymbolGroup/SymbolItem. */
data class SymbolToolbarState(
    val selectedGroupIndex: Int = -1,
    val groupCount: Int = 0,
    val itemCount: Int = 0,
    val isEnabled: Boolean = true,
    val revision: Long = 0L
)

interface SymbolToolbarListener {
    fun onGroupSelected(group: SymbolGroup) {}
    fun onItemInvoked(group: SymbolGroup, item: SymbolItem, invocation: SymbolToolbarInvocation) {}
    fun onStateChanged(state: SymbolToolbarState) {}
}
