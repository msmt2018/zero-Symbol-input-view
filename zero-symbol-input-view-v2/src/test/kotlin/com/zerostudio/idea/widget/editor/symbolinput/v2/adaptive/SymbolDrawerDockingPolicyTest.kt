package com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive

import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.LargeScreenLayoutMode
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SideDockEdge
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class SymbolDrawerDockingPolicyTest {
    @Test fun automaticModeUsesWindowWidthNotDeviceIdentity() {
        val settings = SymbolDrawerSettings()
        assertEquals(SymbolDrawerDocking.BOTTOM, SymbolDrawerDockingPolicy.resolve(settings, window(599)))
        assertEquals(SymbolDrawerDocking.RIGHT, SymbolDrawerDockingPolicy.resolve(settings, window(600)))
    }

    @Test fun automaticModeUsesAvailableWidthAfterInsets() {
        val settings = SymbolDrawerSettings()
        val insetWindow = WindowAdaptiveInfo(
            widthDp = 620f,
            heightDp = 900f,
            insets = com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.WindowInsetsDp(
                left = 16f,
                right = 16f
            )
        )
        assertEquals(
            SymbolDrawerDocking.BOTTOM,
            SymbolDrawerDockingPolicy.resolve(settings, insetWindow)
        )
    }

    @Test fun manualBottomOverridesLargeWindow() {
        val settings = SymbolDrawerSettings(largeScreenLayoutMode = LargeScreenLayoutMode.BOTTOM)
        assertEquals(SymbolDrawerDocking.BOTTOM, SymbolDrawerDockingPolicy.resolve(settings, window(1000)))
    }

    @Test fun manualSideAndEdgeAreRespectedOnCompactWindow() {
        val settings = SymbolDrawerSettings(
            largeScreenLayoutMode = LargeScreenLayoutMode.SIDE,
            sideDockEdge = SideDockEdge.LEFT
        )
        assertEquals(SymbolDrawerDocking.LEFT, SymbolDrawerDockingPolicy.resolve(settings, window(400)))
    }

    @Test fun sideWidthIsBounded() {
        assertEquals(56, SymbolDrawerDockingPolicy.sideWidthDp(20))
        assertEquals(88, SymbolDrawerDockingPolicy.sideWidthDp(200))
    }

    private fun window(width: Int) = WindowAdaptiveInfo.fromPixels(
        widthPx = width,
        heightPx = 1200,
        density = 1f
    )
}
