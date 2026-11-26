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

package com.zerostudio.idea.widget.drawer.navigation

import com.zerostudio.idea.widget.drawer.panel.DrawerPanelContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerNavigationControllerTest {
    private fun page(id: String) = DrawerPage(id, id, DrawerPanelContent { })

    @Test fun composeStatePublishesImmutableNavigationSnapshots() {
        val controller = DrawerNavigationController()
        assertEquals(null, controller.state.selectedPageId)
        controller.submitPages(listOf(page("first"), page("second")))
        assertEquals(listOf("first", "second"), controller.state.pageIds)
        assertEquals("first", controller.state.selectedPageId)
        controller.selectPage("second")
        assertEquals("second", controller.state.selectedPageId)
    }

    @Test
    fun publishedSnapshotPageIdsCannotBeMutatedByConsumers() {
        val controller = DrawerNavigationController()
        controller.submitPages(listOf(page("first"), page("second")))
        val published = controller.state

        assertThrows(UnsupportedOperationException::class.java) {
            (published.pageIds as MutableList<String>).clear()
        }
        assertEquals(listOf("first", "second"), controller.state.pageIds)
    }

    @Test fun selectsPagesAndPreservesSelectionAcrossUpdates() {
        val controller = DrawerNavigationController()
        controller.submitPages(listOf(page("one"), page("two")))
        assertEquals("one", controller.selectedPage()?.id)
        assertTrue(controller.selectPage("two"))
        controller.submitPages(listOf(page("two"), page("three")))
        assertEquals("two", controller.selectedPage()?.id)
    }

    @Test fun rejectsUnknownPageAndInvalidIndex() {
        val controller = DrawerNavigationController()
        controller.submitPages(listOf(page("one")))
        assertFalse(controller.selectPage("missing"))
        assertFalse(controller.selectPage(4))
        assertEquals(0, controller.selectedIndex())
    }

    @Test fun observersReceiveInitialAndUpdatedSnapshots() {
        val controller = DrawerNavigationController()
        val selected = mutableListOf<String?>()
        val listener = DrawerNavigationController.Listener { snapshot, _ ->
            selected += snapshot.selectedPageId
        }
        controller.addListener(listener)
        controller.submitPages(listOf(page("a"), page("b")))
        controller.selectPage("b")
        controller.removeListener(listener)
        assertEquals(listOf(null, "a", "b"), selected)
    }

    @Test
    fun listenerCallbacksRunSynchronouslyOnTheCallingThread() {
        val controller = DrawerNavigationController()
        val caller = Thread.currentThread()
        val callbackThreads = mutableListOf<Thread>()
        val listener = DrawerNavigationController.Listener { _, _ ->
            callbackThreads += Thread.currentThread()
        }

        controller.addListener(listener)
        controller.submitPages(listOf(page("a")))

        assertEquals(listOf(caller, caller), callbackThreads)
    }

    @Test
    fun addingSameNavigationListenerTwiceDoesNotDuplicateSubscription() {
        val controller = DrawerNavigationController()
        var calls = 0
        val listener = DrawerNavigationController.Listener { _, _ -> calls++ }

        controller.addListener(listener)
        controller.addListener(listener)
        controller.submitPages(listOf(page("a")))

        assertEquals(2, calls)
    }

    @Test
    fun distinctListenersThatCompareEqualRemainIndependentSubscriptions() {
        class EqualListener(
            private val onChanged: () -> Unit
        ) : DrawerNavigationController.Listener {
            override fun onNavigationChanged(
                snapshot: DrawerNavigationSnapshot,
                selectedIndex: Int
            ) = onChanged()

            override fun equals(other: Any?) = other is EqualListener
            override fun hashCode() = 0
        }

        val controller = DrawerNavigationController()
        var firstCalls = 0
        var secondCalls = 0
        val first = EqualListener { firstCalls++ }
        val second = EqualListener { secondCalls++ }

        controller.addListener(first)
        controller.addListener(second)
        assertEquals(1, firstCalls)
        assertEquals(1, secondCalls)

        controller.submitPages(listOf(page("first"), page("second")))
        assertEquals(2, firstCalls)
        assertEquals(2, secondCalls)

        controller.selectPage("second")
        assertEquals(3, firstCalls)
        assertEquals(3, secondCalls)

        controller.removeListener(first)
        controller.selectPage("first")
        assertEquals(3, firstCalls)
        assertEquals(4, secondCalls)
    }

    @Test
    fun listenerRejectingInitialNavigationSnapshotIsNotRegistered() {
        val controller = DrawerNavigationController()
        var calls = 0
        val listener = DrawerNavigationController.Listener { _, _ ->
            calls++
            throw IllegalStateException("initial snapshot rejected")
        }

        try {
            controller.addListener(listener)
            throw AssertionError("Expected initial listener failure")
        } catch (expected: IllegalStateException) {
            assertEquals("initial snapshot rejected", expected.message)
        }

        controller.submitPages(listOf(page("a")))
        assertEquals(1, calls)
    }

    @Test
    fun failingObserverDoesNotBlockOtherNavigationObservers() {
        val controller = DrawerNavigationController()
        val observed = mutableListOf<String?>()
        controller.addListener { snapshot, _ ->
            if (snapshot.selectedPageId == "a") {
                throw IllegalStateException("observer failure")
            }
        }
        controller.addListener { snapshot, _ ->
            if (snapshot.selectedPageId != null) observed += snapshot.selectedPageId
        }

        try {
            controller.submitPages(listOf(page("a"), page("b")))
            throw AssertionError("Expected observer failure to be reported")
        } catch (expected: IllegalStateException) {
            assertEquals("observer failure", expected.message)
        }

        assertEquals(listOf("a"), observed)
        assertEquals("a", controller.selectedPage()?.id)
    }

    @Test
    fun reentrantSelectionIsDeliveredAfterCurrentSnapshotToEveryObserver() {
        val controller = DrawerNavigationController()
        val firstObserver = mutableListOf<String?>()
        val secondObserver = mutableListOf<String?>()
        var reentered = false

        controller.addListener { snapshot, _ ->
            firstObserver += snapshot.selectedPageId
            if (snapshot.selectedPageId == "first" && !reentered) {
                reentered = true
                assertTrue(controller.selectPage("second"))
            }
        }
        controller.addListener { snapshot, _ ->
            secondObserver += snapshot.selectedPageId
        }

        controller.submitPages(listOf(page("first"), page("second")))

        assertEquals(listOf(null, "first", "second"), firstObserver)
        assertEquals(listOf(null, "first", "second"), secondObserver)
        assertEquals("second", controller.state.selectedPageId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDuplicatePageIds() {
        DrawerNavigationController().submitPages(listOf(page("same"), page("same")))
    }
}
