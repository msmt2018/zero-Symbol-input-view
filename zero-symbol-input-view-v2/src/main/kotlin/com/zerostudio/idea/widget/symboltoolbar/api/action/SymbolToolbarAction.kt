/*
 * Editor-neutral action execution contract for legacy numeric symbol actions.
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.symboltoolbar.api.action

import com.zerostudio.idea.widget.symboltoolbar.api.model.SymbolToolbarInvocation

/** Handler is registered by the numeric action ID already stored in SymbolItem. */
fun interface SymbolToolbarActionHandler {
    suspend fun execute(
        invocation: SymbolToolbarInvocation,
        host: SymbolToolbarActionHost
    ): SymbolToolbarActionResult
}

/** Minimal capabilities, deliberately independent of a specific editor core. */
interface SymbolToolbarActionHost {
    suspend fun insertText(text: String): Boolean = false
    suspend fun deleteBackward(): Boolean = false
    suspend fun moveCursor(relativeOffset: Int): Boolean = false
    suspend fun executeCommand(commandId: String, payload: String?): Boolean = false
}

sealed interface SymbolToolbarActionResult {
    data object Success : SymbolToolbarActionResult
    data class Unsupported(val actionId: Int) : SymbolToolbarActionResult
    data class Rejected(val reason: String) : SymbolToolbarActionResult
    data class Failed(val cause: Throwable) : SymbolToolbarActionResult
}
