/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
            input(
                delta = 180f,
                height = 700,
                childCanScrollBackward = true,
                phase = DrawerNestedScrollPhase.PRE_SCROLL
            )
        )
        assertEquals(0f, result.consumedDeltaY, 0f)
        assertEquals(180f, result.remainingDeltaY, 0f)
    }

    @Test
    fun downwardMotionCollapsesDrawerAtChildStartBoundary() {
        val result = DrawerNestedScrollPolicy.resolve(
            input(180f, 700, phase = DrawerNestedScrollPhase.POST_SCROLL)
        )
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
    fun downwardMotionIsNotConsumedDuringPreScrollEvenAtChildStart() {
        val result = DrawerNestedScrollPolicy.resolve(
            input(
                delta = 180f,
                height = 700,
                childCanScrollBackward = false,
                phase = DrawerNestedScrollPhase.PRE_SCROLL
            )
        )
        assertEquals(0f, result.consumedDeltaY, 0f)
        assertEquals(180f, result.remainingDeltaY, 0f)
    }

    @Test
    fun downwardRemainderCollapsesDrawerDuringPostScroll() {
        val result = DrawerNestedScrollPolicy.resolve(
            input(
                delta = 180f,
                height = 700,
                childCanScrollBackward = false,
                phase = DrawerNestedScrollPhase.POST_SCROLL
            )
        )
        assertEquals(180f, result.consumedDeltaY, 0f)
        assertEquals(0f, result.remainingDeltaY, 0f)
    }

    @Test
    fun upwardRemainderIsNotConsumedDuringPostScroll() {
        val result = DrawerNestedScrollPolicy.resolve(
            input(
                delta = -80f,
                height = 400,
                phase = DrawerNestedScrollPhase.POST_SCROLL
            )
        )
        assertEquals(0f, result.consumedDeltaY, 0f)
        assertEquals(-80f, result.remainingDeltaY, 0f)
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

    @Test
    fun consumptionAlwaysConservesTheIncomingDeltaAcrossDirectionsAndPhases() {
        val deltas = listOf(-2_000f, -240f, -0.5f, 0f, 0.5f, 180f, 2_000f)
        val heights = listOf(100, 101, 400, 899, 900)
        val phases = DrawerNestedScrollPhase.entries

        for (delta in deltas) {
            for (height in heights) {
                for (phase in phases) {
                    val result = DrawerNestedScrollPolicy.resolve(
                        input(delta, height, phase = phase)
                    )
                    assertEquals(
                        "Delta must be conserved for delta=$delta height=$height phase=$phase",
                        delta,
                        result.consumedDeltaY + result.remainingDeltaY,
                        0.0001f
                    )
                    assertEquals(
                        "Consumed delta must not reverse direction",
                        true,
                        result.consumedDeltaY == 0f ||
                            kotlin.math.sign(result.consumedDeltaY) == kotlin.math.sign(delta)
                    )
                    assertEquals(
                        "Consumed magnitude must not exceed available magnitude",
                        true,
                        kotlin.math.abs(result.consumedDeltaY) <= kotlin.math.abs(delta)
                    )
                }
            }
        }
    }

    @Test
    fun childScrollDiagnosticFlagDoesNotOverrideActualPostScrollRemainder() {
        val childCanScroll = DrawerNestedScrollPolicy.resolve(
            input(
                delta = 120f,
                height = 600,
                childCanScrollBackward = true,
                phase = DrawerNestedScrollPhase.POST_SCROLL
            )
        )
        val childAtBoundary = DrawerNestedScrollPolicy.resolve(
            input(
                delta = 120f,
                height = 600,
                childCanScrollBackward = false,
                phase = DrawerNestedScrollPhase.POST_SCROLL
            )
        )

        assertEquals(childAtBoundary, childCanScroll)
        assertEquals(120f, childCanScroll.consumedDeltaY, 0f)
    }

    @Test
    fun veryLargeFiniteDeltaLeavesFiniteUnconsumedRemainder() {
        val result = DrawerNestedScrollPolicy.resolve(input(Float.MAX_VALUE, 700))
        assertEquals(0f, result.consumedDeltaY, 0f)
        assertEquals(Float.MAX_VALUE, result.remainingDeltaY, 0f)
        assertTrue(result.remainingDeltaY.isFinite())
    }

}
