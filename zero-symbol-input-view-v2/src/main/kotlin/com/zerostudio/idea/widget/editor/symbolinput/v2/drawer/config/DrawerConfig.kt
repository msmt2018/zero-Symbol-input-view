package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.config

import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core.DrawerAnchorStrategies
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core.DrawerAnchorStrategy
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.panel.DrawerPanelContent
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerColors
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerDimensions
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerMotion
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerShape
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerStyle
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerThemeMode
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerTypography

/** Declarative, feature-neutral configuration consumed by a future DrawerContainer. */
data class DrawerConfig(
    val anchorStrategy: DrawerAnchorStrategy = DrawerAnchorStrategies.existingHeight(),
    val style: DrawerStyle = DrawerStyle(),
    val peekContent: DrawerPanelContent? = null,
    val expandedContent: DrawerPanelContent? = null,
    val enableDrag: Boolean = true,
    val enableNestedScroll: Boolean = true,
    val enableEdgeToEdge: Boolean = false,
    val dismissOnScrimTap: Boolean = true,
    val dismissOnBack: Boolean = true,
    val dragHandleEnabled: Boolean = true,
    val accessibilityEnabled: Boolean = true
) {
    class Builder {
        private var anchorStrategy: DrawerAnchorStrategy = DrawerAnchorStrategies.existingHeight()
        private var style: DrawerStyle = DrawerStyle()
        private var peekContent: DrawerPanelContent? = null
        private var expandedContent: DrawerPanelContent? = null
        private var enableDrag = true
        private var enableNestedScroll = true
        private var enableEdgeToEdge = false
        private var dismissOnScrimTap = true
        private var dismissOnBack = true
        private var dragHandleEnabled = true
        private var accessibilityEnabled = true

        fun setAnchorStrategy(value: DrawerAnchorStrategy) = apply { anchorStrategy = value }
        fun setPeekContent(value: DrawerPanelContent?) = apply { peekContent = value }
        fun setExpandedContent(value: DrawerPanelContent?) = apply { expandedContent = value }
        fun setDragEnabled(value: Boolean) = apply { enableDrag = value }
        fun setNestedScrollEnabled(value: Boolean) = apply { enableNestedScroll = value }
        fun setEdgeToEdgeEnabled(value: Boolean) = apply { enableEdgeToEdge = value }
        fun setDismissOnScrimTap(value: Boolean) = apply { dismissOnScrimTap = value }
        fun setDismissOnBack(value: Boolean) = apply { dismissOnBack = value }
        fun setDragHandleEnabled(value: Boolean) = apply { dragHandleEnabled = value }
        fun setAccessibilityEnabled(value: Boolean) = apply { accessibilityEnabled = value }

        fun setStyle(value: DrawerStyle) = apply { style = value }
        fun setThemeMode(value: DrawerThemeMode) = apply {
            style = style.copy(themeMode = value)
        }
        fun setFollowHostTheme() = setThemeMode(DrawerThemeMode.FOLLOW_HOST)
        fun setColors(value: DrawerColors) = apply { style = style.copy(colors = value) }
        fun setDimensions(value: DrawerDimensions) = apply { style = style.copy(dimensions = value) }
        fun setTypography(value: DrawerTypography) = apply { style = style.copy(typography = value) }
        fun setMotion(value: DrawerMotion) = apply { style = style.copy(motion = value) }
        fun setShape(value: DrawerShape) = apply { style = style.copy(shape = value) }
        fun setElevationDp(value: Float) = apply { style = style.copy(elevationDp = value) }
        fun setScrimEnabled(value: Boolean) = apply { style = style.copy(drawScrim = value) }
        fun setClipContentToShape(value: Boolean) = apply { style = style.copy(clipContentToShape = value) }

        fun setPeekHeightDp(value: Float) = apply {
            style = style.copy(dimensions = style.dimensions.copy(peekHeightDp = value))
        }
        fun setExpandedMaxHeightDp(value: Float) = apply {
            style = style.copy(dimensions = style.dimensions.copy(expandedMaxHeightDp = value))
        }
        fun setCornerRadiusDp(value: Float) = apply {
            style = style.copy(dimensions = style.dimensions.copy(cornerRadiusDp = value),
                shape = style.shape.copy(topStartRadiusDp = value, topEndRadiusDp = value))
        }
        fun setHorizontalPaddingDp(value: Float) = apply {
            style = style.copy(dimensions = style.dimensions.copy(horizontalPaddingDp = value))
        }
        fun setVerticalPaddingDp(value: Float) = apply {
            style = style.copy(dimensions = style.dimensions.copy(verticalPaddingDp = value))
        }
        fun setSettleDurationMs(value: Long) = apply {
            style = style.copy(motion = style.motion.copy(settleDurationMs = value))
        }
        fun setDragResistance(value: Float) = apply {
            style = style.copy(motion = style.motion.copy(dragResistance = value))
        }

        fun build(): DrawerConfig = DrawerConfig(
            anchorStrategy, style, peekContent, expandedContent, enableDrag,
            enableNestedScroll, enableEdgeToEdge, dismissOnScrimTap, dismissOnBack,
            dragHandleEnabled, accessibilityEnabled
        )
    }

    companion object {
        @JvmStatic fun builder() = Builder()
    }
}
