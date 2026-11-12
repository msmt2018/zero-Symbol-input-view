package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core

/**
 * Resolves a complete anchor set from measured layout inputs.
 *
 * Implementations must be deterministic and must not read Android Views directly.
 * This makes anchor calculation testable and lets applications supply custom policies.
 */
interface DrawerAnchorStrategy {
    val id: String
    fun resolve(input: DrawerAnchorInput): List<DrawerAnchor>
}

/**
 * Compatibility strategy matching the current V2 two-position height model:
 * the existing collapsed height and the existing expanded height.
 */
class ExistingHeightAnchorStrategy : DrawerAnchorStrategy {
    override val id: String = ID

    override fun resolve(input: DrawerAnchorInput): List<DrawerAnchor> = listOf(
        DrawerAnchor("peek", input.minHeightPx, DrawerAnchorState.PEEK),
        DrawerAnchor(
            "expanded",
            input.existingExpandedHeightPx.coerceIn(input.minHeightPx, input.maxHeightPx),
            DrawerAnchorState.EXPANDED
        )
    )

    companion object {
        const val ID = "existing-height"
    }
}

/** Independent three-stage policy: Peek, Half-Expanded and Expanded. */
data class ThreeStageAnchorStrategy(
    val halfExpandedFraction: Float = 0.5f,
    val expandedViewportFraction: Float = 0.92f
) : DrawerAnchorStrategy {
    init {
        require(halfExpandedFraction in 0.1f..0.9f)
        require(expandedViewportFraction in 0.2f..1f)
        require(halfExpandedFraction < expandedViewportFraction)
    }

    override val id: String = ID

    override fun resolve(input: DrawerAnchorInput): List<DrawerAnchor> {
        val peek = input.minHeightPx
        val expanded = minOf(
            input.maxHeightPx,
            (input.viewportHeightPx * expandedViewportFraction).toInt()
        ).coerceAtLeast(peek)
        val half = (input.viewportHeightPx * halfExpandedFraction).toInt()
            .coerceIn(peek, expanded)
        return listOf(
            DrawerAnchor("peek", peek, DrawerAnchorState.PEEK),
            DrawerAnchor("half", half, DrawerAnchorState.HALF_EXPANDED),
            DrawerAnchor("expanded", expanded, DrawerAnchorState.EXPANDED)
        )
    }

    companion object {
        const val ID = "three-stage"
    }
}

/**
 * Content-sized policy. The second anchor follows measured content height,
 * clamped to the available viewport and configured maximum.
 */
class ContentBasedAnchorStrategy : DrawerAnchorStrategy {
    override val id: String = ID

    override fun resolve(input: DrawerAnchorInput): List<DrawerAnchor> = listOf(
        DrawerAnchor("peek", input.minHeightPx, DrawerAnchorState.PEEK),
        DrawerAnchor(
            "content",
            input.contentHeightPx.coerceIn(input.minHeightPx, input.maxHeightPx),
            DrawerAnchorState.EXPANDED
        )
    )

    companion object {
        const val ID = "content-based"
    }
}

/** Application-defined anchor heights in pixels. Heights are clamped by the resolver. */
data class CustomAnchorStrategy(
    val anchors: List<DrawerAnchor>
) : DrawerAnchorStrategy {
    init {
        require(anchors.isNotEmpty()) { "At least one custom anchor is required" }
        require(anchors.map { it.id }.distinct().size == anchors.size) {
            "Custom anchor IDs must be unique"
        }
    }

    override val id: String = ID
    override fun resolve(input: DrawerAnchorInput): List<DrawerAnchor> = anchors

    companion object {
        const val ID = "custom"
    }
}

/** Named factories make the four supported policies explicit at call sites. */
object DrawerAnchorStrategies {
    @JvmStatic fun existingHeight(): DrawerAnchorStrategy = ExistingHeightAnchorStrategy()
    @JvmStatic fun threeStage(
        halfExpandedFraction: Float = 0.5f,
        expandedViewportFraction: Float = 0.92f
    ): DrawerAnchorStrategy = ThreeStageAnchorStrategy(halfExpandedFraction, expandedViewportFraction)
    @JvmStatic fun contentBased(): DrawerAnchorStrategy = ContentBasedAnchorStrategy()
    @JvmStatic fun custom(anchors: List<DrawerAnchor>): DrawerAnchorStrategy = CustomAnchorStrategy(anchors)
}
