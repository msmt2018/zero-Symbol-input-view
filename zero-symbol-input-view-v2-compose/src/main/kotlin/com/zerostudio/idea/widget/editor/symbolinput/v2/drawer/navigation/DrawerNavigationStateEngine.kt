/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation

/**
 * Pure Kotlin page identity and selection engine.
 *
 * The engine owns only immutable page descriptors and the selected page ID.
 * It has no Compose Runtime, Android UI, View, callback, or lifecycle
 * dependency. It is single-thread-confined; callers serialize access.
 */
class DrawerNavigationStateEngine {
    private var pageDescriptors: List<DrawerPageDescriptor> = emptyList()
    private var selectedPageId: String? = null

    fun submitPages(
        pages: List<DrawerPageDescriptor>,
        preserveSelection: Boolean = true
    ) {
        require(pages.map { it.id }.distinct().size == pages.size) {
            "Drawer page IDs must be unique"
        }
        val nextPages = pages.toList()
        val previousSelection = selectedPageId
        pageDescriptors = nextPages
        selectedPageId = if (
            preserveSelection &&
            previousSelection != null &&
            nextPages.any { it.id == previousSelection }
        ) {
            previousSelection
        } else {
            nextPages.firstOrNull()?.id
        }
    }

    fun selectPage(id: String): Boolean {
        if (pageDescriptors.none { it.id == id }) return false
        selectedPageId = id
        return true
    }

    fun selectPage(index: Int): Boolean {
        val page = pageDescriptors.getOrNull(index) ?: return false
        selectedPageId = page.id
        return true
    }

    fun pages(): List<DrawerPageDescriptor> = pageDescriptors.toList()

    fun selectedPage(): DrawerPageDescriptor? =
        pageDescriptors.firstOrNull { it.id == selectedPageId }

    fun selectedIndex(): Int = pageDescriptors.indexOfFirst { it.id == selectedPageId }

    fun snapshot(): DrawerNavigationSnapshot = DrawerNavigationSnapshot(
        selectedPageId = selectedPageId,
        pageIds = java.util.Collections.unmodifiableList(pageDescriptors.map { it.id })
    )
}
