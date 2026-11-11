package com.zerostudio.idea.widget.editor.symbolinput.v2.data

import android.content.Context
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.LegacySymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

/**
 * One-way, non-destructive migration from the legacy compact JSON schema.
 * The old preference is read-only; the imported copy is saved to the V2 store.
 */
object LegacySymbolDataMigration {
    private const val LEGACY_PREFS = "advanced_symbol_prefs"
    private const val LEGACY_DATA_KEY = "symbol_json_data"

    fun importIfV2Empty(context: Context, repository: SymbolGroupRepository): Boolean {
        if (repository.isLegacyMigrationComplete()) return false
        if (repository.loadGroups().isNotEmpty()) {
            repository.markLegacyMigrationComplete()
            return false
        }
        val json = context.applicationContext
            .getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
            .getString(LEGACY_DATA_KEY, null)
            ?.takeIf { it.isNotBlank() } ?: run {
                repository.markLegacyMigrationComplete()
                return false
            }

        val imported = runCatching {
            val type = object : TypeToken<List<LegacyGroup>>() {}.type
            val legacy: List<LegacyGroup> = Gson().fromJson(json, type) ?: emptyList()
            legacy.mapIndexedNotNull { groupIndex, group ->
                val symbols = group.items.mapIndexed { symbolIndex, item ->
                    SymbolEntry(
                        id = ((groupIndex.toLong() shl 32) or (symbolIndex.toLong() and 0xffffffffL)) + 1L,
                        label = item.display,
                        shortAction = LegacySymbolActionAdapter.convert(LegacySymbolAction(item.shortAction, item.shortText)),
                        longAction = item.longAction?.let {
                            LegacySymbolActionAdapter.convert(LegacySymbolAction(it, item.longText))
                        },
                        legacyShortActionId = item.shortAction,
                        legacyShortText = item.shortText,
                        legacyLongActionId = item.longAction,
                        legacyLongText = item.longText
                    )
                }
                if (symbols.isEmpty()) null else SymbolGroup(
                    id = "legacy-$groupIndex-${group.name.hashCode()}",
                    title = group.name,
                    symbols = symbols
                )
            }
        }.getOrElse { return false }

        if (imported.isEmpty()) {
            repository.markLegacyMigrationComplete()
            return false
        }
        repository.saveGroups(imported)
        repository.markLegacyMigrationComplete()
        return true
    }

    private data class LegacyGroup(
        @SerializedName("n") val name: String = "",
        @SerializedName("d") val items: List<LegacySymbol> = emptyList()
    )

    private data class LegacySymbol(
        @SerializedName("a") val shortAction: Int = 0,
        @SerializedName("b") val display: String = "",
        @SerializedName("c") val shortText: String? = null,
        @SerializedName("d") val longAction: Int? = null,
        @SerializedName("e") val longText: String? = null
    )
}
