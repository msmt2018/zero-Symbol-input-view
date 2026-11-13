/*
 * This file is part of ZeroStudio.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive

import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveGridResolverTest {
    @Test fun compactWindowsReflowToFitMinimumCellWidth() {
        val spec = AdaptiveGridResolver.resolve(
            window = WindowAdaptiveInfo(360f, 800f),
            preferredColumns = 12,
            minimumCellWidthDp = 40f,
            baseHorizontalSpacingDp = 2
        )
        assertEquals(8, spec.columnCount)
        assertEquals(2, spec.horizontalSpacingDp)
    }

    @Test fun expandedWindowsUseMoreColumnsWithoutExceedingConfiguredMaximum() {
        val spec = AdaptiveGridResolver.resolve(
            window = WindowAdaptiveInfo(1000f, 800f),
            preferredColumns = 18,
            minimumCellWidthDp = 40f,
            baseHorizontalSpacingDp = 2
        )
        assertEquals(18, spec.columnCount)
        assertEquals(5, spec.horizontalSpacingDp)
    }

    @Test fun fixedDimensionModePreservesConfiguredColumnsAndSpacing() {
        val spec = AdaptiveGridResolver.resolve(
            window = WindowAdaptiveInfo(360f, 800f),
            preferredColumns = 12,
            minimumCellWidthDp = 40f,
            baseHorizontalSpacingDp = -8,
            adaptive = false
        )
        assertEquals(12, spec.columnCount)
        assertEquals(-8, spec.horizontalSpacingDp)
    }

    @Test fun gridAlwaysKeepsAtLeastOneColumn() {
        val spec = AdaptiveGridResolver.resolve(
            window = WindowAdaptiveInfo(100f, 800f),
            preferredColumns = 12,
            minimumCellWidthDp = 48f,
            baseHorizontalSpacingDp = 24
        )
        assertEquals(1, spec.columnCount)
    }
}
