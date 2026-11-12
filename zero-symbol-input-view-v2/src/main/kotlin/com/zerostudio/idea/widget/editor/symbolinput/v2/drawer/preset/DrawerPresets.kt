package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.preset

import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.config.DrawerConfig
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core.DrawerAnchorStrategies
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerDimensions
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerShape
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerStyle

/** Named baseline layouts. Every preset remains overridable through DrawerConfig.Builder. */
enum class DrawerPreset { STANDARD, COMPACT, FULL_SCREEN, SPLIT_PANEL }

object DrawerPresets {
    @JvmStatic
    fun config(preset: DrawerPreset): DrawerConfig {
        val builder = DrawerConfig.builder()
        when (preset) {
            DrawerPreset.STANDARD -> builder
                .setAnchorStrategy(DrawerAnchorStrategies.existingHeight())
                .setStyle(DrawerStyle())
            DrawerPreset.COMPACT -> builder
                .setAnchorStrategy(DrawerAnchorStrategies.contentBased())
                .setStyle(DrawerStyle(
                    dimensions = DrawerDimensions(
                        peekHeightDp = 48f,
                        expandedMaxHeightDp = 420f,
                        cornerRadiusDp = 20f,
                        horizontalPaddingDp = 12f,
                        verticalPaddingDp = 8f,
                        panelSpacingDp = 6f
                    ),
                    shape = DrawerShape(20f, 20f)
                ))
            DrawerPreset.FULL_SCREEN -> builder
                .setAnchorStrategy(DrawerAnchorStrategies.threeStage(0.5f, 1f))
                .setStyle(DrawerStyle(
                    dimensions = DrawerDimensions(
                        peekHeightDp = 64f,
                        expandedMaxHeightDp = 1000f,
                        cornerRadiusDp = 16f,
                        horizontalPaddingDp = 16f,
                        verticalPaddingDp = 12f
                    ),
                    shape = DrawerShape(16f, 16f)
                ))
            DrawerPreset.SPLIT_PANEL -> builder
                .setAnchorStrategy(DrawerAnchorStrategies.threeStage())
                .setStyle(DrawerStyle(
                    dimensions = DrawerDimensions(
                        peekHeightDp = 56f,
                        expandedMaxHeightDp = 720f,
                        cornerRadiusDp = 24f,
                        horizontalPaddingDp = 16f,
                        verticalPaddingDp = 12f,
                        panelSpacingDp = 12f
                    )
                ))
        }
        return builder.build()
    }
}
