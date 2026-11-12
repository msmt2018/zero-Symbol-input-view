package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.config

import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core.ThreeStageAnchorStrategy
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.preset.DrawerPreset
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.preset.DrawerPresets
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerConfigTest {
    @Test fun builderExposesAnchorAndInteractionSetters() {
        val config = DrawerConfig.builder()
            .setAnchorStrategy(ThreeStageAnchorStrategy())
            .setDragEnabled(false)
            .setNestedScrollEnabled(false)
            .setDismissOnBack(false)
            .setDismissOnScrimTap(false)
            .setDragHandleEnabled(false)
            .build()

        assertTrue(config.anchorStrategy is ThreeStageAnchorStrategy)
        assertFalse(config.enableDrag)
        assertFalse(config.enableNestedScroll)
        assertFalse(config.dismissOnBack)
        assertFalse(config.dismissOnScrimTap)
        assertFalse(config.dragHandleEnabled)
    }

    @Test fun builderSupportsGranularStyleSetters() {
        val config = DrawerConfig.builder()
            .setThemeMode(DrawerThemeMode.DARK)
            .setSurfaceColor(0xff101010.toInt())
            .setPrimaryTextColor(0xffffffff.toInt())
            .setCornerRadiusDp(28f)
            .setTitleTextSizeSp(20f)
            .setSettleDurationMs(300L)
            .setDragResistance(0.6f)
            .build()

        assertEquals(DrawerThemeMode.DARK, config.style.themeMode)
        assertEquals(0xff101010.toInt(), config.style.colors.surface)
        assertEquals(0xffffffff.toInt(), config.style.colors.primaryText)
        assertEquals(28f, config.style.dimensions.cornerRadiusDp)
        assertEquals(20f, config.style.typography.titleSizeSp)
        assertEquals(300L, config.style.motion.settleDurationMs)
        assertEquals(0.6f, config.style.motion.dragResistance)
    }

    @Test fun presetsRemainDistinctAndOverrideable() {
        val compact = DrawerPresets.config(DrawerPreset.COMPACT)
        val full = DrawerPresets.config(DrawerPreset.FULL_SCREEN)
        assertTrue(compact.style.dimensions.expandedMaxHeightDp < full.style.dimensions.expandedMaxHeightDp)
        assertTrue(DrawerConfig.builder().setStyle(compact.style).setPeekHeightDp(72f)
            .build().style.dimensions.peekHeightDp == 72f)
    }
}
