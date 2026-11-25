/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConcurrentSnapshotPublisherTest {
    @Test
    fun reentrantPublicationIsQueuedAfterEveryListenerReceivesCurrentValue() {
        val publisher = ConcurrentSnapshotPublisher(0)
        val first = mutableListOf<Int>()
        val second = mutableListOf<Int>()
        var reentered = false

        publisher.addListener(emitCurrent = false) { value ->
            first += value
            if (value == 1 && !reentered) {
                reentered = true
                publisher.publish(2)
            }
        }
        publisher.addListener(emitCurrent = false) { second += it }

        publisher.publish(1)

        assertEquals(listOf(1, 2), first)
        assertEquals(listOf(1, 2), second)
        assertEquals(2, publisher.snapshot)
    }

    @Test
    fun repeatedCallbackRegistrationsHaveIndependentCloseableSubscriptions() {
        val publisher = ConcurrentSnapshotPublisher(0)
        val observed = mutableListOf<Int>()
        val callback: (Int) -> Unit = { observed += it }
        val first = publisher.addListener(emitCurrent = false, listener = callback)
        val second = publisher.addListener(emitCurrent = false, listener = callback)

        publisher.publish(1)
        first.close()
        publisher.publish(2)
        second.close()
        publisher.publish(3)

        assertEquals(listOf(1, 1, 2), observed)
    }

    @Test
    fun queuedPublicationRetainsRecipientsCapturedBeforeSubscriptionCloses() {
        val publisher = ConcurrentSnapshotPublisher(0)
        val observed = mutableListOf<Int>()
        lateinit var subscription: AutoCloseable
        publisher.addListener(emitCurrent = false) { value ->
            if (value == 1) subscription.close()
        }
        subscription = publisher.addListener(emitCurrent = false) { observed += it }

        publisher.publish(1)
        publisher.publish(2)

        assertEquals(listOf(1), observed)
    }

    @Test
    fun failingListenerDoesNotBlockOtherListenersOrQueueDrain() {
        val publisher = ConcurrentSnapshotPublisher(0)
        val observed = mutableListOf<Int>()
        publisher.addListener(emitCurrent = false) { error("observer failure") }
        publisher.addListener(emitCurrent = false) { observed += it }

        publisher.publish(1)

        assertEquals(listOf(1), observed)
        assertTrue(publisher.snapshot == 1)
    }

    @Test
    fun equalValuesAreSkippedButRefreshForcesCurrentPublication() {
        val publisher = ConcurrentSnapshotPublisher(5)
        val observed = mutableListOf<Int>()
        publisher.addListener(emitCurrent = false) { observed += it }

        publisher.publish(5)
        publisher.refresh()
        publisher.publish(6)

        assertEquals(listOf(5, 6), observed)
    }
}
