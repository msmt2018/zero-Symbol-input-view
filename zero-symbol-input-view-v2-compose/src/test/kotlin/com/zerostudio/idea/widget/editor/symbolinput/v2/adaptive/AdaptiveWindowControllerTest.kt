/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive

import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveWindowControllerTest {
    private fun info(width: Float, height: Float) = WindowAdaptiveInfo(
        widthDp = width,
        heightDp = height,
        density = 1f,
        insets = WindowInsetsDp(),
        foldingFeature = FoldingFeatureInfo()
    )

    @Test
    fun updatePublishesObservableSnapshotWithoutListenerRegistration() {
        val initial = info(400f, 800f)
        val updated = info(720f, 540f)
        val controller = AdaptiveWindowController(initial)

        assertEquals(initial, controller.snapshot)
        controller.update(updated)

        assertEquals(updated, controller.snapshot)
        assertEquals(updated, controller.latest())
    }

    @Test
    fun equalSnapshotDoesNotReplacePublishedValue() {
        val initial = info(400f, 800f)
        val controller = AdaptiveWindowController(initial)

        controller.update(initial)

        assertEquals(initial, controller.snapshot)
    }
    @Test
    fun refreshReemitsLatestSnapshotAndClosedListenersAreNotCalled() {
        val initial = info(400f, 800f)
        val updated = info(720f, 540f)
        val controller = AdaptiveWindowController(initial)
        val received = mutableListOf<WindowAdaptiveInfo>()
        val subscription = controller.addListener(emitCurrent = false) {
            received += it
        }

        controller.update(updated)
        controller.refresh()

        assertEquals(listOf(updated, updated), received)

        subscription.close()
        controller.update(info(900f, 700f))
        controller.refresh()

        assertEquals(listOf(updated, updated), received)
    }

}
