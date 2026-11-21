package com.zerostudio.idea.widget.drawer.panel

typealias DrawerPanelContent = com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.panel.DrawerPanelContent
typealias DrawerPanelSlot = com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.panel.DrawerPanelSlot

@androidx.compose.runtime.Composable
fun DrawerPanelHost(
    content: DrawerPanelContent?,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    slot: DrawerPanelSlot
) = com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.panel.DrawerPanelHost(
    content = content,
    modifier = modifier,
    slot = slot
)
