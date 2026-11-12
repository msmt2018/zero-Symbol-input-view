package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core

/**
 * Normalizes strategy output into an ordered, bounded anchor list.
 * Duplicate pixel positions are collapsed deterministically, preferring the
 * more expanded semantic state while retaining a stable id.
 */
object DrawerAnchorResolver {
    fun resolve(
        strategy: DrawerAnchorStrategy,
        input: DrawerAnchorInput
    ): List<DrawerAnchor> {
        val resolved = strategy.resolve(input)
        require(resolved.isNotEmpty()) { "Anchor strategy returned no anchors" }

        val normalized = resolved.map {
            it.copy(heightPx = it.heightPx.coerceIn(input.minHeightPx, input.maxHeightPx))
        }.sortedWith(compareBy<DrawerAnchor> { it.heightPx }.thenBy { rank(it.state) })

        val unique = LinkedHashMap<Int, DrawerAnchor>()
        normalized.forEach { candidate ->
            val current = unique[candidate.heightPx]
            if (current == null || rank(candidate.state) >= rank(current.state)) {
                unique[candidate.heightPx] = candidate
            }
        }
        return unique.values.toList()
    }

    fun nearestAnchor(anchors: List<DrawerAnchor>, heightPx: Int): DrawerAnchor {
        require(anchors.isNotEmpty()) { "Cannot select from an empty anchor list" }
        return anchors.minBy { kotlin.math.abs(it.heightPx - heightPx) }
    }

    /** Resolve a fling target without allowing a single gesture to skip every stage. */
    fun flingTarget(
        anchors: List<DrawerAnchor>,
        currentHeightPx: Int,
        velocityY: Float,
        minimumFlingVelocity: Float
    ): DrawerAnchor {
        require(anchors.isNotEmpty()) { "Cannot select from an empty anchor list" }
        val ordered = anchors.sortedBy { it.heightPx }
        val currentIndex = ordered.indices.minBy { kotlin.math.abs(ordered[it].heightPx - currentHeightPx) }
        if (kotlin.math.abs(velocityY) < minimumFlingVelocity) {
            return nearestAnchor(ordered, currentHeightPx)
        }
        val nextIndex = (currentIndex + if (velocityY > 0f) 1 else -1)
            .coerceIn(ordered.indices)
        return ordered[nextIndex]
    }

    private fun rank(state: DrawerAnchorState): Int = when (state) {
        DrawerAnchorState.PEEK -> 0
        DrawerAnchorState.CUSTOM -> 1
        DrawerAnchorState.HALF_EXPANDED -> 2
        DrawerAnchorState.EXPANDED -> 3
    }
}
