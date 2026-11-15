/*
 * Rebinds persisted action references to developer-provided Kotlin handlers.
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.symboltoolbar.api.persistence

import com.google.gson.JsonElement
import com.zerostudio.idea.widget.symboltoolbar.api.action.SymbolToolbarAction
import com.zerostudio.idea.widget.symboltoolbar.api.action.SymbolToolbarActionContext
import com.zerostudio.idea.widget.symboltoolbar.api.action.SymbolToolbarActionList
import com.zerostudio.idea.widget.symboltoolbar.api.action.SymbolToolbarActionListBuilder
import com.zerostudio.idea.widget.symboltoolbar.api.action.SymbolToolbarActionResult
import com.zerostudio.idea.widget.symboltoolbar.api.model.SymbolToolbarInvocation
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/** Plugin supplies executable Kotlin code for a stable persisted action ID. */
typealias SymbolToolbarPersistedActionHandler =
    suspend SymbolToolbarActionContext.(arguments: JsonElement) -> SymbolToolbarActionResult

class SymbolToolbarActionRebinder {
    private val handlers = ConcurrentHashMap<String, SymbolToolbarPersistedActionHandler>()

    fun register(actionId: String, handler: SymbolToolbarPersistedActionHandler): AutoCloseable {
        require(actionId.isNotBlank())
        check(handlers.putIfAbsent(actionId, handler) == null) {
            "Persisted action handler already registered: $actionId"
        }
        val closed = AtomicBoolean(false)
        return AutoCloseable {
            if (closed.compareAndSet(false, true)) handlers.remove(actionId, handler)
        }
    }

    fun registeredActionIds(): Set<String> = handlers.keys.toSortedSet()

    fun bind(
        persisted: PersistedSymbolItemActions,
        gesture: SymbolToolbarInvocation.Gesture
    ): SymbolToolbarActionList {
        val actions = when (gesture) {
            SymbolToolbarInvocation.Gesture.LONG_PRESS -> persisted.longPress
            SymbolToolbarInvocation.Gesture.TAP,
            SymbolToolbarInvocation.Gesture.ACCESSIBILITY -> persisted.click
        }.filter { it.enabled }

        return SymbolToolbarActionListBuilder().apply {
            actions.forEach { descriptor ->
                val handler = handlers[descriptor.actionId]
                step(descriptor.id) {
                    if (handler == null) {
                        SymbolToolbarActionResult.Unsupported(descriptor.actionId)
                    } else {
                        val reboundInvocation = invocation.copy(
                            action = SymbolToolbarAction(
                                type = descriptor.actionId,
                                payload = descriptor.arguments.toString()
                            ),
                            gesture = gesture
                        )
                        handler(
                            SymbolToolbarActionContext(reboundInvocation, host),
                            descriptor.arguments.deepCopy()
                        )
                    }
                }
            }
        }.build()
    }
}
