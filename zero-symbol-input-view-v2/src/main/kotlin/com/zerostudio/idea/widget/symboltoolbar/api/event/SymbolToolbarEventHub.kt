/*
 * Event subscription and dispatch implementation.
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.symboltoolbar.api.event

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

fun interface SymbolToolbarEventListener {
    fun onEvent(event: SymbolToolbarEvent)
}

/**
 * Lightweight, thread-safe event hub scoped to one toolbar runtime.
 * Listener exceptions are isolated and cannot interrupt UI/action dispatch.
 */
class SymbolToolbarEventHub(
    private val controlId: String,
    private val clock: () -> Long = System::currentTimeMillis
) : AutoCloseable {
    private data class Entry(
        val id: Long,
        val filter: (SymbolToolbarEvent) -> Boolean,
        val listener: SymbolToolbarEventListener
    )

    private val ids = AtomicLong()
    private val sequence = AtomicLong()
    private val entries = CopyOnWriteArrayList<Entry>()
    private val closed = AtomicBoolean(false)

    init {
        require(controlId.isNotBlank()) { "controlId must not be blank" }
    }

    fun nextSequence(): Long = sequence.incrementAndGet()
    fun timestamp(): Long = clock()

    fun subscribe(
        listener: SymbolToolbarEventListener
    ): AutoCloseable = subscribe(filter = { true }, listener = listener)

    fun subscribe(
        eventType: Class<out SymbolToolbarEvent>,
        listener: SymbolToolbarEventListener
    ): AutoCloseable = subscribe(filter = { eventType.isInstance(it) }, listener = listener)

    fun subscribe(
        filter: (SymbolToolbarEvent) -> Boolean,
        listener: SymbolToolbarEventListener
    ): AutoCloseable {
        check(!closed.get()) { "Event hub is closed" }
        val entry = Entry(ids.incrementAndGet(), filter, listener)
        entries += entry
        val disposed = AtomicBoolean(false)
        return AutoCloseable {
            if (disposed.compareAndSet(false, true)) entries.remove(entry)
        }
    }

    inline fun <reified E : SymbolToolbarEvent> on(
        noinline listener: (E) -> Unit
    ): AutoCloseable = subscribe(E::class.java) { event ->
        @Suppress("UNCHECKED_CAST")
        listener(event as E)
    }

    fun publish(event: SymbolToolbarEvent) {
        if (closed.get()) return
        require(event.controlId == controlId) {
            "Event belongs to control '${event.controlId}', expected '$controlId'"
        }
        entries.forEach { entry ->
            val matches = runCatching { entry.filter(event) }.getOrDefault(false)
            if (matches) runCatching { entry.listener.onEvent(event) }
        }
    }

    fun listenerCount(): Int = entries.size

    override fun close() {
        if (closed.compareAndSet(false, true)) entries.clear()
    }
}
