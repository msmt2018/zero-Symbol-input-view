package com.zerostudio.idea.widget.symboltoolbar.api.extension

import org.junit.Assert.*
import org.junit.Test

class SymbolToolbarExtensionRegistryTest {
    private data class Provider(
        override val id: String,
        override val title: String,
        override val priority: Int
    ) : SymbolToolbarGroupProvider {
        override suspend fun loadGroups() = emptyList<com.zerostudio.idea.widget.symboltoolbar.api.model.SymbolToolbarGroup>()
    }

    @Test
    fun registrationIsDiscoverableAndHandleRemovesOnlyItsOwnEntry() {
        val registry = SymbolToolbarExtensionRegistry()
        val provider = Provider("sample.provider", "Sample", 10)
        val handle = registry.registerGroupProvider(provider)

        assertEquals(listOf(provider), registry.groupProviders())
        assertEquals(provider, registry.snapshot().groupProviders.single())

        handle.close()
        handle.close()
        assertTrue(registry.groupProviders().isEmpty())
    }

    @Test
    fun duplicateIdsAreRejectedWithinSameCategory() {
        val registry = SymbolToolbarExtensionRegistry()
        registry.registerGroupProvider(Provider("sample.provider", "First", 0))

        try {
            registry.registerGroupProvider(Provider("sample.provider", "Second", 0))
            fail("duplicate registration should fail")
        } catch (_: IllegalStateException) {
            // Expected: extension replacement must be explicit.
        }
    }

    @Test
    fun priorityAndIdProvideDeterministicOrdering() {
        val registry = SymbolToolbarExtensionRegistry()
        registry.registerGroupProvider(Provider("z.provider", "Z", 5))
        registry.registerGroupProvider(Provider("b.provider", "B", 1))
        registry.registerGroupProvider(Provider("a.provider", "A", 1))

        assertEquals(
            listOf("a.provider", "b.provider", "z.provider"),
            registry.groupProviders().map { it.id }
        )
    }

    @Test
    fun observersReceiveChangesAndCanUnsubscribe() {
        val registry = SymbolToolbarExtensionRegistry()
        val revisions = mutableListOf<Long>()
        val observer = registry.observe { revisions += it.revision }
        val handle = registry.registerGroupProvider(Provider("sample.provider", "Sample", 0))

        assertEquals(2, revisions.size)
        assertTrue(revisions.last() > revisions.first())

        observer.close()
        val count = revisions.size
        handle.close()
        assertEquals(count, revisions.size)
    }

    @Test
    fun clearRemovesEveryRuntimeContributionAndPublishesChange() {
        val registry = SymbolToolbarExtensionRegistry()
        registry.registerGroupProvider(Provider("sample.provider", "Sample", 0))
        val before = registry.revision

        registry.clear()

        assertTrue(registry.groupProviders().isEmpty())
        assertTrue(registry.snapshot().actionIds.isEmpty())
        assertTrue(registry.revision > before)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidExtensionIdIsRejected() {
        SymbolToolbarExtensionRegistry().registerGroupProvider(Provider("bad id", "Bad", 0))
    }
}
