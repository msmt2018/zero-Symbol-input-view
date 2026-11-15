/*
 * Editor-neutral action protocol for symbol toolbar extensions.
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.symboltoolbar.api.action

/**
 * Serializable-by-design action description. It contains no editor or View
 * instance; hosts translate supported kinds to their own editing engine.
 */
data class SymbolToolbarAction(
    val type: String,
    val payload: String? = null,
    val arguments: Map<String, String> = emptyMap()
) {
    init {
        require(type.isNotBlank()) { "action type must not be blank" }
    }
}

fun interface SymbolToolbarActionHandler {
    suspend fun execute(
        invocation: SymbolToolbarActionInvocation,
        host: SymbolToolbarActionHost
    ): SymbolToolbarActionResult
}

data class SymbolToolbarActionInvocation(
    val groupId: String,
    val itemId: String,
    val action: SymbolToolbarAction,
    val isLongPress: Boolean,
    val gesture: com.zerostudio.idea.widget.symboltoolbar.api.model.SymbolToolbarInvocation.Gesture =
        if (isLongPress) com.zerostudio.idea.widget.symboltoolbar.api.model.SymbolToolbarInvocation.Gesture.LONG_PRESS
        else com.zerostudio.idea.widget.symboltoolbar.api.model.SymbolToolbarInvocation.Gesture.TAP,
    val eventTimeMillis: Long = System.currentTimeMillis()
)

/** Minimal capabilities, deliberately independent of a specific editor. */
interface SymbolToolbarActionHost {
    suspend fun insertText(text: String): Boolean = false
    suspend fun deleteBackward(): Boolean = false
    suspend fun moveCursor(relativeOffset: Int): Boolean = false
    suspend fun executeCommand(commandId: String, payload: String?): Boolean = false
}

sealed interface SymbolToolbarActionResult {
    data object Success : SymbolToolbarActionResult
    data class Unsupported(val actionType: String) : SymbolToolbarActionResult
    data class Rejected(val reason: String) : SymbolToolbarActionResult
    data class Failed(val cause: Throwable) : SymbolToolbarActionResult
}
