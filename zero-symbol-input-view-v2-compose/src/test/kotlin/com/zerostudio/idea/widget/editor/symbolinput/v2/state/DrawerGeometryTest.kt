/*
 * This file is part of zero-Symbol-input-view-V2.
 *
 * Copyright (C) 2026 android_zero
 *
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.state

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DrawerConfig
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerGeometryTest {
    private val config = DrawerConfig(columns = 4, collapsedRows = 2, cellHeightDp = 30)

    @Test fun collapsedHeightUsesConfiguredRows() {
        assertEquals(70, DrawerGeometry.collapsedHeightPx(config, 1f))
    }

    @Test fun collapsedHeightTracksConfiguredRowsInsteadOfAViewHardcodedRowCount() {
        listOf(1, 2, 3, 5, 8).forEach { rows ->
            val configured = config.copy(collapsedRows = rows)
            assertEquals(DrawerGeometry.gridHeightPx(rows, configured, 1f),
                DrawerGeometry.collapsedHeightPx(configured, 1f))
        }
    }

    @Test fun expandedHeightUsesLargestGroup() {
        val groups = listOf(group("a", 5), group("b", 9))
        assertEquals(102, DrawerGeometry.expandedHeightPx(groups, config, 1f))
    }

    @Test fun minimumTouchTargetContributesToGridHeight() {
        val accessible = config.copy(minimumTouchTargetDp = 48)
        assertEquals(106, DrawerGeometry.collapsedHeightPx(accessible, 1f))
    }

    @Test fun expandedHeightRespectsConfiguredMaximum() {
        val limited = config.copy(maxExpandedHeightDp = 120)
        assertEquals(94, DrawerGeometry.expandedHeightPx(listOf(group("large", 100)), limited, 1f))
    }

    @Test fun gridHeightRejectsNonPositiveOrNonFiniteDensity() {
        listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)
            .forEach { density ->
                assertTrue("Expected invalid density $density to be rejected",
                    runCatching { DrawerGeometry.gridHeightPx(2, config, density) }.isFailure)
            }
    }

    @Test fun expandedHeightRejectsNonFiniteDensity() {
        assertTrue(runCatching {
            DrawerGeometry.expandedHeightPx(emptyList(), config, Float.NaN)
        }.isFailure)
    }

    @Test fun extremeFiniteDensitySaturatesInsteadOfWrappingPixelHeight() {
        assertEquals(Int.MAX_VALUE, DrawerGeometry.gridHeightPx(2, config, Float.MAX_VALUE))
    }

    @Test fun indicatorRevealIsClampedAndInterpolated() {
        assertEquals(0f, DrawerGeometry.indicatorReveal(0f), 0.0001f)
        assertEquals(0.315f, DrawerGeometry.indicatorReveal(0.315f), 0.0001f)
        assertEquals(1f, DrawerGeometry.indicatorReveal(1f), 0.0001f)
    }

    private fun group(id: String, count: Int) = SymbolGroup(
        id = id,
        title = id,
        symbols = (0 until count).map {
            SymbolEntry(it.toLong(), it.toString(), SymbolAction.InsertText(it.toString()))
        }
    )
}
