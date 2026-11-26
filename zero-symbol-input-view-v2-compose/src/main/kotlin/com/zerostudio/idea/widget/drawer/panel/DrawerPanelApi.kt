/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.drawer.panel

typealias DrawerPanelContent = com.zerostudio.idea.widget.drawer.panel.DrawerPanelContent
typealias DrawerPanelSlot = com.zerostudio.idea.widget.drawer.panel.DrawerPanelSlot

@androidx.compose.runtime.Composable
fun DrawerPanelHost(
    content: DrawerPanelContent?,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    slot: DrawerPanelSlot
) = com.zerostudio.idea.widget.drawer.panel.DrawerPanelHost(
    content = content,
    modifier = modifier,
    slot = slot
)
