/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerNestedScrollConnectionTest {

    private fun holder(): DrawerStateHolder {
        val input = DrawerAnchorInput(
            minHeightPx = 80,
            maxHeightPx = 800,
            viewportHeightPx = 800,
            contentHeightPx = 600,
            viewportWidthPx = 400
        )
        return DrawerStateHolder(
            initialInput = input,
            initialStrategy = ThreeStageAnchorStrategy(useAutomaticPeekHeight = false),
            initialAnchorId = "half"
        )
    }

    @Test
    fun cancelledNestedGestureCannotClaimLaterDirectPointerFling() = runBlocking {
        val holder = holder()
        val connection = DrawerNestedScrollConnection(holder)

        // Start a nested-scroll gesture that this connection actually owns.
        val consumed = connection.onPreScroll(
            available = Offset(x = 0f, y = -20f),
            source = NestedScrollSource.UserInput
        )
        assertTrue("Drawer should consume upward pre-scroll", consumed.y < 0f)

        // The host reports cancellation before onPostFling can be dispatched.
        connection.cancelGesture()
        holder.cancelDrag()

        // A later direct pointer gesture uses the same holder but is not owned
        // by this nested-scroll connection.
        holder.beginDrag()
        holder.dragBy(-25f)
        connection.onPostFling(
            consumed = Velocity.Zero,
            available = Velocity.Zero
        )

        assertTrue("Unrelated direct drag must remain active", holder.isDragging)
    }

    @Test
    fun sideEffectScrollDoesNotMoveDrawerOrAcquireGestureOwnership() = runBlocking {
        val holder = holder()
        val connection = DrawerNestedScrollConnection(holder)
        val initialHeight = holder.currentHeightPx

        val preConsumed = connection.onPreScroll(
            available = Offset(x = 0f, y = -120f),
            source = NestedScrollSource.SideEffect
        )
        val postConsumed = connection.onPostScroll(
            consumed = Offset.Zero,
            available = Offset(x = 0f, y = 120f),
            source = NestedScrollSource.SideEffect
        )

        assertTrue(preConsumed == Offset.Zero)
        assertTrue(postConsumed == Offset.Zero)
        assertTrue(holder.currentHeightPx == initialHeight)
        assertTrue(!holder.isDragging)

        connection.onPostFling(
            consumed = Velocity.Zero,
            available = Velocity(y = -2_000f, x = 0f)
        )
        assertTrue("Side-effect scrolling must not settle the drawer", !holder.isDragging)
        assertTrue(holder.currentHeightPx == initialHeight)
    }

    @Test
    fun unrelatedNestedFlingDoesNotFinishDirectPointerDrag() = runBlocking {
        val holder = holder()
        val connection = DrawerNestedScrollConnection(holder)

        // A direct pointer pipeline can put the shared holder into dragging
        // state without this NestedScrollConnection consuming any scroll.
        holder.beginDrag()
        holder.dragBy(-25f)

        connection.onPostFling(
            consumed = Velocity.Zero,
            available = Velocity.Zero
        )

        assertTrue("Direct drag ownership must remain with its pointer host", holder.isDragging)
    }
}
