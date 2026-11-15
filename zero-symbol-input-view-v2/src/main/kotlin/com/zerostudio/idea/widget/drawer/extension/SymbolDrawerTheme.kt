/*
 * Public JVM theme extension API.
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.drawer.extension

import android.content.Context
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings

/** Immutable theme token snapshot. Color values use Android ARGB encoding. */
data class SymbolDrawerTheme(
    val primary: Int,
    val onPrimary: Int,
    val surface: Int,
    val onSurface: Int,
    val secondaryText: Int,
    val outline: Int,
    val drawerBackground: Int = surface,
    val selectedCellBackground: Int = primary
)

/**
 * Java/Kotlin JVM extension point for supplying a drawer palette.
 *
 * The extension is registered with the owning drawer's extension registry.
 * Returning a new snapshot causes attached drawer surfaces to receive the
 * updated palette; null is represented by clearing the selected theme ID.
 */
interface SymbolDrawerThemeExtension {
    val id: String
    val title: String
    val priority: Int get() = 0
    fun resolve(context: Context, settings: SymbolDrawerSettings): SymbolDrawerTheme
}
