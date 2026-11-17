/*
 * This file is part of zero-Symbol-input-view-V2.
 *
 * Copyright (C) 2026 android_zero
 *
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 *
 * License Information:
 *
 * This file is distributed under the following licenses, which apply
 * cumulatively to the same work:
 *
 * 1. GNU Affero General Public License v3.0 only (AGPL-3.0-only):
 *    Grants the rights to use, study, modify, and redistribute this work.
 *    When a modified version is used to provide network services to users,
 *    the corresponding source code must be made available under the
 *    applicable AGPL terms.
 *
 * 2. GNU General Public License v3.0 only (GPL-3.0-only):
 *    Grants the rights to use, study, modify, and redistribute this work.
 *    Distribution of covered binaries requires compliance with the source
 *    code, license notice, attribution, and other applicable GPL terms.
 *
 * 3. Apache License 2.0 (Apache-2.0):
 *    Grants copyright and applicable patent rights to use, modify, and
 *    distribute this work, subject to its license conditions, including
 *    preservation of notices, inclusion of the license, and marking changes.
 *
 * The SPDX operator "AND" means that all listed license terms apply
 * cumulatively. It does not mean that recipients may choose only one
 * of the listed licenses.
 *
 * You must comply with the applicable terms of all three licenses.
 * This notice is a summary and does not replace the full license texts.
 *
 * Full license texts:
 * - GNU AGPL v3.0: https://www.gnu.org/licenses/agpl-3.0.html
 * - GNU GPL v3.0:  https://www.gnu.org/licenses/gpl-3.0.html
 * - Apache 2.0:    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Author: android_zero
 * GitHub: https://github.com/msmt2018
 * WeChat: android_zero
 */
package com.zerostudio.idea.widget.drawer

typealias DrawerConfig = com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.config.DrawerConfig
typealias DrawerPresentationMode = com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.config.DrawerPresentationMode
typealias DrawerPreset = com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.preset.DrawerPreset
typealias DrawerPresets = com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.preset.DrawerPresets
typealias DrawerStyle = com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerStyle
typealias DrawerThemeMode = com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerThemeMode
typealias DrawerColorAttributes = com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerColorAttributes
typealias DrawerColors = com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerColors
typealias DrawerDimensions = com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerDimensions
typealias DrawerTypography = com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerTypography
typealias DrawerMotion = com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerMotion
typealias DrawerShape = com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerShape
typealias DrawerThemeResolver = com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerThemeResolver


/** Compose-native drawer entry point. */
typealias SymbolDrawerState = com.zerostudio.idea.widget.editor.symbolinput.v2.SymbolDrawerState
typealias DrawerContainerState = com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.DrawerContainerState

/** Compose-native generic drawer container facade. */
@androidx.compose.runtime.Composable
fun DrawerContainer(
    config: com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.config.DrawerConfig = com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.config.DrawerConfig(),
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    state: DrawerContainerState = com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.rememberDrawerContainerState(),
    title: String? = null,
    pages: List<com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerPage> = emptyList(),
    selectedPageId: String? = null,
    onPageSelected: (com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerPage) -> Unit = {},
    onDismiss: () -> Unit = {},
    content: @androidx.compose.runtime.Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) = com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.DrawerContainer(
    config = config,
    modifier = modifier,
    state = state,
    title = title,
    pages = pages,
    selectedPageId = selectedPageId,
    onPageSelected = onPageSelected,
    onDismiss = onDismiss,
    content = content
)
