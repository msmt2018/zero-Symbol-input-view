/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.drawer.navigation


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
