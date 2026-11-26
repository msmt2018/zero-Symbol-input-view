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

import com.google.gson.Gson
import com.google.gson.JsonParser

/** Platform-neutral settings persistence contract. */
interface SymbolDrawerSettingsRepository {
    fun load(): SettingsLoadResult
    fun save(settings: SymbolDrawerSettings)
    fun reset()
    /** True when a durable user snapshot exists rather than only defaults. */
    fun hasStoredSettings(): Boolean = false
}

data class SettingsLoadResult(
    val settings: SymbolDrawerSettings,
    val recoveredFromInvalidData: Boolean = false
)

/** Versioned JSON codec kept separate so defaults and malformed values are testable. */
object SymbolDrawerSettingsCodec {
    private val gson = Gson()

    fun encode(settings: SymbolDrawerSettings): String = gson.toJson(settings)

    fun decode(json: String): SymbolDrawerSettings = decodeInternal(json, strict = false)

    /** Import path: unlike local preference migration, external backups must be complete. */
    fun decodeStrict(json: String): SymbolDrawerSettings = decodeInternal(json, strict = true)

    private fun decodeInternal(json: String, strict: Boolean): SymbolDrawerSettings {
        val rootElement = JsonParser.parseString(json)
        require(rootElement.isJsonObject) { "Settings backup must be a JSON object" }
        val root = rootElement.asJsonObject
        val expected = setOf(
            "schemaVersion", "themeMode", "columns", "collapsedRows", "cellHeightDp",
            "cellHorizontalSpacingDp", "cellVerticalSpacingDp", "indicatorHeightDp",
            "indicatorStyle", "showGroupTitles", "showSymbolLabels", "rememberExpandedState",
            "showFullSymbolLabels", "symbolLabelTextSizeSp", "showActionHints",
            "maxExpandedHeightDp", "animationDurationMs", "reduceMotion",
            "flingVelocityDpPerSecond", "longPressTimeoutMs", "hapticMode",
            "autoCollapseAfterAction", "pageCacheLimit", "prefetchAdjacentGroups",
            "minimumTouchTargetDp", "searchIncludesActionPayload",
            "largeScreenLayoutMode", "sideDockEdge"
        )
        require(root.keySet().all { it in expected }) { "Settings backup contains unknown fields" }

        val defaults = SymbolDrawerSettings()
        val inputVersion = runCatching {
            val rawVersion = root.get("schemaVersion") ?: return@runCatching defaults.schemaVersion
            require(rawVersion.isJsonPrimitive && rawVersion.asJsonPrimitive.isNumber)
            rawVersion.asInt.also { require(rawVersion.asDouble == it.toDouble()) }
        }.getOrElse { throw IllegalArgumentException("Invalid settings schema version", it) }
        require(inputVersion in 1..SymbolDrawerSettings.CURRENT_SCHEMA_VERSION) {
            "Unsupported settings schema version: $inputVersion"
        }
        if (strict) {
            val requiredFields = when (inputVersion) {
                1 -> expected - setOf("largeScreenLayoutMode", "sideDockEdge", "rememberExpandedState", "showFullSymbolLabels", "symbolLabelTextSizeSp")
                2 -> expected - setOf("rememberExpandedState", "showFullSymbolLabels", "symbolLabelTextSizeSp")
                else -> expected
            }
            require(root.keySet() == requiredFields) {
                "Settings backup fields do not match schema version $inputVersion"
            }
        }
        fun int(name: String, default: Int): Int {
            val value = root.get(name) ?: return default
            require(value.isJsonPrimitive && value.asJsonPrimitive.isNumber) { "Invalid integer setting: $name" }
            val number = value.asInt
            require(value.asDouble == number.toDouble()) { "Non-integral setting: $name" }
            return number
        }
        fun bool(name: String, default: Boolean): Boolean {
            val value = root.get(name) ?: return default
            require(value.isJsonPrimitive && value.asJsonPrimitive.isBoolean) { "Invalid boolean setting: $name" }
            return value.asBoolean
        }
        fun enumValue(name: String, default: String): String {
            val value = root.get(name) ?: return default
            require(value.isJsonPrimitive && value.asJsonPrimitive.isString) { "Invalid enum setting: $name" }
            return value.asString
        }

        val version = int("schemaVersion", defaults.schemaVersion)
        require(version in 1..SymbolDrawerSettings.CURRENT_SCHEMA_VERSION) {
            "Unsupported settings schema version: $version"
        }
        return SymbolDrawerSettings(
            schemaVersion = SymbolDrawerSettings.CURRENT_SCHEMA_VERSION,
            themeMode = ThemeMode.valueOf(enumValue("themeMode", defaults.themeMode.name)),
            columns = int("columns", defaults.columns),
            collapsedRows = int("collapsedRows", defaults.collapsedRows),
            cellHeightDp = int("cellHeightDp", defaults.cellHeightDp),
            cellHorizontalSpacingDp = int("cellHorizontalSpacingDp", defaults.cellHorizontalSpacingDp),
            cellVerticalSpacingDp = int("cellVerticalSpacingDp", defaults.cellVerticalSpacingDp),
            indicatorHeightDp = int("indicatorHeightDp", defaults.indicatorHeightDp),
            indicatorStyle = GroupIndicatorStyle.valueOf(enumValue("indicatorStyle", defaults.indicatorStyle.name)),
            showGroupTitles = bool("showGroupTitles", defaults.showGroupTitles),
            showSymbolLabels = bool("showSymbolLabels", defaults.showSymbolLabels),
            rememberExpandedState = bool("rememberExpandedState", defaults.rememberExpandedState),
            showFullSymbolLabels = bool("showFullSymbolLabels", defaults.showFullSymbolLabels),
            symbolLabelTextSizeSp = int("symbolLabelTextSizeSp", defaults.symbolLabelTextSizeSp),
            showActionHints = bool("showActionHints", defaults.showActionHints),
            maxExpandedHeightDp = int("maxExpandedHeightDp", defaults.maxExpandedHeightDp),
            animationDurationMs = int("animationDurationMs", defaults.animationDurationMs),
            reduceMotion = bool("reduceMotion", defaults.reduceMotion),
            flingVelocityDpPerSecond = int("flingVelocityDpPerSecond", defaults.flingVelocityDpPerSecond),
            longPressTimeoutMs = int("longPressTimeoutMs", defaults.longPressTimeoutMs),
            hapticMode = HapticMode.valueOf(enumValue("hapticMode", defaults.hapticMode.name)),
            autoCollapseAfterAction = bool("autoCollapseAfterAction", defaults.autoCollapseAfterAction),
            pageCacheLimit = int("pageCacheLimit", defaults.pageCacheLimit),
            prefetchAdjacentGroups = bool("prefetchAdjacentGroups", defaults.prefetchAdjacentGroups),
            minimumTouchTargetDp = int("minimumTouchTargetDp", defaults.minimumTouchTargetDp),
            searchIncludesActionPayload = bool("searchIncludesActionPayload", defaults.searchIncludesActionPayload),
            largeScreenLayoutMode = LargeScreenLayoutMode.valueOf(
                enumValue("largeScreenLayoutMode", defaults.largeScreenLayoutMode.name)
            ),
            sideDockEdge = SideDockEdge.valueOf(enumValue("sideDockEdge", defaults.sideDockEdge.name))
        )
    }
}
