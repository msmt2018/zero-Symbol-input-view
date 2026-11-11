package com.zerostudio.idea.widget.editor.symbolinput.v2.state

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DrawerConfig
import kotlin.math.ceil

/**
 * Pure geometry functions. Keeping this calculation out of View code makes
 * height behaviour deterministic and straightforward to unit test.
 */
object DrawerGeometry {
    fun collapsedHeightPx(config: DrawerConfig, density: Float): Int =
        gridHeightPx(config.collapsedRows, config, density)

    fun expandedHeightPx(
        groups: List<com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup>,
        config: DrawerConfig,
        density: Float
    ): Int {
        val maxRows = groups.maxOfOrNull {
            ceil(it.symbols.size.toDouble() / config.columns).toInt().coerceAtLeast(1)
        } ?: config.collapsedRows
        return gridHeightPx(maxOf(config.collapsedRows, maxRows), config, density)
    }

    fun gridHeightPx(rows: Int, config: DrawerConfig, density: Float): Int {
        val safeRows = rows.coerceAtLeast(1)
        val cell = (config.cellHeightDp * density).toInt()
        val gap = (config.cellVerticalSpacingDp * density).toInt()
        val padding = (4f * density).toInt()
        return safeRows * cell + (safeRows - 1) * gap + padding * 2
    }

    /**
     * Linear reveal keeps the rendered height equal to the finger-controlled
     * drawer height: grid interpolation + indicator interpolation share one fraction.
     */
    fun indicatorReveal(expansion: Float): Float =
        expansion.coerceIn(0f, 1f)
}
