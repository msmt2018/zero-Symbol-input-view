/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core

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
            viewportWidth = 400
        )
        return DrawerStateHolder(
            initialInput = input,
            initialStrategy = ThreeStageAnchorStrategy(useAutomaticPeekHeight = false),
            initialAnchorId = "half"
        )
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
