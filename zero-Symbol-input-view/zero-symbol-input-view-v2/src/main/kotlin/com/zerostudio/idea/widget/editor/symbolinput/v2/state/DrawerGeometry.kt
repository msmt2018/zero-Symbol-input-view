package com.zerostudio.idea.widget.editor.symbolinput.v2.state

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DrawerConfig
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import kotlin.math.ceil

/** Pure geometry functions; no View dependencies. */
object DrawerGeometry {
    fun collapsedHeightPx(config: DrawerConfig, density: Float): Int =
        gridHeightPx(config.collapsedRows, config, density)

    fun expandedHeightPx(groups: List<SymbolGroup>, config: DrawerConfig, density: Float): Int {
        val maxRows = groups.maxOfOrNull {
            ceil(it.symbols.size.toDouble() / config.columns).toInt().coerceAtLeast(1)
        } ?: config.collapsedRows
        val naturalHeight = gridHeightPx(maxOf(config.collapsedRows, maxRows), config, density)
        val maximum = (config.maxExpandedHeightDp * density).toInt()
        return naturalHeight.coerceAtMost(maximum).coerceAtLeast(collapsedHeightPx(config, density))
    }

    fun gridHeightPx(rows: Int, config: DrawerConfig, density: Float): Int {
        val safeRows = rows.coerceAtLeast(1)
        val cell = (config.cellHeightDp * density).toInt()
        val gap = (config.cellVerticalSpacingDp * density).toInt()
        val padding = (4f * density).toInt()
        return safeRows * cell + (safeRows - 1) * gap + padding * 2
    }

    fun indicatorReveal(expansion: Float): Float = expansion.coerceIn(0f, 1f)
}
