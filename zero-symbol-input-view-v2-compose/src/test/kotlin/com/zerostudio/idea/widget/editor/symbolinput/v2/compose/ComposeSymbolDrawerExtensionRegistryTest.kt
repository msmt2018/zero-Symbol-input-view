/*
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.compose

import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposeSymbolDrawerExtensionRegistryTest {
    @Test
    fun closingReplacedRegistrationDoesNotRemoveReplacementWhenInstanceIsReused() {
        val registry = ComposeSymbolDrawerExtensionRegistry()
        val extension = ComposeSymbolDrawerActionExtension(
            id = "test.action",
            title = "Test action",
            handler = ComposeSymbolActionHandler { _, _ -> true }
        )

        val first = registry.registerAction(extension)
        val replacement = registry.registerAction(extension)

        first.close()
        assertEquals(listOf(extension), registry.snapshot().actions)

        replacement.close()
        assertTrue(registry.snapshot().actions.isEmpty())
    }

    @Test
    fun closeIsIdempotentAndClearInvalidatesOutstandingHandles() {
        val registry = ComposeSymbolDrawerExtensionRegistry()
        val extension = ComposeSymbolDrawerActionExtension(
            id = "test.action",
            title = "Test action",
            handler = ComposeSymbolActionHandler { _, _ -> true }
        )
        val handle = registry.registerAction(extension)

        registry.clear()
        handle.close()
        handle.close()

        assertTrue(registry.snapshot().actions.isEmpty())
    }

    @Test
    fun observerNotificationsAreDeliveredInMonotonicRevisionOrder() {
        val registry = ComposeSymbolDrawerExtensionRegistry()
        val firstCallbackEntered = CountDownLatch(1)
        val releaseFirstCallback = CountDownLatch(1)
        val secondMutationVisible = CountDownLatch(1)
        val revisions = Collections.synchronizedList(mutableListOf<Long>())

        registry.observe(emitCurrent = false) { revision ->
            if (revision == 1L) {
                firstCallbackEntered.countDown()
                check(releaseFirstCallback.await(5, TimeUnit.SECONDS))
            }
            // Record completion order, not entry order: this exposes a newer
            // callback overtaking a stalled older callback.
            revisions += revision
        }

        val firstWriter = Thread {
            registry.registerAction(action("test.first"))
        }
        firstWriter.start()
        assertTrue(firstCallbackEntered.await(5, TimeUnit.SECONDS))

        val secondWriter = Thread {
            registry.registerAction(action("test.second"))
            secondMutationVisible.countDown()
        }
        secondWriter.start()

        // The second mutation is committed while its notification waits for
        // the first observer delivery to finish.
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (registry.snapshot().actions.size < 2 && System.nanoTime() < deadline) {
            Thread.yield()
        }
        assertEquals(2, registry.snapshot().actions.size)

        releaseFirstCallback.countDown()
        firstWriter.join(5_000)
        secondWriter.join(5_000)
        assertTrue(!firstWriter.isAlive)
        assertTrue(!secondWriter.isAlive)
        assertTrue(secondMutationVisible.await(5, TimeUnit.SECONDS))
        assertEquals(listOf(1L, 2L), revisions.toList())
    }


    @Test
    fun reentrantRegistrationDoesNotDeliverOlderRevisionAfterNewerRevision() {
        val registry = ComposeSymbolDrawerExtensionRegistry()
        val observedBySecondListener = mutableListOf<Long>()

        registry.observe(emitCurrent = false) { revision ->
            if (revision == 1L) registry.registerAction(action("test.reentrant"))
        }
        registry.observe(emitCurrent = false) { revision -> observedBySecondListener += revision }

        registry.registerAction(action("test.initial"))

        assertEquals(listOf(1L, 2L), observedBySecondListener)
    }

    @Test
    fun observeCanEmitCurrentRevisionImmediately() {
        val registry = ComposeSymbolDrawerExtensionRegistry()
        registry.registerAction(action("test.existing"))
        val revisions = mutableListOf<Long>()

        val handle = registry.observe(emitCurrent = true) { revisions += it }

        assertEquals(listOf(1L), revisions)
        handle.close()
    }

    @Test
    fun initialRevisionUsesSerializedQueueWhenObserverMutatesRegistry() {
        val registry = ComposeSymbolDrawerExtensionRegistry()
        registry.registerAction(action("test.existing"))
        val revisions = mutableListOf<Long>()

        val handle = registry.observe(emitCurrent = true) { revision ->
            revisions += revision
            if (revision == 1L) {
                registry.registerAction(action("test.added.during.initial"))
            }
        }

        assertEquals(listOf(1L, 2L), revisions)
        assertEquals(2, registry.snapshot().actions.size)
        handle.close()
    }

    private fun action(id: String) = ComposeSymbolDrawerActionExtension(
        id = id,
        title = id,
        handler = ComposeSymbolActionHandler { _, _ -> true }
    )
}
