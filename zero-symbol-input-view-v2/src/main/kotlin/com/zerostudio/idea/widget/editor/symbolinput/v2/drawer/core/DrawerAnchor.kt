package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core

/** Semantic identity of a resolved drawer anchor. */
enum class DrawerAnchorState {
    PEEK,
    HALF_EXPANDED,
    EXPANDED,
    CUSTOM
}

/** A stable, pixel-resolved vertical position for the drawer's top edge. */
data class DrawerAnchor(
    val id: String,
    val heightPx: Int,
    val state: DrawerAnchorState
) {
    init {
        require(id.isNotBlank()) { "Anchor id must not be blank" }
        require(heightPx >= 0) { "Anchor height cannot be negative" }
    }
}

/** Inputs are supplied by the host layout engine; strategies remain UI-independent. */
data class DrawerAnchorInput(
    val minHeightPx: Int,
    val maxHeightPx: Int,
    val viewportHeightPx: Int,
    val contentHeightPx: Int,
    val existingExpandedHeightPx: Int = maxHeightPx,
    val density: Float = 1f
) {
    init {
        require(minHeightPx >= 0)
        require(maxHeightPx >= minHeightPx)
        require(viewportHeightPx >= 0)
        require(contentHeightPx >= 0)
        require(existingExpandedHeightPx >= 0)
        require(density > 0f)
    }
}
