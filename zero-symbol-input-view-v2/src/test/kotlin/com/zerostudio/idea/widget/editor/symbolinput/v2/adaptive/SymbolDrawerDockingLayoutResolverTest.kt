package com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolDrawerDockingLayoutResolverTest {
    @Test fun bottomModePreservesLegacyHeightAnchorsExactly() {
        val spec = SymbolDrawerDockingLayoutResolver.resolve(
            docking = SymbolDrawerDocking.BOTTOM,
            window = window(1200, 800),
            bottomCollapsedHeightDp = 54f,
            bottomExpandedHeightDp = 286f
        )
        assertEquals(54f, spec.collapsedExtentDp, 0f)
        assertEquals(286f, spec.expandedExtentDp, 0f)
        assertEquals(1200f, spec.crossAxisExtentDp, 0f)
        assertEquals(0f, spec.sideRailWidthDp, 0f)
    }

    @Test fun sideModeUsesFullAvailableWindowHeightAndKeepsEditorSpace() {
        val spec = SymbolDrawerDockingLayoutResolver.resolve(
            docking = SymbolDrawerDocking.LEFT,
            window = window(1000, 700),
            bottomCollapsedHeightDp = 54f,
            bottomExpandedHeightDp = 286f
        )
        assertEquals(700f, spec.crossAxisExtentDp, 0f)
        assertEquals(72f, spec.collapsedExtentDp, 0f)
        assertEquals(480f, spec.expandedExtentDp, 0f)
        assertEquals(520f, spec.remainingEditorWidthDp, 0f)
        assertTrue(spec.isSideDocked)
    }

    @Test fun sideWidthNeverConsumesTheMinimumEditorViewport() {
        val spec = SymbolDrawerDockingLayoutResolver.resolve(
            docking = SymbolDrawerDocking.RIGHT,
            window = window(700, 900),
            bottomCollapsedHeightDp = 50f,
            bottomExpandedHeightDp = 250f
        )
        assertEquals(340f, spec.expandedExtentDp, 0f)
        assertEquals(360f, spec.remainingEditorWidthDp, 0f)
    }

    @Test fun narrowWindowClampsExpandedWidthToAvailableSpace() {
        val spec = SymbolDrawerDockingLayoutResolver.resolve(
            docking = SymbolDrawerDocking.LEFT,
            window = window(500, 700),
            bottomCollapsedHeightDp = 50f,
            bottomExpandedHeightDp = 250f
        )
        assertEquals(72f, spec.collapsedExtentDp, 0f)
        assertEquals(140f, spec.expandedExtentDp, 0f)
        assertEquals(360f, spec.remainingEditorWidthDp, 0f)
    }

    private fun window(width: Int, height: Int) = WindowAdaptiveInfo(
        widthDp = width.toFloat(),
        heightDp = height.toFloat()
    )
}
