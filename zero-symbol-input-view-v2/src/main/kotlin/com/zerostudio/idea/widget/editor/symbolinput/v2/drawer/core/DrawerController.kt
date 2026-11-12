package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core

/**
 * Public control surface. A concrete container binds this contract to Android
 * measurement, nested scrolling, touch dispatch and animation.
 */
interface DrawerController {
    fun expand(animated: Boolean = true)
    fun collapse(animated: Boolean = true)
    fun moveToAnchor(anchorId: String, animated: Boolean = true)
    fun setAnchorStrategy(strategy: DrawerAnchorStrategy)
    fun currentState(): DrawerState?
    fun addStateListener(listener: (DrawerState) -> Unit)
    fun removeStateListener(listener: (DrawerState) -> Unit)
}
