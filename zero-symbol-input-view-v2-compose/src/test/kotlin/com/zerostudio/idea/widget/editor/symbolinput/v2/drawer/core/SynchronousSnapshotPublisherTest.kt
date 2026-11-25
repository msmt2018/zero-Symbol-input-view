/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class SynchronousSnapshotPublisherTest {
    @Test
    fun emitsInitialSnapshotSynchronouslyAndIgnoresDuplicateSubscription() {
        val publisher = SynchronousSnapshotPublisher<Int>()
        val received = mutableListOf<Int>()
        val listener: (Int) -> Unit = received::add

        publisher.addListener(listener, 7)
        publisher.addListener(listener, 8)
        publisher.publish(9)

        assertEquals(listOf(7, 9), received)
    }

    @Test
    fun reentrantPublicationWaitsUntilEveryListenerReceivesCurrentSnapshot() {
        val publisher = SynchronousSnapshotPublisher<Int>()
        val first = mutableListOf<Int>()
        val second = mutableListOf<Int>()
        var reentered = false

        publisher.addListener({ value ->
            first += value
            if (value == 1 && !reentered) {
                reentered = true
                publisher.publish(2)
            }
        }, 0)
        publisher.addListener({ value -> second += value }, 0)

        publisher.publish(1)

        assertEquals(listOf(0, 1, 2), first)
        assertEquals(listOf(0, 1, 2), second)
    }

    @Test
    fun observerFailuresDoNotBlockOtherObserversOrQueuedSnapshots() {
        val publisher = SynchronousSnapshotPublisher<Int>()
        val firstFailure = IllegalStateException("first observer failed")
        val secondFailure = IllegalArgumentException("second observer failed")
        val receivedBySecond = mutableListOf<Int>()

        publisher.addListener({ value ->
            if (value == 1) {
                publisher.publish(2)
                throw firstFailure
            }
        }, 0)
        publisher.addListener({ value ->
            if (value == 1) throw secondFailure
            receivedBySecond += value
        }, 0)

        val thrown = assertThrows(IllegalStateException::class.java) {
            publisher.publish(1)
        }

        assertSame(firstFailure, thrown)
        assertEquals(listOf(secondFailure), thrown.suppressed.toList())
        assertEquals(listOf(0, 2), receivedBySecond)
    }

    @Test
    fun listenerRejectingInitialSnapshotIsRolledBack() {
        val publisher = SynchronousSnapshotPublisher<Int>()
        var calls = 0
        val listener: (Int) -> Unit = {
            calls += 1
            throw IllegalStateException("initial state rejected")
        }

        assertThrows(IllegalStateException::class.java) {
            publisher.addListener(listener, 0)
        }
        publisher.publish(1)

        assertEquals(1, calls)
    }

    @Test
    fun removedListenerReceivesNoFurtherSnapshots() {
        val publisher = SynchronousSnapshotPublisher<Int>()
        val received = mutableListOf<Int>()
        val listener: (Int) -> Unit = received::add

        publisher.addListener(listener, 0)
        publisher.removeListener(listener)
        publisher.publish(1)

        assertEquals(listOf(0), received)
    }
}
