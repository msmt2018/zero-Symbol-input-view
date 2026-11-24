/*
 * This file is part of zero-Symbol-input-view-V2.
 *
 * Copyright (C) 2026 android_zero
 *
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 *
 * License Information:
 *
 * This file is distributed under the following licenses, which apply
 * cumulatively to the same work:
 *
 * 1. GNU Affero General Public License v3.0 only (AGPL-3.0-only):
 *    Grants the rights to use, study, modify, and redistribute this work.
 *    When a modified version is used to provide network services to users,
 *    the corresponding source code must be made available under the
 *    applicable AGPL terms.
 *
 * 2. GNU General Public License v3.0 only (GPL-3.0-only):
 *    Grants the rights to use, study, modify, and redistribute this work.
 *    Distribution of covered binaries requires compliance with the source
 *    code, license notice, attribution, and other applicable GPL terms.
 *
 * 3. Apache License 2.0 (Apache-2.0):
 *    Grants copyright and applicable patent rights to use, modify, and
 *    distribute this work, subject to its license conditions, including
 *    preservation of notices, inclusion of the license, and marking changes.
 *
 * The SPDX operator "AND" means that all listed license terms apply
 * cumulatively. It does not mean that recipients may choose only one
 * of the listed licenses.
 *
 * You must comply with the applicable terms of all three licenses.
 * This notice is a summary and does not replace the full license texts.
 *
 * Full license texts:
 * - GNU AGPL v3.0: https://www.gnu.org/licenses/agpl-3.0.html
 * - GNU GPL v3.0:  https://www.gnu.org/licenses/gpl-3.0.html
 * - Apache 2.0:    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Author: android_zero
 * GitHub: https://github.com/msmt2018
 * WeChat: android_zero
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation


/**
 * Page identity and selection model. It owns no Compose Runtime state, View,
 * adapter or lifecycle object; DrawerPage values carry the Composable content
 * slots that the Compose page host renders.
 *
 * Threading contract: this controller is single-thread-confined, not
 * thread-safe. Callers must serialize every operation (including reads,
 * listener registration and listener removal) on one thread. Listener
 * callbacks run synchronously on the thread that publishes the change; the
 * initial callback from [addListener] runs on the registering thread.
 * Compose hosts must perform controller operations on their UI/composition
 * dispatcher so synchronous notifications can safely update composition
 * state. This class does not dispatch to, or retain, a thread or dispatcher.
 */
class DrawerNavigationController {
    fun interface Listener {
        fun onNavigationChanged(snapshot: DrawerNavigationSnapshot, selectedIndex: Int)
    }

    private var pages: List<DrawerPage> = emptyList()
    private var selectedId: String? = null
    private val listeners = LinkedHashSet<Listener>()
    private val pendingNotifications = java.util.ArrayDeque<Pair<DrawerNavigationSnapshot, Int>>()
    private var dispatchingNotifications = false

    /** Latest immutable navigation snapshot. Compose callers should use rememberDrawerNavigationSnapshot. */
    val state: DrawerNavigationSnapshot get() = snapshot()

    fun submitPages(value: List<DrawerPage>, preserveSelection: Boolean = true) {
        require(value.map { it.id }.distinct().size == value.size) {
            "Drawer page IDs must be unique"
        }
        val previous = selectedId
        pages = value.toList()
        selectedId = if (preserveSelection && previous != null && pages.any { it.id == previous }) {
            previous
        } else {
            pages.firstOrNull()?.id
        }
        dispatch()
    }

    fun selectPage(id: String): Boolean {
        if (pages.none { it.id == id }) return false
        if (selectedId == id) return true
        selectedId = id
        dispatch()
        return true
    }

    fun selectPage(index: Int): Boolean {
        val page = pages.getOrNull(index) ?: return false
        return selectPage(page.id)
    }

    fun pages(): List<DrawerPage> = pages.toList()
    fun selectedPage(): DrawerPage? = pages.firstOrNull { it.id == selectedId }
    fun selectedIndex(): Int = pages.indexOfFirst { it.id == selectedId }
    fun snapshot() = DrawerNavigationSnapshot(
        selectedPageId = selectedId,
        pageIds = java.util.Collections.unmodifiableList(pages.map { it.id })
    )

    fun addListener(listener: Listener) {
        if (!listeners.add(listener)) return
        try {
            listener.onNavigationChanged(snapshot(), selectedIndex())
        } catch (failure: RuntimeException) {
            // Roll back a subscription that cannot accept its initial state.
            listeners.remove(listener)
            throw failure
        }
    }

    fun removeListener(listener: Listener) {
        listeners -= listener
    }

    private fun dispatch() {
        val nextSnapshot = snapshot()
        pendingNotifications.addLast(nextSnapshot to selectedIndex())
        if (dispatchingNotifications) return

        // Reentrant listeners may synchronously select another page. Queue that
        // publication so every observer sees the current snapshot before any
        // observer receives the newer one.
        dispatchingNotifications = true
        var dispatchFailure: RuntimeException? = null
        try {
            while (pendingNotifications.isNotEmpty()) {
                val (publishedSnapshot, publishedIndex) = pendingNotifications.removeFirst()
                listeners.toList().forEach { listener ->
                    try {
                        listener.onNavigationChanged(publishedSnapshot, publishedIndex)
                    } catch (failure: RuntimeException) {
                        val firstFailure = dispatchFailure
                        if (firstFailure == null) dispatchFailure = failure
                        else firstFailure.addSuppressed(failure)
                    }
                }
            }
        } finally {
            dispatchingNotifications = false
        }
        // Report callback failures only after all observers have received all
        // queued snapshots; a failure cannot strand a reentrant publication.
        dispatchFailure?.let { throw it }
    }
}
