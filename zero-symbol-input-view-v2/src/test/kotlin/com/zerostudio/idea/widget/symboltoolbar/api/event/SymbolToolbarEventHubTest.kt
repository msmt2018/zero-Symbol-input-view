package com.zerostudio.idea.widget.symboltoolbar.api.event

import org.junit.Assert.*
import org.junit.Test

class SymbolToolbarEventHubTest {
    @Test
    fun publishesOnlyMatchingControlAndSupportsTypedSubscription() {
        val hub = SymbolToolbarEventHub("toolbar.main")
        var received = 0
        val subscription = hub.on<SymbolToolbarLifecycleEvent.Attached> { received++ }

        hub.publish(SymbolToolbarLifecycleEvent.Attached(
            "toolbar.main", hub.timestamp(), hub.nextSequence()
        ))
        hub.publish(SymbolToolbarLifecycleEvent.Ready(
            "toolbar.main", hub.timestamp(), hub.nextSequence()
        ))

        assertEquals(1, received)
        subscription.close()
        assertEquals(0, hub.listenerCount())
        hub.close()
    }

    @Test
    fun listenerFailureDoesNotBreakOtherListeners() {
        val hub = SymbolToolbarEventHub("toolbar.main")
        var called = false
        hub.subscribe { throw IllegalStateException("listener failure") }
        hub.subscribe { called = true }

        hub.publish(SymbolToolbarLifecycleEvent.Ready(
            "toolbar.main", hub.timestamp(), hub.nextSequence()
        ))

        assertTrue(called)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEventsFromAnotherControl() {
        val hub = SymbolToolbarEventHub("toolbar.main")
        hub.publish(SymbolToolbarLifecycleEvent.Ready(
            "toolbar.other", hub.timestamp(), hub.nextSequence()
        ))
    }

    @Test
    fun closeRemovesListenersAndStopsDelivery() {
        val hub = SymbolToolbarEventHub("toolbar.main")
        var called = false
        hub.subscribe { called = true }
        hub.close()
        hub.publish(SymbolToolbarLifecycleEvent.Ready(
            "toolbar.main", hub.timestamp(), hub.nextSequence()
        ))
        assertFalse(called)
        assertEquals(0, hub.listenerCount())
    }
}
