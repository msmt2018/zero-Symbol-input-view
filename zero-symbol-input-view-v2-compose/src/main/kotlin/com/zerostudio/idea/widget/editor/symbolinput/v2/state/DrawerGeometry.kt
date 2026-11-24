/*
 * This file is part of zero-Symbol-input-view-V2.
 *
 * Copyright (C) 2026 android_zero
 *
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.state

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DrawerConfig
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import kotlin.math.ceil

/** Pure geometry functions; no View dependencies. */
object DrawerGeometry {
    fun collapsedHeightPx(config: DrawerConfig, density: Float): Int =
        gridHeightPx(config.collapsedRows, config, density)

    fun expandedHeightPx(groups: List<SymbolGroup>, config: DrawerConfig, density: Float): Int {
        requireValidDensity(density)
        val maxRows = groups.maxOfOrNull {
            ceil(it.symbols.size.toDouble() / config.columns).toInt().coerceAtLeast(1)
        } ?: config.collapsedRows
        val naturalHeight = gridHeightPx(maxOf(config.collapsedRows, maxRows), config, density)
        val maximumGridHeight = saturatingInt(
            (config.maxExpandedHeightDp - config.indicatorHeightDp)
                .coerceAtLeast(0).toDouble() * density.toDouble()
        )
        return naturalHeight.coerceAtMost(maximumGridHeight)
            .coerceAtLeast(collapsedHeightPx(config, density))
    }

    fun gridHeightPx(rows: Int, config: DrawerConfig, density: Float): Int {
        requireValidDensity(density)
        val safeRows = rows.coerceAtLeast(1)
        val cellDp = maxOf(config.cellHeightDp, config.minimumTouchTargetDp)
        val totalPx = safeRows.toDouble() * cellDp.toDouble() * density.toDouble() +
            (safeRows - 1).toDouble() * config.cellVerticalSpacingDp.toDouble() * density.toDouble() +
            8.0 * density.toDouble()
        return saturatingInt(totalPx)
    }

    fun indicatorReveal(expansion: Float): Float {
        require(expansion.isFinite()) { "expansion must be finite" }
        return expansion.coerceIn(0f, 1f)
    }

    private fun requireValidDensity(density: Float) {
        require(density.isFinite() && density > 0f) {
            "density must be finite and > 0"
        }
    }

    /** Converts a pixel extent without allowing Int arithmetic to wrap. */
    private fun saturatingInt(value: Double): Int = when {
        value.isNaN() -> 0
        value >= Int.MAX_VALUE.toDouble() -> Int.MAX_VALUE
        value <= Int.MIN_VALUE.toDouble() -> Int.MIN_VALUE
        else -> value.toInt()
    }
}
