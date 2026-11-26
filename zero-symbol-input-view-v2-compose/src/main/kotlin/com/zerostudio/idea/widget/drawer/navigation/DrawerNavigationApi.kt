/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.drawer.navigation

typealias DrawerNavigationPlacement = com.zerostudio.idea.widget.drawer.navigation.DrawerNavigationPlacement
typealias DrawerIndicatorShape = com.zerostudio.idea.widget.drawer.navigation.DrawerIndicatorShape
typealias DrawerIndicatorStyle = com.zerostudio.idea.widget.drawer.navigation.DrawerIndicatorStyle
typealias DrawerNavigationConfig = com.zerostudio.idea.widget.drawer.navigation.DrawerNavigationConfig
typealias DrawerNavigationController = com.zerostudio.idea.widget.drawer.navigation.DrawerNavigationController
typealias DrawerPage = com.zerostudio.idea.widget.drawer.navigation.DrawerPage
typealias DrawerNavigationSnapshot = com.zerostudio.idea.widget.drawer.navigation.DrawerNavigationSnapshot

@androidx.compose.runtime.Composable
fun DrawerPageHost(
    pages: List<DrawerPage>,
    selectedPageId: String?,
    onPageSelected: (DrawerPage) -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) = com.zerostudio.idea.widget.drawer.navigation.DrawerPageHost(
    pages = pages,
    selectedPageId = selectedPageId,
    onPageSelected = onPageSelected,
    modifier = modifier
)


/** Subscribes the current Composition to page selection updates. */
@androidx.compose.runtime.Composable
fun rememberDrawerNavigationSnapshot(
    controller: DrawerNavigationController
): DrawerNavigationSnapshot =
    com.zerostudio.idea.widget.drawer.navigation.rememberDrawerNavigationSnapshot(controller)
