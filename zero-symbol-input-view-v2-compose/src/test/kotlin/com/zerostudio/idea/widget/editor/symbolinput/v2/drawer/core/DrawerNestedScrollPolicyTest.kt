/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core

import org.junit.Assert.assertEquals
import org.junit.Test

class DrawerNestedScrollPolicyTest {
    private fun input(
        delta: Float,
        height: Int,
        childCanScrollBackward: Boolean = false,
        phase: DrawerNestedScrollPhase = DrawerNestedScrollPhase.PRE_SCROLL
    ) = DrawerNestedScrollInput(
        availableDeltaY = delta,
        currentHeightPx = height,
        minimumHeightPx = 100,
        maximumHeightPx = 900,
        childCanScrollBackward = childCanScrollBackward,
        phase = phase
    )

    @Test
    fun upwardMotionExpandsDrawerBeforeChild() {
        val result = DrawerNestedScrollPolicy.resolve(input(-240f, 400))
        assertEquals(-240f, result.consumedDeltaY, 0f)
        assertEquals(0f, result.remainingDeltaY, 0f)
    }

    @Test
    fun upwardMotionIsLeftToChildWhenDrawerIsExpanded() {
        val result = DrawerNestedScrollPolicy.resolve(input(-120f, 900))
        assertEquals(0f, result.consumedDeltaY, 0f)
        assertEquals(-120f, result.remainingDeltaY, 0f)
    }

    @Test
    fun downwardMotionIsLeftToScrollableChild() {
        val result = DrawerNestedScrollPolicy.resolve(
            input(delta = 180f, height = 700, childCanScrollBackward = true)
        )
        assertEquals(0f, result.consumedDeltaY, 0f)
        assertEquals(180f, result.remainingDeltaY, 0f)
    }

    @Test
    fun downwardMotionCollapsesDrawerAtChildStartBoundary() {
        val result = DrawerNestedScrollPolicy.resolve(input(180f, 700))
        assertEquals(180f, result.consumedDeltaY, 0f)
        assertEquals(0f, result.remainingDeltaY, 0f)
    }

    @Test
    fun drawerConsumesOnlyAvailableHeightAndReturnsRemainder() {
        val result = DrawerNestedScrollPolicy.resolve(input(delta = -300f, height = 800))
        assertEquals(-100f, result.consumedDeltaY, 0f)
        assertEquals(-200f, result.remainingDeltaY, 0f)
    }

    @Test
    fun zeroRangeDoesNotConsume() {
        val result = DrawerNestedScrollPolicy.resolve(
            DrawerNestedScrollInput(
                availableDeltaY = -50f,
                currentHeightPx = 100,
                minimumHeightPx = 100,
                maximumHeightPx = 100,
                childCanScrollBackward = false,
                phase = DrawerNestedScrollPhase.POST_SCROLL
            )
        )
        assertEquals(0f, result.consumedDeltaY, 0f)
        assertEquals(-50f, result.remainingDeltaY, 0f)
    }
}
