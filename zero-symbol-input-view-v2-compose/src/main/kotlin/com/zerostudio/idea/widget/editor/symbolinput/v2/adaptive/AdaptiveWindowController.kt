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
    private var nextListenerId = 0L
    @Volatile private var latestSnapshot: WindowAdaptiveInfo = initialSnapshot

    fun latest(): WindowAdaptiveInfo = latestSnapshot

    fun addListener(
        emitCurrent: Boolean = true,
        listener: (WindowAdaptiveInfo) -> Unit
    ): AutoCloseable {
        val id: Long
        val snapshot: WindowAdaptiveInfo
        synchronized(lock) {
            id = ++nextListenerId
            listeners[id] = listener
            snapshot = latestSnapshot
        }
        if (emitCurrent) runCatching { listener(snapshot) }
        return AutoCloseable { synchronized(lock) { listeners.remove(id) } }
    }

    fun update(value: WindowAdaptiveInfo) {
        val callbacks = synchronized(lock) {
            if (latestSnapshot == value) return
            latestSnapshot = value
            listeners.values.toList()
        }
        callbacks.forEach { callback -> runCatching { callback(value) } }
    }

    /** Re-emits the current snapshot after host policy changes. */
    fun refresh() {
        val snapshot = latestSnapshot
        val callbacks = synchronized(lock) { listeners.values.toList() }
        callbacks.forEach { callback -> runCatching { callback(snapshot) } }
    }
}
