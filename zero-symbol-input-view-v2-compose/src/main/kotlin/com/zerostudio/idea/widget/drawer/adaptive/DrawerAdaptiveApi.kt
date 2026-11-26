/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.drawer.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.AdaptiveWindowObserver as InternalAdaptiveWindowObserver
import com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.rememberAdaptiveWindowController as rememberInternalAdaptiveWindowController

typealias AdaptiveWindowController = com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.AdaptiveWindowController
typealias AdaptiveContentLayoutSpec = com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.AdaptiveContentLayoutSpec
typealias AdaptiveContentLayoutPolicy = com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.AdaptiveContentLayoutPolicy
typealias AdaptiveContentLayoutResolver = com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.AdaptiveContentLayoutResolver
typealias WindowAdaptiveInfo = com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.WindowAdaptiveInfo
typealias WindowInsetsDp = com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.WindowInsetsDp
typealias FoldingFeatureInfo = com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.FoldingFeatureInfo
typealias WindowSizeClass = com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.WindowSizeClass
typealias WindowPosture = com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.WindowPosture
typealias WindowInsetsMode = com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.WindowInsetsMode

/** Public composition-native adaptive window observer. */
@Composable
fun AdaptiveWindowObserver(
    modifier: Modifier = Modifier,
    foldingFeature: FoldingFeatureInfo = FoldingFeatureInfo(),
    insetsMode: WindowInsetsMode = WindowInsetsMode.SUBTRACT_SYSTEM_INSETS,
    content: @Composable (WindowAdaptiveInfo) -> Unit
) {
    InternalAdaptiveWindowObserver(
        modifier = modifier,
        foldingFeature = foldingFeature,
        insetsMode = insetsMode,
        content = content
    )
}

/** Creates a controller whose observable snapshot is owned by Composition. */
@Composable
fun rememberAdaptiveWindowController(
    initialSnapshot: WindowAdaptiveInfo
): AdaptiveWindowController = rememberInternalAdaptiveWindowController(initialSnapshot)


/** Subscribes the current Composition to adaptive-window snapshot updates. */
@Composable
fun rememberAdaptiveWindowSnapshot(
    controller: AdaptiveWindowController
): WindowAdaptiveInfo =
    com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.rememberAdaptiveWindowSnapshot(controller)
