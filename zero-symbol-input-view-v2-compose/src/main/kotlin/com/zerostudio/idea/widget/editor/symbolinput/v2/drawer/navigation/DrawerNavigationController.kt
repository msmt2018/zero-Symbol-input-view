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

import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core.SynchronousSnapshotPublisher


/**
 * Compatibility-facing page-content adapter and synchronous observer facade.
 * Page identity and selection are delegated to [DrawerNavigationStateEngine],
 * which is independent of Compose. This facade retains content slots only to
 * preserve the existing DrawerPage API; new state-only consumers should use
 * the engine directly.
 *
 * Threading contract: single-thread-confined and not thread-safe. Callers must
 * serialize all operations. Listener callbacks run synchronously on the
 * publishing thread; no dispatcher or lifecycle owner is retained.
 */
class DrawerNavigationController {
    fun interface Listener {
        fun onNavigationChanged(snapshot: DrawerNavigationSnapshot, selectedIndex: Int)
    }

    private val navigation = DrawerNavigationStateEngine()
    private var pageContentById: Map<String, DrawerPage> = emptyMap()
    private val publisher = SynchronousSnapshotPublisher<Pair<DrawerNavigationSnapshot, Int>>()
    private val listenerAdapters =
        LinkedHashMap<Listener, (Pair<DrawerNavigationSnapshot, Int>) -> Unit>()

    /** Latest immutable navigation snapshot. Compose callers should use rememberDrawerNavigationSnapshot. */
    val state: DrawerNavigationSnapshot get() = snapshot()

    fun submitPages(value: List<DrawerPage>, preserveSelection: Boolean = true) {
        require(value.map { it.id }.distinct().size == value.size) {
            "Drawer page IDs must be unique"
        }
        val nextPages = value.toList()
        val nextContent = nextPages.associateBy { it.id }
        pageContentById = nextContent
        navigation.submitPages(nextPages.map { DrawerPageDescriptor(it.id, it.title) }, preserveSelection)
        dispatch()
    }

    fun selectPage(id: String): Boolean {
        val previousSelection = navigation.snapshot().selectedPageId
        if (!navigation.selectPage(id)) return false
        if (navigation.snapshot().selectedPageId != previousSelection) dispatch()
        return true
    }

    fun selectPage(index: Int): Boolean {
        val descriptor = navigation.pages().getOrNull(index) ?: return false
        return selectPage(descriptor.id)
    }

    fun pages(): List<DrawerPage> =
        navigation.pages().mapNotNull { descriptor -> pageContentById[descriptor.id] }
    fun selectedPage(): DrawerPage? = navigation.snapshot().selectedPageId?.let(pageContentById::get)
    fun selectedIndex(): Int = navigation.selectedIndex()
    fun snapshot(): DrawerNavigationSnapshot = navigation.snapshot()

    fun addListener(listener: Listener) {
        val adapter = listenerAdapters.getOrPut(listener) { publication ->
            listener.onNavigationChanged(publication.first, publication.second)
        }
        try {
            publisher.addListener(adapter, currentPublication())
        } catch (failure: RuntimeException) {
            // Roll back both adapter and publisher registration if the initial
            // snapshot cannot be accepted by this observer.
            listenerAdapters.remove(listener)
            throw failure
        }
    }

    fun removeListener(listener: Listener) {
        listenerAdapters.remove(listener)?.let(publisher::removeListener)
    }

    private fun currentPublication(): Pair<DrawerNavigationSnapshot, Int> =
        snapshot() to navigation.selectedIndex()

    private fun dispatch() {
        publisher.publish(currentPublication())
    }
}
