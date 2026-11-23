/*
 * This file is part of zero-Symbol-input-view-V2.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive


/**
 * Optional platform-neutral adaptive-window snapshot publisher.
 *
 * Android/Compose hosts supply immutable measurements; this model never
 * observes a View or owns window/listener lifecycle.
 */
class AdaptiveWindowController(initialSnapshot: WindowAdaptiveInfo) {
    private val lock = Any()
    private val listeners = LinkedHashMap<Long, (WindowAdaptiveInfo) -> Unit>()
    private val pendingNotifications =
        java.util.ArrayDeque<Pair<WindowAdaptiveInfo, List<(WindowAdaptiveInfo) -> Unit>>>()
    private var dispatchingNotifications = false
    private var nextListenerId = 0L
    @Volatile private var latestSnapshot: WindowAdaptiveInfo = initialSnapshot

    /** Latest immutable snapshot; Composables should use rememberAdaptiveWindowSnapshot. */
    val snapshot: WindowAdaptiveInfo get() = latestSnapshot

    fun latest(): WindowAdaptiveInfo = latestSnapshot

    fun addListener(
        emitCurrent: Boolean = true,
        listener: (WindowAdaptiveInfo) -> Unit
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
        if (shouldDrain) drainNotifications()
        return AutoCloseable { synchronized(lock) { listeners.remove(id) } }
    }

    fun update(value: WindowAdaptiveInfo) {
        val shouldDrain = synchronized(lock) {
            if (latestSnapshot == value) return
            latestSnapshot = value
            enqueueLocked(value, listeners.values.toList())
        }
        if (shouldDrain) drainNotifications()
    }

    /** Re-emits the current snapshot after host policy changes. */
    fun refresh() {
        // Capture the value and its subscribers in one critical section. Queue
        // insertion shares that lock, preserving order across concurrent and
        // reentrant update/refresh calls.
        val shouldDrain = synchronized(lock) {
            enqueueLocked(latestSnapshot, listeners.values.toList())
        }
        if (shouldDrain) drainNotifications()
    }

    /** Must be called while [lock] is held. */
    private fun enqueueLocked(
        snapshot: WindowAdaptiveInfo,
        callbacks: List<(WindowAdaptiveInfo) -> Unit>
    ): Boolean {
        pendingNotifications.addLast(snapshot to callbacks)
        if (dispatchingNotifications) return false
        dispatchingNotifications = true
        return true
    }

    private fun drainNotifications() {
        while (true) {
            val next = synchronized(lock) {
                if (pendingNotifications.isEmpty()) {
                    dispatchingNotifications = false
                    null
                } else {
                    pendingNotifications.removeFirst()
                }
            } ?: return

            // Preserve the controller's best-effort observer contract: one
            // failing callback must not prevent delivery to other observers or
            // strand notifications queued by a reentrant update.
            next.second.forEach { callback ->
                runCatching { callback(next.first) }
            }
        }
    }
}

