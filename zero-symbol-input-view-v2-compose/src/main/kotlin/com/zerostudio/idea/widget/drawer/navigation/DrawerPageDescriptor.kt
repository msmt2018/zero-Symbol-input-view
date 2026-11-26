/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.drawer.navigation

/**
 * Platform-neutral identity and display metadata for one drawer page.
 *
 * This type deliberately contains no Composable, Android, View, or lifecycle
 * references. Compose content is associated with the descriptor by the
 * presentation adapter, not by the navigation state engine.
 */
data class DrawerPageDescriptor(
    val id: String,
    val title: String
) {
    init {
        require(id.isNotBlank()) { "Page id must not be blank" }
        require(title.isNotBlank()) { "Page title must not be blank" }
    }
}

/** Immutable state published by the platform-neutral navigation engine. */
data class DrawerNavigationSnapshot(
    val selectedPageId: String?,
    val pageIds: List<String>
)
