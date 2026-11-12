package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core

/** Runtime snapshot shared by the container and application-facing controller. */
data class DrawerState(
    val currentAnchorId: String,
    val currentHeightPx: Int,
    val anchors: List<DrawerAnchor>,
    val isDragging: Boolean = false,
    val isAnimating: Boolean = false
) {
    init {
        require(currentAnchorId.isNotBlank())
        require(currentHeightPx >= 0)
        require(anchors.isNotEmpty())
    }

    val currentAnchor: DrawerAnchor?
        get() = anchors.firstOrNull { it.id == currentAnchorId }
}
