/*
 * This file is part of ZeroStudio.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive

import android.view.View

/**
 * Lifecycle-safe shared window observation for V2 widgets.
 *
 * A control can subscribe to the same controller instead of installing its own
 * layout/insets listeners. The controller owns one observer and fans out
 * immutable snapshots to registered listeners.
 */
class AdaptiveWindowController(
    hostView: View,
    insetsMode: WindowInsetsMode = WindowInsetsMode.SUBTRACT_SYSTEM_INSETS
) : AutoCloseable {
    private val listeners = LinkedHashSet<(WindowAdaptiveInfo) -> Unit>()
    private var closed = false
    private var latestSnapshot: WindowAdaptiveInfo? = null

    private val observer = AdaptiveWindowObserver(
        view = hostView,
        onChanged = ::dispatch,
        insetsMode = insetsMode
    )

    val current: WindowAdaptiveInfo?
        get() = latestSnapshot

    fun addListener(
        listener: (WindowAdaptiveInfo) -> Unit,
        emitCurrent: Boolean = true
    ): AutoCloseable {
        check(!closed) { "AdaptiveWindowController is closed" }
        listeners += listener
        if (emitCurrent) latestSnapshot?.let(listener)
        return AutoCloseable { listeners -= listener }
    }

    fun updateFoldingFeature(value: FoldingFeatureInfo) {
        check(!closed) { "AdaptiveWindowController is closed" }
        observer.updateFoldingFeature(value)
    }

    fun refresh() {
        check(!closed) { "AdaptiveWindowController is closed" }
        observer.refresh()
    }

    private fun dispatch(snapshot: WindowAdaptiveInfo) {
        if (closed) return
        latestSnapshot = snapshot
        listeners.toList().forEach { it(snapshot) }
    }

    override fun close() {
        if (closed) return
        closed = true
        observer.close()
        listeners.clear()
        latestSnapshot = null
    }
}
