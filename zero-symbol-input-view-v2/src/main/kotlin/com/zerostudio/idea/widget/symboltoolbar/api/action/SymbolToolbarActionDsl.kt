/*
 * Concise public action registration DSL.
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.symboltoolbar.api.action

import com.zerostudio.idea.widget.symboltoolbar.api.extension.SymbolToolbarExtensionRegistry
import com.zerostudio.idea.widget.symboltoolbar.api.model.SymbolToolbarInvocation

/** Convenient aliases for common action implementations. */
typealias SymbolToolbarActionBlock = suspend SymbolToolbarActionContext.() -> SymbolToolbarActionResult

/**
 * Rich action context. The original invocation and the explicitly granted host
 * are both available; no editor implementation is leaked through this API.
 */
class SymbolToolbarActionContext internal constructor(
    val invocation: SymbolToolbarInvocation,
    val host: SymbolToolbarActionHost
) {
    val action: SymbolToolbarAction get() = invocation.action
    val groupId: String get() = invocation.groupId
    val itemId: String get() = invocation.itemId
    val isLongPress: Boolean
        get() = invocation.gesture == SymbolToolbarInvocation.Gesture.LONG_PRESS

    suspend fun insert(text: String): SymbolToolbarActionResult =
        result(host.insertText(text))

    suspend fun deleteBackward(): SymbolToolbarActionResult =
        result(host.deleteBackward())

    suspend fun moveCursor(offset: Int): SymbolToolbarActionResult =
        result(host.moveCursor(offset))

    suspend fun command(id: String, payload: String? = null): SymbolToolbarActionResult =
        result(host.executeCommand(id, payload))

    private fun result(success: Boolean): SymbolToolbarActionResult =
        if (success) SymbolToolbarActionResult.Success
        else SymbolToolbarActionResult.Rejected("Host declined the operation")
}

/** Register an action in one concise block. */
fun SymbolToolbarExtensionRegistry.action(
    type: String,
    handler: SymbolToolbarActionBlock
): AutoCloseable {
    require(type.isNotBlank()) { "Action type must not be blank" }
    return registerAction(type) { invocation, host ->
        handler(SymbolToolbarActionContext(
            invocation = SymbolToolbarInvocation(
                groupId = invocation.groupId,
                itemId = invocation.itemId,
                action = invocation.action,
                gesture = invocation.gesture,
                eventTimeMillis = invocation.eventTimeMillis
            ),
            host = host
        ))
    }
}

/** Build action descriptions with minimal ceremony. */
fun symbolAction(
    type: String,
    payload: String? = null,
    arguments: Map<String, String> = emptyMap()
): SymbolToolbarAction = SymbolToolbarAction(type, payload, arguments)
