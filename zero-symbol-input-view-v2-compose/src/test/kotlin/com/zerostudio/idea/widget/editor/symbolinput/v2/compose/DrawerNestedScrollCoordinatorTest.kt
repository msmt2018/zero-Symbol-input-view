/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.compose

import org.junit.Assert.assertEquals
import org.junit.Test

class DrawerNestedScrollCoordinatorTest {
    @Test
    fun consecutiveUpwardCallbacksConsumeAgainstProjectedPosition() {
        val coordinator = DrawerNestedScrollCoordinator(0f, 100f)

        val first = coordinator.consumePreScroll(-40f)
        val second = coordinator.consumePreScroll(-40f)
        val third = coordinator.consumePreScroll(-40f)

        assertEquals(-40f, first.consumedY, 0f)
        assertEquals(-40f, second.consumedY, 0f)
        assertEquals(-20f, third.consumedY, 0f)
        assertEquals(1f, coordinator.projectedFraction(), 0f)
    }

    @Test
    fun oppositeDirectionImmediatelyCancelsPendingProjection() {
        val coordinator = DrawerNestedScrollCoordinator(0f, 100f)

        coordinator.consumePreScroll(-60f)
        val reverse = coordinator.consumePostScroll(25f)

        assertEquals(25f, reverse.consumedY, 0f)
        assertEquals(-0.25f, reverse.deltaFraction, 0.0001f)
        assertEquals(0.35f, coordinator.projectedFraction(), 0.0001f)
    }

    @Test
    fun rangeChangePreservesFractionAndRecalculatesPixelCapacity() {
        val coordinator = DrawerNestedScrollCoordinator(0.5f, 100f)
        coordinator.updateRange(200f)

        val collapse = coordinator.consumePostScroll(200f)

        assertEquals(100f, collapse.consumedY, 0f)
        assertEquals(0f, coordinator.projectedFraction(), 0f)
    }

    @Test
    fun reconcileRebasesAfterExternalStateMutation() {
        val coordinator = DrawerNestedScrollCoordinator(0.2f, 100f)
        coordinator.consumePreScroll(-50f)
        coordinator.reconcile(0.4f)

        assertEquals(0.4f, coordinator.projectedFraction(), 0f)
        assertEquals(-60f, coordinator.consumePreScroll(-100f).consumedY, 0f)
    }
}
