/*
 * Extension contracts for symbol toolbar controls.
 * Symbol groups/items/settings use the canonical V1/V2 models directly.
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.symboltoolbar.api.extension

import android.content.Context
import android.view.View
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.legacy.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.legacy.SymbolItem
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
import com.zerostudio.idea.widget.symboltoolbar.api.action.SymbolToolbarActionHost
import com.zerostudio.idea.widget.symboltoolbar.api.action.SymbolToolbarActionResult
import com.zerostudio.idea.widget.symboltoolbar.api.model.SymbolToolbarInvocation

interface SymbolToolbarExtension {
    val id: String
    val title: String
    val description: String get() = ""
    val priority: Int get() = 0
}

interface SymbolToolbarGroupProvider : SymbolToolbarExtension {
    suspend fun loadGroups(): List<SymbolGroup>
}

interface SymbolToolbarItemViewExtension : SymbolToolbarExtension {
    fun createView(context: Context, item: SymbolItem, state: SymbolToolbarItemViewState): View
    fun bindView(view: View, item: SymbolItem, state: SymbolToolbarItemViewState)
    fun onViewRecycled(view: View) = Unit
}

data class SymbolToolbarItemViewState(
    val selected: Boolean = false,
    val pressed: Boolean = false,
    val enabled: Boolean = true,
    val position: Int = 0,
    val groupIndex: Int = -1
)

interface SymbolToolbarGroupIndicatorExtension : SymbolToolbarExtension {
    fun createView(context: Context, state: SymbolToolbarGroupIndicatorState): SymbolToolbarGroupIndicator
}

interface SymbolToolbarGroupIndicator {
    val view: View
    fun submitGroups(groups: List<SymbolGroup>)
    fun setSelectedGroup(index: Int)
    fun setOnGroupSelectedListener(listener: (Int) -> Unit)
    fun applySettings(settings: SymbolDrawerSettings) = Unit
    fun onDispose() = Unit
}

data class SymbolToolbarGroupIndicatorState(
    val groups: List<SymbolGroup>,
    val selectedGroupIndex: Int,
    val settings: SymbolDrawerSettings
)

interface SymbolToolbarSettingsExtension : SymbolToolbarExtension {
    fun createView(context: Context, scope: SymbolToolbarSettingsScope): View
}

interface SymbolToolbarSettingsScope {
    val settings: SymbolDrawerSettings
    fun updateSettings(transform: (SymbolDrawerSettings) -> SymbolDrawerSettings)
}

interface SymbolToolbarThemeExtension : SymbolToolbarExtension {
    fun resolve(context: Context): SymbolToolbarTheme
}

data class SymbolToolbarTheme(
    val surface: Int,
    val onSurface: Int,
    val selectedSurface: Int,
    val selectedOnSurface: Int,
    val outline: Int,
    val disabledAlpha: Float = 0.38f,
    val itemCornerRadiusDp: Float = 8f
) {
    init {
        require(disabledAlpha in 0f..1f)
        require(itemCornerRadiusDp in 0f..64f)
    }
}

interface SymbolToolbarInvocationInterceptor : SymbolToolbarExtension {
    fun beforeInvoke(invocation: SymbolToolbarInvocation): SymbolToolbarInvocationDecision =
        SymbolToolbarInvocationDecision.Continue
    fun afterInvoke(invocation: SymbolToolbarInvocation, result: SymbolToolbarActionResult) = Unit
}

sealed interface SymbolToolbarInvocationDecision {
    data object Continue : SymbolToolbarInvocationDecision
    data object Consume : SymbolToolbarInvocationDecision
    data class Reject(val reason: String) : SymbolToolbarInvocationDecision
}

interface SymbolToolbarLifecycleExtension : SymbolToolbarExtension {
    fun onAttached(controlId: String) = Unit
    fun onSettingsChanged(settings: SymbolDrawerSettings) = Unit
    fun onDetached(controlId: String) = Unit
}

/** Adds a numeric V1 action handler to the toolbar's existing SymbolItem action slot. */
data class SymbolToolbarActionContribution(
    val actionId: Int,
    val title: String,
    val description: String = "",
    val priority: Int = 0
) {
    init {
        require(actionId >= 0)
        require(title.isNotBlank())
    }
}

fun interface SymbolToolbarHostAction {
    suspend fun execute(
        invocation: SymbolToolbarInvocation,
        host: SymbolToolbarActionHost
    ): SymbolToolbarActionResult
}
