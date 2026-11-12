package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerNavigationConfigTest {
    @Test fun defaultsProvideOptionalTopScrollableNavigation() {
        val config = DrawerNavigationConfig()
        assertTrue(config.enabled)
        assertEquals(DrawerNavigationPlacement.TOP, config.placement)
        assertTrue(config.scrollable)
        assertTrue(config.showTitles)
        assertFalse(config.showIcons)
        assertFalse(config.showInPeekPanel)
    }

    @Test fun indicatorSupportsLinePillAndHiddenModes() {
        assertEquals(DrawerIndicatorShape.LINE, DrawerIndicatorStyle().shape)
        assertEquals(DrawerIndicatorShape.PILL, DrawerIndicatorStyle(shape = DrawerIndicatorShape.PILL).shape)
        assertEquals(DrawerIndicatorShape.NONE, DrawerIndicatorStyle(shape = DrawerIndicatorShape.NONE).shape)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOutOfRangeIndicatorWidth() {
        DrawerIndicatorStyle(widthFraction = 1.2f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonPositiveNavigationTextSize() {
        DrawerNavigationConfig(titleTextSizeSp = 0f)
    }
}
