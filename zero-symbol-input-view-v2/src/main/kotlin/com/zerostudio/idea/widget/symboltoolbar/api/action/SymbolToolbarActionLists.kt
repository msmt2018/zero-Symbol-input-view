/*
 * Developer-authored click and long-press action pipelines.
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.symboltoolbar.api.action

import com.zerostudio.idea.widget.symboltoolbar.api.model.SymbolToolbarInvocation
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * An executable action step. The body is ordinary developer Kotlin code and may
 * call application services or suspend; it receives only the explicitly granted
 * toolbar action context.
 */
typealias SymbolToolbarActionStep = suspend SymbolToolbarActionContext.() -> SymbolToolbarActionResult

data class SymbolToolbarActionStepDefinition(
    val id: String,
    val body: SymbolToolbarActionStep
) {
    init {
        require(id.isNotBlank()) { "Action step id must not be blank" }
    }
}

/** Immutable, ordered action list for one gesture. */
data class SymbolToolbarActionList(
    val steps: List<SymbolToolbarActionStepDefinition>
) {
    init {
        require(steps.map { it.id }.distinct().size == steps.size) {
            "Action step IDs must be unique within a list"
        }
    }

    val isEmpty: Boolean get() = steps.isEmpty()
}

/** Per-symbol click and long-press pipelines; the two lists never fall through to each other. */
data class SymbolToolbarItemActionBindings(
    val itemId: String,
    val click: SymbolToolbarActionList = SymbolToolbarActionList(emptyList()),
    val longPress: SymbolToolbarActionList = SymbolToolbarActionList(emptyList())
) {
    init {
        require(itemId.isNotBlank()) { "itemId must not be blank" }
    }

    fun forGesture(gesture: SymbolToolbarInvocation.Gesture): SymbolToolbarActionList =
        when (gesture) {
            SymbolToolbarInvocation.Gesture.LONG_PRESS -> longPress
            SymbolToolbarInvocation.Gesture.TAP,
            SymbolToolbarInvocation.Gesture.ACCESSIBILITY -> click
        }
}

/** DSL used to author one gesture's ordered sequence of arbitrary Kotlin handlers. */
class SymbolToolbarActionListBuilder internal constructor() {
    private val steps = mutableListOf<SymbolToolbarActionStepDefinition>()

    fun step(id: String, body: SymbolToolbarActionStep) {
        require(steps.none { it.id == id }) { "Duplicate action step id: $id" }
        steps += SymbolToolbarActionStepDefinition(id, body)
    }

    fun build(): SymbolToolbarActionList = SymbolToolbarActionList(steps.toList())
}

/** Concise declaration of independent click and long-press action lists. */
fun symbolItemActions(
    itemId: String,
    click: SymbolToolbarActionListBuilder.() -> Unit = {},
    longPress: SymbolToolbarActionListBuilder.() -> Unit = {}
): SymbolToolbarItemActionBindings {
    val clickBuilder = SymbolToolbarActionListBuilder().apply(click)
    val longPressBuilder = SymbolToolbarActionListBuilder().apply(longPress)
    return SymbolToolbarItemActionBindings(
        itemId = itemId,
        click = clickBuilder.build(),
        longPress = longPressBuilder.build()
    )
}

/**
 * Runtime store for per-item pipelines. Replacing bindings is atomic; returned
 * handles remove only the exact registration they created.
 */
class SymbolToolbarActionListRegistry {
    private val bindings = ConcurrentHashMap<String, SymbolToolbarItemActionBindings>()

    fun register(value: SymbolToolbarItemActionBindings): AutoCloseable {
        check(bindings.putIfAbsent(value.itemId, value) == null) {
            "Action lists already registered for item: ${value.itemId}"
        }
        val closed = AtomicBoolean(false)
        return AutoCloseable {
            if (closed.compareAndSet(false, true)) bindings.remove(value.itemId, value)
        }
    }

    fun replace(value: SymbolToolbarItemActionBindings) {
        bindings[value.itemId] = value
    }

    fun find(itemId: String): SymbolToolbarItemActionBindings? = bindings[itemId]

    fun remove(itemId: String): Boolean = bindings.remove(itemId) != null

    fun clear() = bindings.clear()

    fun snapshot(): Map<String, SymbolToolbarItemActionBindings> = bindings.toMap()
}

/** Executes each step in order and stops on the first non-success result. */
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
