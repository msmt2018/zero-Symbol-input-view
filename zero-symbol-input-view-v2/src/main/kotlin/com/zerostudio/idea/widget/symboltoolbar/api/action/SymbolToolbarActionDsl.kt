/*
 * Concise public DSL for legacy numeric symbol actions.
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.symboltoolbar.api.action

import com.zerostudio.idea.widget.symboltoolbar.api.extension.SymbolToolbarExtensionRegistry
import com.zerostudio.idea.widget.symboltoolbar.api.model.SymbolToolbarInvocation

typealias SymbolToolbarActionBlock = suspend SymbolToolbarActionContext.() -> SymbolToolbarActionResult

class SymbolToolbarActionContext(
    val invocation: SymbolToolbarInvocation,
    val host: SymbolToolbarActionHost
) {
    val group get() = invocation.group
    val item get() = invocation.item
    val actionId: Int get() = invocation.actionId
    val payload: String? get() = invocation.payload
    val isLongPress: Boolean get() = invocation.isLongPress

    suspend fun insert(text: String) = result(host.insertText(text))
    suspend fun deleteBackward() = result(host.deleteBackward())
    suspend fun moveCursor(offset: Int) = result(host.moveCursor(offset))
    suspend fun command(id: String, payload: String? = null) = result(host.executeCommand(id, payload))

    private fun result(success: Boolean): SymbolToolbarActionResult =
        if (success) SymbolToolbarActionResult.Success
        else SymbolToolbarActionResult.Rejected("Host declined the operation")
}

/** Register directly against SymbolItem.shortAction/longAction numeric IDs. */
fun SymbolToolbarExtensionRegistry.action(
    actionId: Int,
    handler: SymbolToolbarActionBlock
): AutoCloseable {
    require(actionId >= 0)
    return registerAction(actionId) { invocation, host ->
        handler(SymbolToolbarActionContext(invocation, host))
    }
}
