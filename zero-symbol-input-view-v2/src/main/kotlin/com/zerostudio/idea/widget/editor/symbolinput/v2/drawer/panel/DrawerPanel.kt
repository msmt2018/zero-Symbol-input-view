package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.panel

import android.content.Context
import android.view.View

/** Optional lifecycle contract for content hosted by a drawer panel. */
interface DrawerPanelContent {
    fun createView(): View
    /** Theme-aware creation hook; legacy implementations remain source-compatible. */
    fun createView(context: Context): View = createView()
    fun onPanelAttached() = Unit
    fun onPanelVisible() = Unit
    fun onPanelHidden() = Unit
    fun onPanelDetached() = Unit
}

/** Distinguishes the always-available compact surface from expanded content. */
enum class DrawerPanelSlot {
    PEEK,
    EXPANDED
}
