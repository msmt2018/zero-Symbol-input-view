/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive

/**
 * Thread-safe snapshot publication primitive for platform-neutral state.
 *
 * Each publication captures its recipient list when it is enqueued. Reentrant
 * and concurrent publications are serialized in queue order, while callbacks
 * always execute outside the lock on the thread that starts the drain.
 *
 * Every registration owns an independent subscription, even when the same
 * callback instance is registered more than once. Closing a subscription
 * prevents future publications from including it; already queued publications
 * retain their captured recipient list.
 */
internal class ConcurrentSnapshotPublisher<T>(initialSnapshot: T) {
    private data class Publication<T>(
        val snapshot: T,
        val recipients: List<(T) -> Unit>
    )

    private val lock = Any()
    private val listeners = LinkedHashMap<Long, (T) -> Unit>()
    private val pending = java.util.ArrayDeque<Publication<T>>()
    private var nextListenerId = 0L
    private var dispatching = false

    @Volatile
    private var latestSnapshot: T = initialSnapshot

    val snapshot: T
        get() = latestSnapshot

    fun addListener(
        emitCurrent: Boolean = true,
        listener: (T) -> Unit
    ): AutoCloseable {
        val (id, shouldDrain) = synchronized(lock) {
            val newId = ++nextListenerId
            listeners[newId] = listener
            newId to if (emitCurrent) {
                enqueueLocked(latestSnapshot, listOf(listener))
            } else {
                false
            }
        }
        if (shouldDrain) drain()
        return AutoCloseable {
            synchronized(lock) {
                listeners.remove(id)
            }
        }
    }

    /**
     * Publishes [value]. Equal values are ignored unless [force] is true.
     * Returns without waiting when another thread is already draining.
     */
    fun publish(value: T, force: Boolean = false) {
        val shouldDrain = synchronized(lock) {
            if (!force && latestSnapshot == value) return
            latestSnapshot = value
            enqueueLocked(value, listeners.values.toList())
        }
        if (shouldDrain) drain()
    }

    /** Re-emits the latest value to the listeners registered at enqueue time. */
    fun refresh() {
        val shouldDrain = synchronized(lock) {
            enqueueLocked(latestSnapshot, listeners.values.toList())
        }
        if (shouldDrain) drain()
    }

    /** Must be called while [lock] is held. */
    private fun enqueueLocked(
        snapshot: T,
        recipients: List<(T) -> Unit>
    ): Boolean {
        pending.addLast(Publication(snapshot, recipients))
        if (dispatching) return false
        dispatching = true
        return true
    }

    private fun drain() {
        while (true) {
            val next = synchronized(lock) {
                if (pending.isEmpty()) {
                    dispatching = false
                    null
                } else {
                    pending.removeFirst()
                }
            } ?: return

            // This publisher intentionally has a best-effort observer contract:
            // one faulty callback must not strand the queue or block its peers.
            next.recipients.forEach { callback ->
                runCatching { callback(next.snapshot) }
            }
        }
    }
}
