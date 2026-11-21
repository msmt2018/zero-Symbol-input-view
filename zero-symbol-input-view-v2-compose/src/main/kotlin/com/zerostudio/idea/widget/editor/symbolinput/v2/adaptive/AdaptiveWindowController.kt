/*
 * This file is part of zero-Symbol-input-view-V2.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Optional platform-neutral adaptive-window snapshot publisher.
 *
 * Android/Compose hosts supply immutable measurements; this model never
 * observes a View or owns window/listener lifecycle.
 */
@Stable
class AdaptiveWindowController(initialSnapshot: WindowAdaptiveInfo) {
    private val lock = Any()
    private val listeners = LinkedHashMap<Long, (WindowAdaptiveInfo) -> Unit>()
    private val pendingNotifications =
        java.util.ArrayDeque<Pair<WindowAdaptiveInfo, List<(WindowAdaptiveInfo) -> Unit>>>()
    private var dispatchingNotifications = false
    private var nextListenerId = 0L
    private var latestSnapshot by mutableStateOf(initialSnapshot)

    /**
     * Observable immutable snapshot. Reading this property from a Composable
     * subscribes that composition to adaptive-window updates without registering
     * a callback or managing a disposable listener.
     */
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


/**
 * Remembers an adaptive-window state publisher for the calling Composition.
 *
 * The initial value is used only when the controller is first created. New
 * measurements should be delivered through [AdaptiveWindowController.update].
 */
@Composable
fun rememberAdaptiveWindowController(
    initialSnapshot: WindowAdaptiveInfo
): AdaptiveWindowController = remember {
    AdaptiveWindowController(initialSnapshot)
}
