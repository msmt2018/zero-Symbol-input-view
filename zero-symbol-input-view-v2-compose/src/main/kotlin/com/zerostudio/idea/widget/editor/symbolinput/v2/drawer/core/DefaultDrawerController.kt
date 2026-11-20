/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.config.DrawerConfig
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.config.DrawerPresentationMode
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerNavigationConfig
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerNavigationController
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerPage
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.panel.DrawerPanelContent
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerColors
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerDimensions
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerMotion
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerShape
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerStyle
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerThemeMode
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerTypography

/**
 * Default host-facing controller backed by exactly one [DrawerStateHolder].
 *
 * Geometry, drag state, resolved anchors and animation targets are never copied
 * into this controller. Configuration and page navigation are orthogonal
 * presentation state; the Compose host remains responsible for measuring,
 * rendering and executing animations.
 *
 * [anchorInputProvider] is invoked only by [refreshAnchors], allowing the host
 * to supply its latest measured geometry without introducing View or lifecycle
 * dependencies into this API layer.
 */
class DefaultDrawerController(
    val stateHolder: DrawerStateHolder,
    private val anchorInputProvider: () -> DrawerAnchorInput
) : DrawerController {

    var config: DrawerConfig by mutableStateOf(DrawerConfig(anchorStrategy = stateHolder.anchorStrategy))
        private set

    var title: String? by mutableStateOf(null)
        private set

    val navigationController = DrawerNavigationController()

    override fun expand(animated: Boolean) {
        val target = stateHolder.anchors.maxByOrNull { it.heightPx } ?: return
        if (animated) stateHolder.moveToAnchor(target.id) else stateHolder.snapToAnchor(target.id)
    }

    override fun collapse(animated: Boolean) {
        val target = stateHolder.anchors.minByOrNull { it.heightPx } ?: return
        if (animated) stateHolder.moveToAnchor(target.id) else stateHolder.snapToAnchor(target.id)
    }

    override fun moveToAnchor(anchorId: String, animated: Boolean) {
        if (animated) stateHolder.moveToAnchor(anchorId) else stateHolder.snapToAnchor(anchorId)
    }

    override fun refreshAnchors() {
        stateHolder.updateGeometry(anchorInputProvider())
    }

    override fun setAnchorStrategy(strategy: DrawerAnchorStrategy) {
        // Re-applying an equivalent strategy must not interrupt active motion.
        if (strategy != stateHolder.anchorStrategy) {
            stateHolder.setAnchorStrategy(strategy)
        }
        if (strategy != config.anchorStrategy) {
            config = config.copy(anchorStrategy = strategy)
        }
    }

    override fun setPresentationMode(mode: DrawerPresentationMode) {
        config = config.copy(presentationMode = mode)
    }

    override fun applyConfig(config: DrawerConfig) {
        // Style/presentation-only updates must preserve an active drag/animation.
        if (config.anchorStrategy != stateHolder.anchorStrategy) {
            stateHolder.setAnchorStrategy(config.anchorStrategy)
        }
        this.config = config
    }

    override fun setStyle(style: DrawerStyle) {
        config = config.copy(style = style)
    }

    override fun setThemeMode(mode: DrawerThemeMode) {
        setStyle(config.style.copy(themeMode = mode))
    }

    override fun setColors(colors: DrawerColors) {
        setStyle(config.style.copy(colors = colors))
    }

    override fun setDimensions(dimensions: DrawerDimensions) {
        setStyle(config.style.copy(dimensions = dimensions))
    }

    override fun setTypography(typography: DrawerTypography) {
        setStyle(config.style.copy(typography = typography))
    }

    override fun setMotion(motion: DrawerMotion) {
        setStyle(config.style.copy(motion = motion))
    }

    override fun setShape(shape: DrawerShape) {
        setStyle(config.style.copy(shape = shape))
    }

    override fun setPeekContent(content: DrawerPanelContent?) {
        config = config.copy(peekContent = content)
    }

    override fun setExpandedContent(content: DrawerPanelContent?) {
        config = config.copy(expandedContent = content)
    }

    override fun setTitle(title: CharSequence?) {
        this.title = title?.toString()
    }

    override fun setPages(pages: List<DrawerPage>, preserveSelection: Boolean) {
        navigationController.submitPages(pages, preserveSelection)
    }

    override fun selectPage(pageId: String): Boolean =
        navigationController.selectPage(pageId)

    override fun setNavigationConfig(config: DrawerNavigationConfig) {
        this.config = this.config.copy(navigationConfig = config)
    }

    override fun setDragEnabled(enabled: Boolean) {
        config = config.copy(enableDrag = enabled)
    }

    override fun setNestedScrollEnabled(enabled: Boolean) {
        config = config.copy(enableNestedScroll = enabled)
    }

    override fun setEdgeToEdgeEnabled(enabled: Boolean) {
        config = config.copy(enableEdgeToEdge = enabled)
    }

    override fun setDismissOnScrimTap(enabled: Boolean) {
        config = config.copy(dismissOnScrimTap = enabled)
    }

    override fun setDismissOnBack(enabled: Boolean) {
        config = config.copy(dismissOnBack = enabled)
    }

    override fun setDragHandleEnabled(enabled: Boolean) {
        config = config.copy(dragHandleEnabled = enabled)
    }

    override fun setAccessibilityEnabled(enabled: Boolean) {
        config = config.copy(accessibilityEnabled = enabled)
    }

    override fun currentState(): DrawerState = stateHolder.snapshot

    override fun addStateListener(listener: (DrawerState) -> Unit) {
        stateHolder.addStateListener(listener)
    }

    override fun removeStateListener(listener: (DrawerState) -> Unit) {
        stateHolder.removeStateListener(listener)
    }
}
