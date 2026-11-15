package com.zerostudio.idea.widget.editor.symbolinput.v2

import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core.DrawerInteractionController

/**
 * @deprecated Gesture arbitration is now owned by the feature-neutral drawer
 * core. Kept as a source-compatible bridge for existing tests and integrations.
 */
@Deprecated("Use DrawerInteractionController.shouldInterceptVerticalDrag")
internal object SymbolDrawerDragPolicy {
    fun shouldInterceptVerticalDrag(
        fingerDeltaY: Float,
        currentHeightPx: Int,
        collapsedHeightPx: Int,
        expandedHeightPx: Int,
        childCanScrollUp: Boolean
    ): Boolean = DrawerInteractionController.shouldInterceptVerticalDrag(
        fingerDeltaY = fingerDeltaY,
        currentHeightPx = currentHeightPx,
        collapsedHeightPx = collapsedHeightPx,
        expandedHeightPx = expandedHeightPx,
        childCanScrollUp = childCanScrollUp
    )
}
