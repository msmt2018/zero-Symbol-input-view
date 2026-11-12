package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core

import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.config.DrawerConfig
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerNavigationConfig
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.panel.DrawerPanelContent
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerPage
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerColors
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerDimensions
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerMotion
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerShape
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerStyle
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerThemeMode
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerTypography

/**
 * Public runtime control surface. A concrete DrawerContainer will bind this contract
 * to Android measurement, nested scrolling, touch dispatch, page hosting and animation.
 */
interface DrawerController {
    fun expand(animated: Boolean = true)
    fun collapse(animated: Boolean = true)
    fun moveToAnchor(anchorId: String, animated: Boolean = true)

    /** Replace the active strategy and recalculate anchors against current measurements. */
    fun setAnchorStrategy(strategy: DrawerAnchorStrategy)

    /** Apply a complete immutable configuration atomically. */
    fun applyConfig(config: DrawerConfig)

    /** Runtime visual/style setters. Null color roles continue to inherit host theme values. */
    fun setStyle(style: DrawerStyle)
    fun setThemeMode(mode: DrawerThemeMode)
    fun setColors(colors: DrawerColors)
    fun setDimensions(dimensions: DrawerDimensions)
    fun setTypography(typography: DrawerTypography)
    fun setMotion(motion: DrawerMotion)
    fun setShape(shape: DrawerShape)

    /** Independently replace either panel; passing null removes that panel. */
    fun setPeekContent(content: DrawerPanelContent?)
    fun setExpandedContent(content: DrawerPanelContent?)

    /** Generic page navigation appearance; the navigation widget itself is optional. */
    fun setTitle(title: CharSequence?)
    fun setPages(pages: List<DrawerPage>, preserveSelection: Boolean = true)
    fun selectPage(pageId: String): Boolean
    fun setNavigationConfig(config: DrawerNavigationConfig)

    fun setDragEnabled(enabled: Boolean)
    fun setNestedScrollEnabled(enabled: Boolean)
    fun setEdgeToEdgeEnabled(enabled: Boolean)
    fun setDismissOnScrimTap(enabled: Boolean)
    fun setDismissOnBack(enabled: Boolean)
    fun setDragHandleEnabled(enabled: Boolean)
    fun setAccessibilityEnabled(enabled: Boolean)

    fun currentState(): DrawerState?
    fun addStateListener(listener: (DrawerState) -> Unit)
    fun removeStateListener(listener: (DrawerState) -> Unit)
}
