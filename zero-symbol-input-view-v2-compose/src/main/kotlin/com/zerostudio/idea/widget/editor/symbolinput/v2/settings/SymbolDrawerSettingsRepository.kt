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

package com.zerostudio.idea.widget.editor.symbolinput.v2.settings

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser

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
            // Legacy snapshots are migrated in memory and re-saved as the current schema version.
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
