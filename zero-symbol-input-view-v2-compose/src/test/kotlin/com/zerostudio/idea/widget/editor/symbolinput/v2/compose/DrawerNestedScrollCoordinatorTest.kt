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

        assertEquals(-40f, first.consumedY, 0.0001f)
        assertEquals(-40f, second.consumedY, 0.0001f)
        assertEquals(-20f, third.consumedY, 0.0001f)
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
        assertEquals(-60f, coordinator.consumePreScroll(-100f).consumedY, 0.0001f)
    }

    @Test
    fun pendingMutationsCoalesceIntoSingleDrainLease() {
        val coordinator = DrawerNestedScrollCoordinator(0.2f, 100f)

        assertEquals(1L, coordinator.enqueueMutation(0.3f))
        assertEquals(null, coordinator.enqueueMutation(0.2f))
        assertEquals(null, coordinator.enqueueMutation(-0.1f))
        assertEquals(true, coordinator.hasPendingMutation())
        assertEquals(0.4f, coordinator.takePendingMutation(1L)!!, 0.0001f)
        assertEquals(true, coordinator.acknowledgeMutation(1L, 0.4f))
        assertEquals(null, coordinator.takePendingMutation(1L))
        assertEquals(false, coordinator.hasPendingMutation())
    }

    @Test
    fun enqueueAfterDrainLeaseReleaseStartsANewWorker() {
        val coordinator = DrawerNestedScrollCoordinator(0f, 100f)

        assertEquals(1L, coordinator.enqueueMutation(0.1f))
        assertEquals(0.1f, coordinator.takePendingMutation(1L)!!, 0.0001f)
        assertEquals(true, coordinator.acknowledgeMutation(1L, 0.1f))
        assertEquals(null, coordinator.takePendingMutation(1L))

        assertEquals(2L, coordinator.enqueueMutation(0.2f))
        assertEquals(0.2f, coordinator.takePendingMutation(2L)!!, 0.0001f)
        assertEquals(true, coordinator.acknowledgeMutation(2L, 0.2f))
        assertEquals(null, coordinator.takePendingMutation(2L))
    }


    @Test
    fun cancelledWorkerCanReleaseLeaseAndPendingWorkCanRestart() {
        val coordinator = DrawerNestedScrollCoordinator(0f, 100f)

        val cancelledLease = coordinator.enqueueMutation(0.25f)!!
        assertEquals(true, coordinator.hasPendingMutation())
        assertEquals(true, coordinator.releaseMutationDrain(cancelledLease))

        val replacementLease = coordinator.enqueueMutation(0.1f)!!
        assertEquals(0.35f, coordinator.takePendingMutation(replacementLease)!!, 0.0001f)
        assertEquals(true, coordinator.acknowledgeMutation(replacementLease, 0.35f))
        assertEquals(null, coordinator.takePendingMutation(replacementLease))
    }

    @Test
    fun cancelledInFlightMutationIsRestoredBeforeReplacementLease() {
        val coordinator = DrawerNestedScrollCoordinator(0f, 100f)
        val firstLease = coordinator.enqueueMutation(0.25f)!!
        assertEquals(0.25f, coordinator.takePendingMutation(firstLease)!!, 0.0001f)

        // Another callback arrives while the first batch is being applied.
        assertEquals(null, coordinator.enqueueMutation(0.1f))
        assertEquals(true, coordinator.releaseMutationDrain(firstLease))

        val replacementLease = coordinator.acquirePendingMutationDrain()!!
        assertEquals(0.35f, coordinator.takePendingMutation(replacementLease)!!, 0.0001f)
        assertEquals(true, coordinator.acknowledgeMutation(replacementLease, 0.35f))
        assertEquals(null, coordinator.takePendingMutation(replacementLease))
        assertEquals(false, coordinator.hasPendingMutation())
    }

    @Test
    fun acknowledgedInFlightMutationIsNotReplayedOnRelease() {
        val coordinator = DrawerNestedScrollCoordinator(0f, 100f)
        val lease = coordinator.enqueueMutation(0.2f)!!
        val delta = coordinator.takePendingMutation(lease)!!

        assertEquals(true, coordinator.acknowledgeMutation(lease, delta))
        assertEquals(true, coordinator.releaseMutationDrain(lease))
        assertEquals(null, coordinator.acquirePendingMutationDrain())
        assertEquals(false, coordinator.hasPendingMutation())
    }

    @Test
    fun staleWorkerCannotReleaseNewerDrainLease() {
        val coordinator = DrawerNestedScrollCoordinator(0f, 100f)

        val firstLease = coordinator.enqueueMutation(0.1f)!!
        assertEquals(0.1f, coordinator.takePendingMutation(firstLease)!!, 0.0001f)
        assertEquals(true, coordinator.acknowledgeMutation(firstLease, 0.1f))
        assertEquals(null, coordinator.takePendingMutation(firstLease))

        val secondLease = coordinator.enqueueMutation(0.2f)!!
        assertEquals(false, coordinator.releaseMutationDrain(firstLease))
        assertEquals(true, coordinator.hasPendingMutation())
        assertEquals(0.2f, coordinator.takePendingMutation(secondLease)!!, 0.0001f)
        assertEquals(true, coordinator.acknowledgeMutation(secondLease, 0.2f))
        assertEquals(null, coordinator.takePendingMutation(secondLease))
    }
}
