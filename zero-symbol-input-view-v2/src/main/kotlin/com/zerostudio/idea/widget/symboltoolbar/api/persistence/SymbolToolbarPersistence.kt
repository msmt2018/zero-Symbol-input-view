/*
 * Persistence adapters for the canonical symbol and settings repositories.
 * No second toolbar document, action schema, or plugin JSON model is defined here.
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.symboltoolbar.api.persistence

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.LegacySymbolDataCodec
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettingsCodec
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettingsRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.legacy.SymbolGroup

/**
 * The public toolbar API uses the exact V1 SymbolGroup/SymbolItem objects.
 * The internal V2 repository remains the source of truth; conversion is routed
 * through the existing LegacySymbolDataCodec so numeric IDs and payloads retain
 * their compatibility metadata rather than being copied into a parallel schema.
 */
object SymbolToolbarPersistence {
    private val gson = Gson()

    fun encodeGroups(groups: List<SymbolGroup>): String {
        val legacyJson = gson.toJson(groups)
        val validatedV2 = LegacySymbolDataCodec.decode(legacyJson)
        return LegacySymbolDataCodec.encode(validatedV2)
    }

    fun decodeGroups(json: String): List<SymbolGroup> {
        // Run the existing strict legacy shape and size validation first.
        LegacySymbolDataCodec.decode(json)
        val type = object : TypeToken<List<SymbolGroup>>() {}.type
        return gson.fromJson<List<SymbolGroup>>(json, type)
            ?: error("Symbol group JSON is empty")
    }

    fun encodeSettings(settings: SymbolDrawerSettings): String =
        SymbolDrawerSettingsCodec.encode(settings)

    fun decodeSettings(json: String): SymbolDrawerSettings =
        SymbolDrawerSettingsCodec.decodeStrict(json)

    fun saveSettings(repository: SymbolDrawerSettingsRepository, settings: SymbolDrawerSettings) {
        repository.save(settings)
    }

    fun loadSettings(repository: SymbolDrawerSettingsRepository): SymbolDrawerSettings =
        repository.load().settings
}
