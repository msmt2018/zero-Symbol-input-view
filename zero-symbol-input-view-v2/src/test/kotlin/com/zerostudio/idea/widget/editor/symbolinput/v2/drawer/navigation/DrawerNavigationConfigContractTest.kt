/*
 * This file is part of ZeroStudio.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation

import org.junit.Assert.*
import org.junit.Test

class DrawerNavigationConfigContractTest {
    @Test fun indicatorAcceptsBoundaryValues() {
        assertEquals(0f, DrawerIndicatorStyle(heightDp = 0f, widthFraction = 0f, cornerRadiusDp = 0f).widthFraction)
        assertEquals(1f, DrawerIndicatorStyle(widthFraction = 1f).widthFraction)
    }

    @Test fun indicatorRejectsInvalidDimensionsAndFractions() {
        assertThrows(IllegalArgumentException::class.java) { DrawerIndicatorStyle(heightDp = -1f) }
        assertThrows(IllegalArgumentException::class.java) { DrawerIndicatorStyle(widthFraction = -0.01f) }
        assertThrows(IllegalArgumentException::class.java) { DrawerIndicatorStyle(widthFraction = 1.01f) }
        assertThrows(IllegalArgumentException::class.java) { DrawerIndicatorStyle(cornerRadiusDp = -1f) }
    }

    @Test fun navigationConfigValidatesSizingAndTextScales() {
        assertThrows(IllegalArgumentException::class.java) { DrawerNavigationConfig(itemMinWidthDp = -1f) }
        assertThrows(IllegalArgumentException::class.java) { DrawerNavigationConfig(itemHorizontalPaddingDp = -1f) }
        assertThrows(IllegalArgumentException::class.java) { DrawerNavigationConfig(itemVerticalPaddingDp = -1f) }
        assertThrows(IllegalArgumentException::class.java) { DrawerNavigationConfig(titleTextSizeSp = 0f) }
        assertThrows(IllegalArgumentException::class.java) { DrawerNavigationConfig(selectedTitleTextSizeSp = -1f) }
    }

    @Test fun navigationModesAndVisibilityFlagsAreIndependent() {
        val config = DrawerNavigationConfig(
            enabled = false,
            placement = DrawerNavigationPlacement.HIDDEN,
            scrollable = false,
            showTitles = false,
            showIcons = true,
            selectedTitleBold = true,
            keepSelectedPageVisible = false,
            showInPeekPanel = true
        )
        assertFalse(config.enabled)
        assertEquals(DrawerNavigationPlacement.HIDDEN, config.placement)
        assertFalse(config.scrollable)
        assertFalse(config.showTitles)
        assertTrue(config.showIcons)
        assertTrue(config.selectedTitleBold)
        assertFalse(config.keepSelectedPageVisible)
        assertTrue(config.showInPeekPanel)
    }
}
