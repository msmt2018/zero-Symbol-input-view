package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation

import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.panel.DrawerPanelContent

/** Generic page contract; no symbol-group model is involved. */
data class DrawerPage(
    val id: String,
    val title: CharSequence,
    val content: DrawerPanelContent
) {
    init {
        require(id.isNotBlank()) { "Page id must not be blank" }
        require(title.isNotEmpty()) { "Page title must not be empty" }
    }
}

data class DrawerNavigationSnapshot(
    val selectedPageId: String?,
    val pageIds: List<String>
)
