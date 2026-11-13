package com.zerostudio.idea.widget.editor.symbolinput.v2

/**
 * Gesture arbitration for the symbol drawer. The existing height/anchor
 * geometry is intentionally untouched: this only decides whether the parent
 * may take a vertical gesture from its scrollable page content.
 */
internal object SymbolDrawerDragPolicy {
    fun shouldInterceptVerticalDrag(
        fingerDeltaY: Float,
        currentHeightPx: Int,
        collapsedHeightPx: Int,
        expandedHeightPx: Int,
        childCanScrollUp: Boolean
    ): Boolean {
        if (expandedHeightPx <= collapsedHeightPx || fingerDeltaY == 0f) return false
        return if (fingerDeltaY < 0f) {
            currentHeightPx < expandedHeightPx
        } else {
            currentHeightPx > collapsedHeightPx && !childCanScrollUp
        }
    }
}
