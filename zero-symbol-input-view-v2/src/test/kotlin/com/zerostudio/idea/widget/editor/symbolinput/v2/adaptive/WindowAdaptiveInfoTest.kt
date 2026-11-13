/*
 * This file is part of ZeroStudio.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive

import org.junit.Assert.assertEquals
import org.junit.Test

class WindowAdaptiveInfoTest {
    @Test fun classifiesCurrentWindowWidthAtCanonicalBreakpoints() {
        assertEquals(WindowSizeClass.COMPACT, WindowAdaptiveInfo(599f, 800f).sizeClass)
        assertEquals(WindowSizeClass.MEDIUM, WindowAdaptiveInfo(600f, 800f).sizeClass)
        assertEquals(WindowSizeClass.MEDIUM, WindowAdaptiveInfo(839f, 800f).sizeClass)
        assertEquals(WindowSizeClass.EXPANDED, WindowAdaptiveInfo(840f, 800f).sizeClass)
    }

    @Test fun insetsReduceAvailableWindowBeforeClassification() {
        val info = WindowAdaptiveInfo(900f, 700f, insets = WindowInsetsDp(left = 40f, right = 40f))
        assertEquals(820f, info.availableWidthDp)
        assertEquals(WindowSizeClass.MEDIUM, info.sizeClass)
    }

    @Test fun pixelFactoryUsesDensity() {
        val info = WindowAdaptiveInfo.fromPixels(1200, 1800, 2f)
        assertEquals(600f, info.widthDp)
        assertEquals(WindowSizeClass.MEDIUM, info.sizeClass)
    }
}
