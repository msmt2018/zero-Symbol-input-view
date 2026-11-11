package com.zerostudio.idea.widget.editor.symbolinput.v2.settings

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DrawerConfig

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class GroupIndicatorStyle { TEXT, PILL, UNDERLINE }
enum class HapticMode { OFF, SELECTION, ALL }

/** Immutable, validated settings snapshot persisted by the V2 settings repository. */
data class SymbolDrawerSettings(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val columns: Int = 10,
    val collapsedRows: Int = 2,
    val cellHeightDp: Int = 28,
    val cellHorizontalSpacingDp: Int = 2,
    val cellVerticalSpacingDp: Int = 2,
    val indicatorHeightDp: Int = 26,
    val indicatorStyle: GroupIndicatorStyle = GroupIndicatorStyle.UNDERLINE,
    val showGroupTitles: Boolean = true,
    val showSymbolLabels: Boolean = true,
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
    val searchIncludesActionPayload: Boolean = false
) {
    init {
        require(schemaVersion in 1..CURRENT_SCHEMA_VERSION)
        require(columns in 1..20)
        require(collapsedRows in 1..10)
        require(cellHeightDp in 20..96)
        require(cellHorizontalSpacingDp in 0..24)
        require(cellVerticalSpacingDp in 0..24)
        require(indicatorHeightDp in 0..64)
        require(maxExpandedHeightDp in 120..1200)
        require(animationDurationMs in 0..1200)
        require(flingVelocityDpPerSecond in 100..6000)
        require(longPressTimeoutMs in 250..1000)
        require(pageCacheLimit in 1..9)
        require(minimumTouchTargetDp in 32..80)
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
        const val CURRENT_SCHEMA_VERSION = 1
    }
}
