/*
 * This file is part of zero-Symbol-input-view-V2.
 *
 * Copyright (C) 2026 android_zero
 *
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 *
 * License Information:
 *
 * This file is distributed under the AGPL-3.0-only, GPL-3.0-only and
 * Apache-2.0 licenses cumulatively. See the repository license files for
 * the complete terms.
 *
 * Author: android_zero
 * GitHub: https://github.com/msmt2018
 * WeChat: android_zero
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.settings

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DrawerConfig

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Presentation strategies for the Compose group-navigation indicator.
 * These values select an indicator renderer without owning page state.
 */
enum class GroupIndicatorStyle {
    TEXT_TABS, TEXT, PILL, DOTS, UNDERLINE,
    GOOEY_METABALL, ELASTIC_WORM, SPRING_BLOB, LIQUID_MERGE,
    VERNIER_TICKS, MECHANICAL_RULER, WHEEL_ROLLER, CAMERA_DIAL,
    RADIAL_RING, RADAR_SWEEP, HUD_ARC, ORBIT_DOTS,
    ELASTIC_WAVE, SINE_WAVE, BOUNCE_TRACK, SPRING_SLIDER,
    MORPHING_BAR, SEGMENTED_TRACK, PROGRESS_LINE, DOUBLE_TRACK,
    MINIMAL_DASHES, SQUARE_TICKS, DIAMOND_MARKERS, TRIANGLE_POINTER,
    HEXAGON_NODES, GLOWING_NODES, PULSE_DOTS, STACKED_CARDS,
    MINI_PREVIEW, THUMBNAIL_STRIP, NUMBER_BADGES, VERTICAL_RAIL,
    ARC_NODES, FLUID_TRAIL, DIGITAL_COUNTER, STEPPER
}
enum class HapticMode { OFF, SELECTION, ALL }

/** Large-window placement preference for the symbol toolbar. */
enum class LargeScreenLayoutMode { AUTO, BOTTOM, SIDE }

/** Preferred edge when the toolbar is docked vertically on a large window. */
enum class SideDockEdge { LEFT, RIGHT }

/** Immutable, validated settings snapshot persisted by the V2 settings repository. */
data class SymbolDrawerSettings(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val columns: Int = 10,
    val collapsedRows: Int = 2,
    val cellHeightDp: Int = 28,
    val cellHorizontalSpacingDp: Int = 2,
    val cellVerticalSpacingDp: Int = -2,
    val indicatorHeightDp: Int = 26,
    val indicatorStyle: GroupIndicatorStyle = GroupIndicatorStyle.UNDERLINE,
    val showGroupTitles: Boolean = true,
    val showSymbolLabels: Boolean = true,
    val rememberExpandedState: Boolean = true,
    val showFullSymbolLabels: Boolean = false,
    val symbolLabelTextSizeSp: Int = 18,
    val showActionHints: Boolean = false,
    val maxExpandedHeightDp: Int = 480,
    val animationDurationMs: Int = 200,
    val reduceMotion: Boolean = false,
    val flingVelocityDpPerSecond: Int = 900,
    val longPressTimeoutMs: Int = 420,
    val hapticMode: HapticMode = HapticMode.SELECTION,
    val autoCollapseAfterAction: Boolean = false,
    val pageCacheLimit: Int = 3,
    val prefetchAdjacentGroups: Boolean = true,
    val minimumTouchTargetDp: Int = 40,
    val searchIncludesActionPayload: Boolean = false,
    val largeScreenLayoutMode: LargeScreenLayoutMode = LargeScreenLayoutMode.AUTO,
    val sideDockEdge: SideDockEdge = SideDockEdge.RIGHT
) {
    init {
        require(schemaVersion in 1..CURRENT_SCHEMA_VERSION)
        require(columns in 1..30)
        require(symbolLabelTextSizeSp in 8..32)
        require(collapsedRows in 1..16)
        require(cellHeightDp in 6..128)
        require(cellHorizontalSpacingDp in -24..48)
        require(cellVerticalSpacingDp in -24..48)
        require(indicatorHeightDp in 0..96)
        require(maxExpandedHeightDp in 80..1800)
        require(animationDurationMs in 0..1200)
        require(flingVelocityDpPerSecond in 100..6000)
        require(longPressTimeoutMs in 250..1000)
        require(pageCacheLimit in 1..9)
        require(minimumTouchTargetDp in 20..112)
    }

    fun toDrawerConfig() = DrawerConfig(
        columns = columns,
        collapsedRows = collapsedRows,
        cellHeightDp = cellHeightDp,
        cellHorizontalSpacingDp = cellHorizontalSpacingDp,
        cellVerticalSpacingDp = cellVerticalSpacingDp,
        indicatorHeightDp = indicatorHeightDp,
        animationDurationMs = if (reduceMotion) 0L else animationDurationMs.toLong(),
        flingVelocityDpPerSecond = flingVelocityDpPerSecond,
        maxExpandedHeightDp = maxExpandedHeightDp,
        minimumTouchTargetDp = minimumTouchTargetDp
    )

    companion object {
        const val CURRENT_SCHEMA_VERSION = 3
    }
}
