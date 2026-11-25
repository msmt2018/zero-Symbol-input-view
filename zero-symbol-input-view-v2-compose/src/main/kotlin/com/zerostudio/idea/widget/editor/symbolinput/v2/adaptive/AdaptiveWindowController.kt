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
 * observes a View or owns window/listener lifecycle. Thread-safe publication
 * and observer delivery are delegated to [ConcurrentSnapshotPublisher].
 */
class AdaptiveWindowController(initialSnapshot: WindowAdaptiveInfo) {
    private val publisher = ConcurrentSnapshotPublisher(initialSnapshot)

    /** Latest immutable snapshot; Composables should use rememberAdaptiveWindowSnapshot. */
    val snapshot: WindowAdaptiveInfo
        get() = publisher.snapshot

    fun latest(): WindowAdaptiveInfo = publisher.snapshot

    fun addListener(
        emitCurrent: Boolean = true,
        listener: (WindowAdaptiveInfo) -> Unit
    ): AutoCloseable = publisher.addListener(emitCurrent, listener)

    fun update(value: WindowAdaptiveInfo) {
        publisher.publish(value)
    }

    /** Re-emits the current snapshot after host policy changes. */
    fun refresh() {
        publisher.refresh()
    }
}
