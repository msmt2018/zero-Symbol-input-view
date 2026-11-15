/*
 * Developer-authored independent click and long-press action pipelines.
 * Bindings reference the canonical mutable legacy SymbolItem instance.
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.symboltoolbar.api.action

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.legacy.SymbolItem
import com.zerostudio.idea.widget.symboltoolbar.api.model.SymbolToolbarInvocation
import java.util.IdentityHashMap

typealias SymbolToolbarActionStep = suspend SymbolToolbarActionContext.() -> SymbolToolbarActionResult

data class SymbolToolbarActionStepDefinition(val id: String, val body: SymbolToolbarActionStep) {
    init { require(id.isNotBlank()) }
}

data class SymbolToolbarActionList(val steps: List<SymbolToolbarActionStepDefinition>) {
    init { require(steps.map { it.id }.distinct().size == steps.size) }
    val isEmpty: Boolean get() = steps.isEmpty()
}

/** Click and long-press pipelines belong to the exact legacy item object. */
data class SymbolToolbarItemActionBindings(
    val item: SymbolItem,
    val click: SymbolToolbarActionList = SymbolToolbarActionList(emptyList()),
    val longPress: SymbolToolbarActionList = SymbolToolbarActionList(emptyList())
) {
    fun forGesture(gesture: SymbolToolbarInvocation.Gesture) =
        if (gesture == SymbolToolbarInvocation.Gesture.LONG_PRESS) longPress else click
}

class SymbolToolbarActionListBuilder internal constructor() {
    private val steps = mutableListOf<SymbolToolbarActionStepDefinition>()
    fun step(id: String, body: SymbolToolbarActionStep) {
        require(steps.none { it.id == id }) { "Duplicate action step id: $id" }
        steps += SymbolToolbarActionStepDefinition(id, body)
    }
    fun build() = SymbolToolbarActionList(steps.toList())
}

fun symbolItemActions(
    item: SymbolItem,
    click: SymbolToolbarActionListBuilder.() -> Unit = {},
    longPress: SymbolToolbarActionListBuilder.() -> Unit = {}
): SymbolToolbarItemActionBindings {
    val clickBuilder = SymbolToolbarActionListBuilder().apply(click)
    val longBuilder = SymbolToolbarActionListBuilder().apply(longPress)
    return SymbolToolbarItemActionBindings(item, clickBuilder.build(), longBuilder.build())
}

/** Identity-keyed runtime bindings avoid inventing a second persisted item ID. */
class SymbolToolbarActionListRegistry {
    private val lock = Any()
    private val bindings = IdentityHashMap<SymbolItem, SymbolToolbarItemActionBindings>()

    fun register(value: SymbolToolbarItemActionBindings): AutoCloseable {
        synchronized(lock) {
            check(!bindings.containsKey(value.item)) { "Action lists already registered for this SymbolItem instance" }
            bindings[value.item] = value
        }
        var closed = false
        return AutoCloseable {
            synchronized(lock) {
                if (!closed) {
                    closed = true
                    if (bindings[value.item] === value) bindings.remove(value.item)
                }
            }
        }
    }

    fun replace(value: SymbolToolbarItemActionBindings) = synchronized(lock) { bindings[value.item] = value }
    fun find(item: SymbolItem): SymbolToolbarItemActionBindings? = synchronized(lock) { bindings[item] }
    fun remove(item: SymbolItem): Boolean = synchronized(lock) { bindings.remove(item) != null }
    fun clear() = synchronized(lock) { bindings.clear() }
    fun snapshot(): List<SymbolToolbarItemActionBindings> = synchronized(lock) { bindings.values.toList() }
}

suspend fun SymbolToolbarActionList.execute(
    invocation: SymbolToolbarInvocation,
    host: SymbolToolbarActionHost
): SymbolToolbarActionResult {
    for (step in steps) {
        val result = try {
            step.body(SymbolToolbarActionContext(invocation, host))
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            return SymbolToolbarActionResult.Failed(error)
        }
        if (result !is SymbolToolbarActionResult.Success) return result
    }
    return SymbolToolbarActionResult.Success
}
