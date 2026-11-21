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
        // Capture the value and its subscribers in one critical section. Reading
        // the snapshot before acquiring the lock could let a concurrent update
        // dispatch a newer value first and this refresh dispatch an older value
        // afterwards.
        val (snapshot, callbacks) = synchronized(lock) {
            latestSnapshot to listeners.values.toList()
        }
        callbacks.forEach { callback -> runCatching { callback(snapshot) } }
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
