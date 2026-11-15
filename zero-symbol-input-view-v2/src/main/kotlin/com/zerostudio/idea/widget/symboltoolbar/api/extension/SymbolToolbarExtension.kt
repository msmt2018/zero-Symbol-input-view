/*
 * Extension contracts for independent symbol toolbar controls.
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.symboltoolbar.api.extension

import android.content.Context
import android.view.View
import com.zerostudio.idea.widget.symboltoolbar.api.SymbolToolbarConfig
import com.zerostudio.idea.widget.symboltoolbar.api.action.SymbolToolbarAction
import com.zerostudio.idea.widget.symboltoolbar.api.action.SymbolToolbarActionHost
import com.zerostudio.idea.widget.symboltoolbar.api.action.SymbolToolbarActionResult
import com.zerostudio.idea.widget.symboltoolbar.api.model.SymbolToolbarGroup
import com.zerostudio.idea.widget.symboltoolbar.api.model.SymbolToolbarItem
import com.zerostudio.idea.widget.symboltoolbar.api.model.SymbolToolbarInvocation

/** Common metadata used by discovery UIs and deterministic extension ordering. */
interface SymbolToolbarExtension {
    val id: String
    val title: String
    val description: String get() = ""
    val priority: Int get() = 0
}

/** Supplies groups without coupling the control to a repository implementation. */
interface SymbolToolbarGroupProvider : SymbolToolbarExtension {
    suspend fun loadGroups(): List<SymbolToolbarGroup>
}

/** Allows a host to provide a completely custom item View while retaining semantic item data. */
interface SymbolToolbarItemViewExtension : SymbolToolbarExtension {
    fun createView(context: Context, item: SymbolToolbarItem, state: SymbolToolbarItemViewState): View
    fun bindView(view: View, item: SymbolToolbarItem, state: SymbolToolbarItemViewState)
    fun onViewRecycled(view: View) = Unit
}

data class SymbolToolbarItemViewState(
    val selected: Boolean = false,
    val pressed: Boolean = false,
    val enabled: Boolean = true,
    val position: Int = 0,
    val groupId: String? = null
)

/** Replaces only group navigation presentation; the host remains owner of selection. */
interface SymbolToolbarGroupIndicatorExtension : SymbolToolbarExtension {
    fun createView(context: Context, state: SymbolToolbarGroupIndicatorState): SymbolToolbarGroupIndicator
}

interface SymbolToolbarGroupIndicator {
    val view: View
    fun submitGroups(groups: List<SymbolToolbarGroup>)
    fun setSelectedGroup(groupId: String?)
    fun setOnGroupSelectedListener(listener: (String) -> Unit)
    fun onDispose() = Unit
}

data class SymbolToolbarGroupIndicatorState(
    val groups: List<SymbolToolbarGroup>,
    val selectedGroupId: String?,
    val config: SymbolToolbarConfig
)

/** Adds a host-owned settings page/section without requiring a particular settings framework. */
interface SymbolToolbarSettingsExtension : SymbolToolbarExtension {
    fun createView(context: Context, scope: SymbolToolbarSettingsScope): View
}

interface SymbolToolbarSettingsScope {
    val config: SymbolToolbarConfig
    fun updateConfig(transform: (SymbolToolbarConfig) -> SymbolToolbarConfig)
}

/** Provides host-independent visual tokens. Colors are Android ARGB integers. */
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

/** Gives extensions a safe veto/observe point around semantic item invocations. */
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

/** Optional lifecycle notifications for extensions bound to a control instance. */
interface SymbolToolbarLifecycleExtension : SymbolToolbarExtension {
    fun onAttached(controlId: String) = Unit
    fun onConfigChanged(config: SymbolToolbarConfig) = Unit
    fun onDetached(controlId: String) = Unit
}

/** Declarative action contribution. Runtime handlers are registered separately. */
data class SymbolToolbarActionContribution(
    val id: String,
    val title: String,
    val description: String = "",
    val action: SymbolToolbarAction,
    val priority: Int = 0
) {
    init {
        require(id.isNotBlank())
        require(title.isNotBlank())
    }
}

/** Handler adapter may use only the host capabilities explicitly granted by the embedding app. */
fun interface SymbolToolbarHostAction {
    suspend fun execute(
        invocation: SymbolToolbarInvocation,
        host: SymbolToolbarActionHost
    ): SymbolToolbarActionResult
}
