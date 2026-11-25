/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core

/**
 * Synchronous, reentrancy-safe publication of immutable snapshots.
 *
 * This primitive is deliberately independent of Android, Compose and
 * coroutine dispatchers. The owner serializes calls on one thread. Listener
 * callbacks execute on the publishing thread; reentrant publications are
 * queued until every listener has received the current snapshot.
 */
internal class SynchronousSnapshotPublisher<T> {
    private val listeners = LinkedHashSet<(T) -> Unit>()
    private val pendingSnapshots = java.util.ArrayDeque<T>()
    private var dispatching = false

    /**
     * Registers [listener] and immediately supplies [initialSnapshot].
     * A listener that rejects its initial state is rolled back.
     */
    fun addListener(listener: (T) -> Unit, initialSnapshot: T) {
        if (!listeners.add(listener)) return
        try {
            listener(initialSnapshot)
        } catch (failure: RuntimeException) {
            listeners.remove(listener)
            throw failure
        }
    }

    fun removeListener(listener: (T) -> Unit) {
        listeners.remove(listener)
    }

    fun publish(snapshot: T) {
        pendingSnapshots.addLast(snapshot)
        if (dispatching) return

        dispatching = true
        var dispatchFailure: RuntimeException? = null
        try {
            while (pendingSnapshots.isNotEmpty()) {
                val current = pendingSnapshots.removeFirst()
                listeners.toList().forEach { listener ->
                    try {
                        listener(current)
                    } catch (failure: RuntimeException) {
                        val firstFailure = dispatchFailure
                        if (firstFailure == null) dispatchFailure = failure
                        else firstFailure.addSuppressed(failure)
                    }
                }
            }
        } finally {
            dispatching = false
        }

        // Deliver every queued snapshot before reporting observer failures.
        dispatchFailure?.let { throw it }
    }
}
